package gslayered;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT32;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDrawBuffers;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glMapBufferRange;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;
import static org.lwjgl.opengl.GL42.glTexStorage3D;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.glfw.GLFW;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;

public class GSLayered extends Application {
    private int      program_gslayers = 0;
    private int      program_showlayers = 0;
    private int      vao;
    private volatile int      mode = 0;
    private int      transform_ubo;

    private int      layered_fbo;
    private int      array_texture;
    private int      array_depth;

    SBMObject obj = new SBMObject();

	public GSLayered() {
		super("OpenGL SuperBible - Layered Rendering");
	}


    protected void startup() throws IOException
    {
    	vao = glGenVertexArrays();
        glBindVertexArray(vao);

        load_shaders();

        obj.load(getMediaPath() + "/objects/torus.sbm");

        transform_ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, transform_ubo);
        glBufferData(GL_UNIFORM_BUFFER, 17L * Matrix4x4f.sizeof(), GL_DYNAMIC_DRAW);

        array_texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, array_texture);
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_RGBA8, 256, 256, 16);

        array_depth = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, array_depth);
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, 1, GL_DEPTH_COMPONENT32, 256, 256, 16);

        layered_fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, layered_fbo);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, array_texture, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, array_depth, 0);
    }

    protected void shutdown()
    {
        glDeleteProgram(program_showlayers);
        glDeleteProgram(program_gslayers);
        glDeleteVertexArrays(vao);
    }

    static class TRANSFORM_BUFFER
    {
        Matrix4x4f proj_matrix;
        Matrix4x4f[] mv_matrix = new Matrix4x4f[16]; // 16 elements
        
		private ByteBuffer data;
		private int start_position;
        
        public TRANSFORM_BUFFER(ByteBuffer data) {
        	this.data = data;
        	this.start_position = data.position();
			proj_matrix = new Matrix4x4f();
        	proj_matrix.fromFloatBuffer(data.asFloatBuffer());
        	data.position((int)Matrix4x4f.sizeof());
        	for (int i = 0; i < mv_matrix.length; i++) {
        		mv_matrix[i] = new Matrix4x4f();
        		mv_matrix[i].fromFloatBuffer(data.asFloatBuffer());
            	data.position(data.position() + (int)Matrix4x4f.sizeof());
        	}
        	
        }

        public void commit() {
        	data.position(start_position);
        	proj_matrix.toFloatBuffer(data.asFloatBuffer());
        	data.position(data.position() + (int)Matrix4x4f.sizeof());
        	for (int i = 0; i < mv_matrix.length; i++) {
        		mv_matrix[i].toFloatBuffer(data.asFloatBuffer());
            	data.position(data.position() + (int)Matrix4x4f.sizeof());
        	}
        }
        
        static long sizeof() {
        	return 17 * Matrix4x4f.sizeof();
        }
    }
    
    protected void render(double t)
    {
//        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f)
//                                .mul(Matrix4x4f.rotate((float)t * 5.0f, 0.0f, 0.0f, 1.0f))
//                                		.mul(Matrix4x4f.rotate((float)t * 30.0f, 1.0f, 0.0f, 0.0f));
//        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
//        Matrix4x4f mvp = proj_matrix.mul(mv_matrix);


        glBindBufferBase(GL_UNIFORM_BUFFER, 0, transform_ubo);

        ByteBuffer data = glMapBufferRange(GL_UNIFORM_BUFFER, 0, TRANSFORM_BUFFER.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        TRANSFORM_BUFFER buffer = new TRANSFORM_BUFFER(data);
        
        
        buffer.proj_matrix = Matrix4x4f.perspective(50.0f, 1.0f, 0.1f, 1000.0f); // proj_matrix;
        int i;

        for (i = 0; i < 16; i++)
        {
            float fi = (float)(i + 12) / 16.0f;
            buffer.mv_matrix[i] = Matrix4x4f.translate(0.0f, 0.0f, -4.0f)
                                  .mul(Matrix4x4f.rotate((float)t * 25.0f * fi, 0.0f, 0.0f, 1.0f))
                                  .mul(Matrix4x4f.rotate((float)t * 30.0f * fi, 1.0f, 0.0f, 0.0f));
        }
        
        buffer.commit();

        glUnmapBuffer(GL_UNIFORM_BUFFER);

        int ca0 = GL_COLOR_ATTACHMENT0;

        glBindFramebuffer(GL_FRAMEBUFFER, layered_fbo);
        glDrawBuffers(ca0);
        glViewport(0, 0, 256, 256);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glUseProgram(program_gslayers);

        obj.render();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glDrawBuffer(GL_BACK);
        glUseProgram(program_showlayers);

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 1.0f);

        glBindTexture(GL_TEXTURE_2D_ARRAY, array_texture);
        glDisable(GL_DEPTH_TEST);

        glBindVertexArray(vao);
        glDrawArraysInstanced(GL_TRIANGLE_FAN, 0, 4, 16);

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action != GLFW.GLFW_PRESS)
            return;

        switch (key)
        {
            case '1':
            case '2':
                    mode = key - '1';
                break;
            case 'R':
                    load_shaders();
                break;
            case 'M':
                    mode = (mode + 1) % 2;
                break;
        }
    }

    void load_shaders() throws IOException
    {
        int vs;
        int gs;
        int fs;

        if (program_showlayers != 0)
            glDeleteProgram(program_showlayers);

        program_showlayers = glCreateProgram();

        vs = Shader.load(getMediaPath() + "/shaders/gslayers/showlayers.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/gslayers/showlayers.fs.glsl", GL_FRAGMENT_SHADER);

        glAttachShader(program_showlayers, vs);
        glAttachShader(program_showlayers, fs);

        glLinkProgram(program_showlayers);

        glDeleteShader(vs);
        glDeleteShader(fs);

        vs = Shader.load(getMediaPath() + "/shaders/gslayers/gslayers.vs.glsl", GL_VERTEX_SHADER);
        gs = Shader.load(getMediaPath() + "/shaders/gslayers/gslayers.gs.glsl", GL_GEOMETRY_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/gslayers/gslayers.fs.glsl", GL_FRAGMENT_SHADER);

        if (program_gslayers != 0)
            glDeleteProgram(program_gslayers);

        program_gslayers = glCreateProgram();

        glAttachShader(program_gslayers, vs);
        glAttachShader(program_gslayers, gs);
        glAttachShader(program_gslayers, fs);

        glLinkProgram(program_gslayers);

        glDeleteShader(vs);
        glDeleteShader(gs);
        glDeleteShader(fs);
    }
	public static void main(String[] args) {
		new GSLayered().run();
	}

}
