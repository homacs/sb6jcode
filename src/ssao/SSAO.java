package ssao;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Random;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import sb6.vmath.Vector4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.*;

public class SSAO extends Application {
    private Random rng = new Random(0x13371337);
    
    private int      render_program = 0;
    private int      ssao_program = 0;
    private boolean        paused = false;

    private int      render_fbo;
    private int[]    fbo_textures = new int[3];
    private int      quad_vao;
    private int      points_buffer;

    private SBMObject object = new SBMObject();
    private SBMObject cube = new SBMObject();

    class Uniforms
    {
        class Render
        {
            int           mv_matrix;
            int           proj_matrix;
            int           shading_level;
        } 
        Render render = new Render();
        class SSAOData
        {
        	int           ssao_level;
        	int           object_level;
        	int           ssao_radius;
        	int           weight_by_angle;
        	int           randomize_points;
        	int           point_count;
        } 
        SSAOData ssao = new SSAOData();
    } 
    private Uniforms uniforms = new Uniforms();

    private boolean  show_shading = true;
    private boolean  show_ao = true;
    // private float ssao_level = 1f; // (never used)
    private float ssao_radius = 0.05f;
    private boolean  weight_by_angle = true;
    private boolean randomize_points = true;
    private int point_count = 10;

    static class SAMPLE_POINTS
    {
    	//
    	// I kept this structure here, just to show how data is
    	// lay out in the buffer later.
    	//
        Vector4f[]     point = new Vector4f[256];
        Vector4f[]     random_vectors = new Vector4f[256];
        
		public static long sizeof() {
			return 256*Vector4f.sizeof() + 256*Vector4f.sizeof();
		}
    }

    double last_time = 0.0;
    double total_time = 0.0;
    private Matrix4x4f view_matrix = Matrix4x4f.lookat(new Vector3f(0.0f, 3.0f, 15.0f),
    		new Vector3f(0.0f, 0.0f, 0.0f),
    		new Vector3f(0.0f, 1.0f, 0.0f));


    
    public SSAO() {
		super("OpenGL SuperBible - Screen-Space Ambient Occlusion");
		info.samples = 8;

	}

	private float random_float() {
	    return rng.nextFloat();
	}
	
	
	protected void startup() throws IOException
	{
	    load_shaders();
	
	    render_fbo = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, render_fbo);
	    int i = 0;
	    fbo_textures[i++] = glGenTextures();
	    fbo_textures[i++] = glGenTextures();
	    fbo_textures[i++] = glGenTextures();
	
