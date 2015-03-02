package simpletexcoords;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;





import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL42.*;


public class SimpleTexCoords extends Application {
    int          render_prog;

    /** opengl names (indices) for the registered texture objects */
    int[]        tex_object = new int[2];
    /** index of the currently applied texture object */
    int          tex_index;

    class Uniforms
    {
        int       mv_matrix;
        int       proj_matrix;
    } 
    /**
     * model view and projection matrix delivered 
     * to vertex shader as uniform variables.
     */
    Uniforms uniforms = new Uniforms();

    /** vertices of the torus */
    SBMObject     object = new SBMObject();


	public SimpleTexCoords() {
		super("OpenGL SuperBible - Texture Coordinates");
	}

	public void init() {
		super.init();
	}
	
	
	@Override
	protected void startup() throws Throwable {
		/*
		 * Instantiate 2 alternate textures. The first is just a black 
		 * and white checkerboard pattern image. The second is a more 
		 * organic black and white pattern.
		 */
		final byte[] B = {0x00, 0x00, 0x00, 0x00};
		final byte[] W = {(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF};
		// Note: this should be actually a 1D array but for
		//       readability we did it here in 2D and convert it later
        final byte[][] tex_data = {
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
            B, W, B, W, B, W, B, W, B, W, B, W, B, W, B, W,
            W, B, W, B, W, B, W, B, W, B, W, B, W, B, W, B,
        };
        // register a new texture object on the graphics card
    	tex_object[0] = glGenTextures();
    	// bind it to the current opengl context
        glBindTexture(GL_TEXTURE_2D, tex_object[0]);
        // allocate appropriate memory
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB8, 16, 16);
        // submit its data
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 16, 16, GL_RGBA, GL_UNSIGNED_BYTE, toUByteBuffer(tex_data));
        
        // The following two parameters define parameters of the default 
        // sampler object for this texture. In particular these two parameters 
        // specify how a fragments colour is determined if the corresponding texture 
        // coordinate does not map exactly to a specific pixel in the texture.
        // GL_LINEAR is the default value which does not work without mipmaps.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // load the image for the second texture.
        tex_object[1] = KTX.load(getMediaPath() + "textures/pattern1.ktx");


        
        // Load the vertices of the 3D object (torus) to be displayed.
        // and the corresponding texture coordinates into buffer objects
        // and register vertex attributes accordingly.
        // Positions are referenced by vertex attribute 0
        // and texture coordinates are referenced by vertex attribute 4
        object.load(getMediaPath() + "objects/torus_nrms_tc.sbm");

        load_shaders();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

	}


	private void load_shaders() {
        if (render_prog != 0)
            glDeleteProgram(render_prog);

        int vs, fs;
        try {
	        vs = Shader.load(getMediaPath() + "shaders/simpletexcoords/render.vs.glsl", GL_VERTEX_SHADER);
	        fs = Shader.load(getMediaPath() + "shaders/simpletexcoords/render.fs.glsl", GL_FRAGMENT_SHADER);
        } catch (IOException e) {
        	throw new Error(e);
        }
        render_prog = glCreateProgram();
        glAttachShader(render_prog, vs);
        glAttachShader(render_prog, fs);
        glLinkProgram(render_prog);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.mv_matrix = glGetUniformLocation(render_prog, "mv_matrix");
        uniforms.proj_matrix = glGetUniformLocation(render_prog, "proj_matrix");
	}

	private ByteBuffer toUByteBuffer(byte[][] tex_data) {
		ByteBuffer b = BufferUtils.createByteBuffer(tex_data.length * tex_data[0].length);
		for (int i = 0; i < tex_data.length; i++) {
			b.put(tex_data[i]);
		}
		b.rewind();
		return b;
	}

	@Override
	protected void render(double currentTime) throws Throwable {

        glClearBuffer4f(GL_COLOR, 0, 0.2f, 0.2f, 0.2f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        // activate the texture we want to use for the torus
        glBindTexture(GL_TEXTURE_2D, tex_object[tex_index]);

        glUseProgram(render_prog);

        // create projection matrix (would be more efficient to do it once and change it only on window resize)
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        // create a model view matrix (here the translation could be constant because it does not change over time)
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -3.0f) 
        		.mul(Matrix4x4f.rotate((float)currentTime * 19.3f, 0.0f, 1.0f, 0.0f))
                .mul(Matrix4x4f.rotate((float)currentTime * 21.1f, 0.0f, 0.0f, 1.0f));

        glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());
        object.render();
	}

	@Override
	protected void shutdown() throws Throwable {
        glDeleteProgram(render_prog);
        glDeleteTextures(tex_object[0]);
        glDeleteTextures(tex_object[1]);
        // Not done in original example..
        object.free();
	}

    protected void onKey(int key, int action)
    {
        if (action != 0)
        {
            switch (key)
            {
                case 'R': load_shaders();
                    break;
                case 'T':
                    tex_index++;
                    if (tex_index > 1)
                        tex_index = 0;
                    break;
            }
        }
    }

	public static void main(String[] args) {
		SimpleTexCoords app = new SimpleTexCoords();
		app.run();
	}

}
