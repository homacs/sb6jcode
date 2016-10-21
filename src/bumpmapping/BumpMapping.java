package bumpmapping;


import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

public class BumpMapping extends Application {

    static class TGAHeader
    {
        byte identsize;    // Size of following ID field
        byte cmaptype;     // Color map type 0 = none
        byte imagetype;    // Image type 2 = rgb
        short cmapstart;   // First entry in palette
        short cmapsize;    // Fumber of entries in palette
        byte cmapbpp;      // Number of bits per palette entry
        short xorigin;     // X origin
        short yorigin;     // Y origin
        short width;       // Width in pixels
        short height;      // Height in pixels
        byte bpp;          // Bits per pixel
        byte descriptor;   // Descriptor bits
        
        TGAHeader() {
        	// Java will init all values with zero, so we don't have to do anything here.
        }

        
        public void write(ByteBuffer buf) {
			buf.put(identsize);
			buf.put(cmaptype);
			buf.put(imagetype);
			buf.putShort(cmapstart);
			buf.putShort(cmapsize);
			buf.put(cmapbpp);
			buf.putShort(xorigin);
			buf.putShort(yorigin);
			buf.putShort(width);
			buf.putShort(height);
			buf.put(bpp);
			buf.put(descriptor);
        }
        
		public void write(FileOutputStream f_out) throws IOException {
			ByteBuffer buf = ByteBuffer.wrap(new byte[sizeof()]);
			buf.order(ByteOrder.LITTLE_ENDIAN);
			// write to buffer with correct byte ordering
			write(buf);
			
			// write to file
			f_out.write(buf.array());
		}

		static int sizeof() {
			return 6 + 6*2;
		}
    } 
    private int          program = 0;

    class Textures
    {
    	int      color;
    	int      normals;
    } 
    private Textures textures = new Textures();

    class Uniforms
    {
    	int       mv_matrix;
    	int       proj_matrix;
    	int       light_pos;
    } 
    private Uniforms uniforms = new Uniforms();

    private SBMObject     object = new SBMObject();
    private boolean            paused = false;

    private double last_time = 0.0;
    private double total_time = 0.0;

	public BumpMapping() {
		super("OpenGL SuperBible - Bump Mapping");
	}

		
	
	protected void startup() throws IOException
	{
	    load_shaders();
	
	    glActiveTexture(GL_TEXTURE0);
	    textures.color = KTX.load(getMediaPath() + "/textures/ladybug_co.ktx");
	    glActiveTexture(GL_TEXTURE1);
	    textures.normals = KTX.load(getMediaPath() + "/textures/ladybug_nm.ktx");
	
	    object.load(getMediaPath() + "/objects/ladybug.sbm");
	}
	
	protected void render(double currentTime)
	{
	
	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;
	
	    float f = (float)total_time;
	
	    glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	    glClearBuffer1f(GL_DEPTH, 0, 1f);
	
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	    glEnable(GL_DEPTH_TEST);
	
	    glUseProgram(program);
	
	    Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
	                                                    (float)info.windowWidth / (float)info.windowHeight,
	                                                    0.1f,
	                                                    1000.0f);
	    glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());
	
	    Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, -0.2f, -5.5f)
	                            .mul(Matrix4x4f.rotate(14.5f, 1.0f, 0.0f, 0.0f))
	                            .mul(Matrix4x4f.rotate(-20.0f, 0.0f, 1.0f, 0.0f))
	                            //.mul(Matrix4x4f.rotate(t * 14.5f, 0.0f, 1.0f, 0.0f))
	                            //.mul(Matrix4x4f.rotate(0.0f, 1.0f, 0.0f, 0.0f))
	                            .mul(Matrix4x4f.identity());
	    glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());
	
	    FloatBuffer light_pos = BufferUtilsHelper.createFloatBuffer(new float[]{
	    		40.0f * MathHelper.sinf(f), 
	    		30.0f + 20.0f * MathHelper.cosf(f), 
	    		40.0f
	    });
	    glUniform3(uniforms.light_pos, light_pos);
	
	    object.render();
	}
	
	void make_screenshot() throws IOException
	{
	    int row_size = ((info.windowWidth * 3 + 3) & ~3);
	    int data_size = row_size * info.windowHeight;
	    ByteBuffer data = BufferUtils.createByteBuffer(data_size);
	
	    TGAHeader tga_header = new TGAHeader();
	
	    glReadPixels(0, 0,                                  // Origin
	                 info.windowWidth, info.windowHeight,   // Size
	                 GL_BGR, GL_UNSIGNED_BYTE,              // Format, type
	                 data);                                 // Data
	
	    tga_header.imagetype = 2;
	    tga_header.width = (short)info.windowWidth;
	    tga_header.height = (short)info.windowHeight;
	    tga_header.bpp = 24;
	
	    File tmpdir = new File(System.getProperty("java.io.tmpdir"));
	    FileOutputStream f_out = new FileOutputStream(new File(tmpdir, "screenshot.tga"));
	    tga_header.write(f_out);
	    
	    // not the most efficient way, but for some reason the JVM crashes 
	    // if we try to provide JVM allocated memory to opengl (graphics card)
	    // (i.e. use an array backed byte buffer instead of memory allocated outside via JNI).
	    for (int i = 0; i < data_size; i++) {
		    f_out.write(data.get());
	    }
	    f_out.close();
	
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action != GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case 'R': 
	                load_shaders();
	                break;
	            case 'S':
	                make_screenshot();
	                break;
	            case 'P':
	                paused = !paused;
	                break;
	        }
	    }
	}
	
	void load_shaders() throws IOException
	{
	    int vs;
	    int fs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/bumpmapping/bumpmapping.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/bumpmapping/bumpmapping.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (program != 0)
	        glDeleteProgram(program);
	
	    program = Program.link(true, vs, fs);
	
	    uniforms.mv_matrix = glGetUniformLocation(program, "mv_matrix");
	    uniforms.proj_matrix = glGetUniformLocation(program, "proj_matrix");
	    uniforms.light_pos = glGetUniformLocation(program, "light_pos");
	}


	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
	}
	
	public static void main(String[] args) {
		new BumpMapping().run();
	}



}