	    glBindTexture(GL_TEXTURE_2D, fbo_textures[0]);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, 2048, 2048);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	
	    glBindTexture(GL_TEXTURE_2D, fbo_textures[1]);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, 2048, 2048);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	
	    glBindTexture(GL_TEXTURE_2D, fbo_textures[2]);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT32F, 2048, 2048);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, fbo_textures[0], 0);
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, fbo_textures[1], 0);
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, fbo_textures[2], 0);
	
	    IntBuffer draw_buffers = BufferUtilsHelper.createIntBuffer(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });
	    glDrawBuffers(draw_buffers);
	
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);
	
	    quad_vao = glGenVertexArrays();
	    glBindVertexArray(quad_vao);
	
	    object.load(getMediaPath() + "/objects/dragon.sbm");
	    cube.load(getMediaPath() + "/objects/cube.sbm");
	
	    glEnable(GL_DEPTH_TEST);
	    glEnable(GL_CULL_FACE);
	
	    //
	    // Constructing SAMPLE_POINTS data here.
	    // Instead of writing it to Java heap memory first, we directly stream 
	    // it to the buffer in graphics memory.
	    // 
	    points_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, points_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, SAMPLE_POINTS.sizeof(), GL_STATIC_DRAW);
	    
	    ByteBuffer bstream = glMapBuffer(GL_UNIFORM_BUFFER, GL_WRITE_ONLY);
	    FloatBuffer stream = bstream.asFloatBuffer();
	    
	    Vector4f point = new Vector4f();
	    Vector4f random_vector = new Vector4f();
	    
	    for (i = 0; i < 256; i++)
	    {
	        do
	        {
	            point.set(0, random_float() * 2.0f - 1.0f);
	            point.set(1, random_float() * 2.0f - 1.0f);
	            point.set(2, random_float()); //  * 2.0f - 1.0f;
	            point.set(3, 0.0f);
	        } while (point.length() > 1.0f);
	        point.normalize();
	        point.toFloatBuffer(stream);
	    }
	    for (i = 0; i < 256; i++)
	    {
	        random_vector.set(0, random_float());
	        random_vector.set(1, random_float());
	        random_vector.set(2, random_float());
	        random_vector.set(3, random_float());
	        random_vector.toFloatBuffer(stream);
	    }

	    glUnmapBuffer(GL_UNIFORM_BUFFER);
	
	}
	
	protected void render(double currentTime)
	{
	
	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;
	
	    float f = (float)total_time;
	
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	
	    glBindFramebuffer(GL_FRAMEBUFFER, render_fbo);
	    glEnable(GL_DEPTH_TEST);
	
	    glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);
	    glClearBuffer4f(GL_COLOR, 1, 0.0f, 0.0f, 0.0f, 0.0f);
	    glClearBuffer1f(GL_DEPTH, 0, 1f);
	
	    glBindBufferBase(GL_UNIFORM_BUFFER, 0, points_buffer);
	
	    glUseProgram(render_program);
	
	    Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
	                                                    (float)info.windowWidth / (float)info.windowHeight,
	                                                    0.1f,
	                                                    1000.0f);
	    glUniformMatrix4(uniforms.render.proj_matrix, false, proj_matrix.toFloatBuffer());
	
	    Matrix4x4f model_matrix = Matrix4x4f.translate(0.0f, -5.0f, 0.0f)
	    		.mul(Matrix4x4f.rotate(f * 5.0f, 0.0f, 1.0f, 0.0f))
	    		.mul(Matrix4x4f.identity());
	    Matrix4x4f mv_matrix = Matrix4x4f.multiply(view_matrix, model_matrix);
	    glUniformMatrix4(uniforms.render.mv_matrix, false, mv_matrix.toFloatBuffer());
	
	    glUniform1f(uniforms.render.shading_level, show_shading ? (show_ao ? 0.7f : 1.0f) : 0.0f);
	
	    object.render();
	
	    model_matrix = Matrix4x4f.translate(0.0f, -4.5f, 0.0f)
	                .mul(Matrix4x4f.rotate(f * 5.0f, 0.0f, 1.0f, 0.0f))
	                .mul(Matrix4x4f.scale(4000.0f, 0.1f, 4000.0f))
	                .mul(Matrix4x4f.identity());
	    mv_matrix = Matrix4x4f.multiply(view_matrix, model_matrix);
	    glUniformMatrix4(uniforms.render.mv_matrix, false, mv_matrix.toFloatBuffer());
	
	    cube.render();
	
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);
	
	    glUseProgram(ssao_program);
	
	    glUniform1f(uniforms.ssao.ssao_radius, ssao_radius * (float)info.windowWidth / 1000.0f);
	    glUniform1f(uniforms.ssao.ssao_level, show_ao ? (show_shading ? 0.3f : 1.0f) : 0.0f);
	    // glUniform1i(uniforms.ssao.weight_by_angle, weight_by_angle ? 1 : 0);
	    glUniform1i(uniforms.ssao.randomize_points, randomize_points ? 1 : 0);
	    glUniform1ui(uniforms.ssao.point_count, point_count);
	
	    glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, fbo_textures[0]);
	    glActiveTexture(GL_TEXTURE1);
	    glBindTexture(GL_TEXTURE_2D, fbo_textures[1]);
	
	    glDisable(GL_DEPTH_TEST);
	    glBindVertexArray(quad_vao);
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}
	
	void load_shaders() throws IOException
	{
	    int vs,fs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/ssao/render.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/ssao/render.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (render_program != 0)
	        glDeleteProgram(render_program);
	
	    render_program = Program.link(true, vs, fs);
	
	    uniforms.render.mv_matrix = glGetUniformLocation(render_program, "mv_matrix");
	    uniforms.render.proj_matrix = glGetUniformLocation(render_program, "proj_matrix");
	    uniforms.render.shading_level = glGetUniformLocation(render_program, "shading_level");
	
	    vs = Shader.load(getMediaPath() + "/shaders/ssao/ssao.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/ssao/ssao.fs.glsl", GL_FRAGMENT_SHADER);
	
	    ssao_program = Program.link(true, vs, fs);
	
	    uniforms.ssao.ssao_radius = glGetUniformLocation(ssao_program, "ssao_radius");
	    uniforms.ssao.ssao_level = glGetUniformLocation(ssao_program, "ssao_level");
	    uniforms.ssao.object_level = glGetUniformLocation(ssao_program, "object_level");
	    uniforms.ssao.weight_by_angle = glGetUniformLocation(ssao_program, "weight_by_angle");
	    uniforms.ssao.randomize_points = glGetUniformLocation(ssao_program, "randomize_points");
	    uniforms.ssao.point_count = glGetUniformLocation(ssao_program, "point_count");
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action == GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case 'N':
	                weight_by_angle = !weight_by_angle;
	                break;
	            case 'R':
	                randomize_points = !randomize_points;
	                break;
	            case 'S':
	                point_count++;
	                break;
	            case 'X':
	                point_count--;
	                break;
	            case 'Q':
	                show_shading = !show_shading;
	                break;
	            case 'W':
	                show_ao = !show_ao;
	                break;
	            case 'A':
	                ssao_radius += 0.01f;
	                break;
	            case 'Z':
	                ssao_radius -= 0.01f;
	                break;
	            case 'P':
	                paused = !paused;
	                break;
	            case 'L':
	                load_shaders();
	                break;
	            case GLFW.GLFW_KEY_SPACE:
	            	System.out.println("====================================");
	            	System.out.println("weight_by_angle (N):\t" + weight_by_angle);
	            	System.out.println("randomize_points (R):\t" + randomize_points);
	            	System.out.println("point_count (S/X):\t" + point_count);
	            	System.out.println("show_shading (Q):\t" + show_shading);
	            	System.out.println("show_ao (W):\t\t" + show_ao);
	            	System.out.println("ssao_radius (A/Z):\t" + ssao_radius);
	            	break;
	        }
	    }
	}
	
	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		new SSAO().run();
	}


}
