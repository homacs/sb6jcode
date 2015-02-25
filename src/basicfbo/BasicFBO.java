package basicfbo;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*; // framebuffer texture
import static org.lwjgl.opengl.GL42.*; // textures

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


import org.lwjgl.BufferUtils;

import sb6.Application;
import sb6.GLAPIHelper;
import sb6.Shader;
import sb6.vmath.Matrix4x4f;

public class BasicFBO extends Application {
	private int program1;
	private int program2;
	private int mv_location;
	private int proj_location;
	private int mv_location2;
	private int proj_location2;
	private int vao;
	private int position_buffer;
	private int index_buffer;
	private int fbo;
	private int color_texture;
	private int depth_texture;

	
	public BasicFBO() {
		super("OpenGL SuperBible - Basic Framebuffer Object");
	}
	
	protected void startup()
	{
		String vs_source =
	            "#version 410 core                                                  \n" +
	            "                                                                   \n" +
	            "layout (location = 0) in vec4 position;                            \n" +
	            "layout (location = 1) in vec2 texcoord;                            \n" +
	            "                                                                   \n" +
	            "out VS_OUT                                                         \n" +
	            "{                                                                  \n" +
	            "    vec4 color;                                                    \n" +
	            "    vec2 texcoord;                                                 \n" +
	            "} vs_out;                                                          \n" +
	            "                                                                   \n" +
	            "uniform mat4 mv_matrix;                                            \n" +
	            "uniform mat4 proj_matrix;                                          \n" +
	            "                                                                   \n" +
	            "void main(void)                                                    \n" +
	            "{                                                                  \n" +
	            "    gl_Position = proj_matrix * mv_matrix * position;              \n" +
	            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n" +
	            "    vs_out.texcoord = texcoord;                                    \n" +
	            "}                                                                  \n";
		
		String fs_source1 =
	            "#version 410 core                                                              \n" +
	            "                                                                               \n" +
	            "in VS_OUT                                                                      \n" +
	            "{                                                                              \n" +
	            "    vec4 color;                                                                \n" +
	            "    vec2 texcoord;                                                             \n" +
	            "} fs_in;                                                                       \n" +
	            "                                                                               \n" +
	            "out vec4 color;                                                                \n" +
	            "                                                                               \n" +
	            "void main(void)                                                                \n" +
	            "{                                                                              \n" +
	            "    color = sin(fs_in.color * vec4(40.0, 20.0, 30.0, 1.0)) * 0.5 + vec4(0.5);  \n" +
	            "}                                                                              \n";
		
		String fs_source2 =
	            "#version 420 core                                                              \n" +
	            "                                                                               \n" +
	            "uniform sampler2D tex;                                                         \n" +
	            "                                                                               \n" +
	            "out vec4 color;                                                                \n" +
	            "                                                                               \n" +
	            "in VS_OUT                                                                      \n" +
	            "{                                                                              \n" +
	            "    vec4 color;                                                                \n" +
	            "    vec2 texcoord;                                                             \n" +
	            "} fs_in;                                                                       \n" +
	            "                                                                               \n" +
	            "void main(void)                                                                \n" +
	            "{                                                                              \n" +
	            "    color = mix(fs_in.color, texture(tex, fs_in.texcoord), 0.7);               \n" +
	            "}                                                                              \n";
		
        program1 = glCreateProgram();
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source1);
        glCompileShader(fs);
        Shader.checkCompilerResult(fs, "fs_source1");
        
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);
        Shader.checkCompilerResult(vs, "vs_source");

        glAttachShader(program1, vs);
        glAttachShader(program1, fs);

        glLinkProgram(program1);
        Shader.checkLinkerResult(program1);

        glDeleteShader(vs);
        glDeleteShader(fs);

        program2 = glCreateProgram();
        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source2);
        glCompileShader(fs);
        Shader.checkCompilerResult(fs, "fs_source2");

        vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);
        Shader.checkCompilerResult(vs, "vs_source");


        glAttachShader(program2, vs);
        glAttachShader(program2, fs);

        glLinkProgram(program2);
        Shader.checkLinkerResult(program2);

        glDeleteShader(vs);
        glDeleteShader(fs);
        
        
        mv_location = glGetUniformLocation(program1, "mv_matrix");
        proj_location = glGetUniformLocation(program1, "proj_matrix");
        mv_location2 = glGetUniformLocation(program2, "mv_matrix");
        proj_location2 = glGetUniformLocation(program2, "proj_matrix");

        
        vao = glGenVertexArrays();
        glBindVertexArray(vao);


        short vertex_indices[] =
            {
                0, 1, 2,
                2, 1, 3,
                2, 3, 4,
                4, 3, 5,
                4, 5, 6,
                6, 5, 7,
                6, 7, 0,
                0, 7, 1,
                6, 0, 2,
                2, 4, 6,
                7, 5, 3,
                7, 3, 1
            };

        float vertex_data[] =
            {
                 // Position                 Tex Coord
                -0.25f, -0.25f,  0.25f,      0.0f, 1.0f,
                -0.25f, -0.25f, -0.25f,      0.0f, 0.0f,
                 0.25f, -0.25f, -0.25f,      1.0f, 0.0f,

                 0.25f, -0.25f, -0.25f,      1.0f, 0.0f,
                 0.25f, -0.25f,  0.25f,      1.0f, 1.0f,
                -0.25f, -0.25f,  0.25f,      0.0f, 1.0f,

                 0.25f, -0.25f, -0.25f,      0.0f, 0.0f,
                 0.25f,  0.25f, -0.25f,      1.0f, 0.0f,
                 0.25f, -0.25f,  0.25f,      0.0f, 1.0f,

                 0.25f,  0.25f, -0.25f,      1.0f, 0.0f,
                 0.25f,  0.25f,  0.25f,      1.0f, 1.0f,
                 0.25f, -0.25f,  0.25f,      0.0f, 1.0f,

                 0.25f,  0.25f, -0.25f,      1.0f, 0.0f,
                -0.25f,  0.25f, -0.25f,      0.0f, 0.0f,
                 0.25f,  0.25f,  0.25f,      1.0f, 1.0f,

                -0.25f,  0.25f, -0.25f,      0.0f, 0.0f,
                -0.25f,  0.25f,  0.25f,      0.0f, 1.0f,
                 0.25f,  0.25f,  0.25f,      1.0f, 1.0f,

                -0.25f,  0.25f, -0.25f,      1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f,      0.0f, 0.0f,
                -0.25f,  0.25f,  0.25f,      1.0f, 1.0f,

                -0.25f, -0.25f, -0.25f,      0.0f, 0.0f,
                -0.25f, -0.25f,  0.25f,      0.0f, 1.0f,
                -0.25f,  0.25f,  0.25f,      1.0f, 1.0f,

                -0.25f,  0.25f, -0.25f,      0.0f, 1.0f,
                 0.25f,  0.25f, -0.25f,      1.0f, 1.0f,
                 0.25f, -0.25f, -0.25f,      1.0f, 0.0f,

                 0.25f, -0.25f, -0.25f,      1.0f, 0.0f,
                -0.25f, -0.25f, -0.25f,      0.0f, 0.0f,
                -0.25f,  0.25f, -0.25f,      0.0f, 1.0f,

                -0.25f, -0.25f,  0.25f,      0.0f, 0.0f,
                 0.25f, -0.25f,  0.25f,      1.0f, 0.0f,
                 0.25f,  0.25f,  0.25f,      1.0f, 1.0f,

                 0.25f,  0.25f,  0.25f,      1.0f, 1.0f,
                -0.25f,  0.25f,  0.25f,      0.0f, 1.0f,
                -0.25f, -0.25f,  0.25f,      0.0f, 0.0f,
            };
        
        position_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, position_buffer);
        // Note: This can be more efficient if the internal array of the buffer is populated directly
        FloatBuffer vertex_data_buffer = BufferUtils.createFloatBuffer(vertex_data.length);
        vertex_data_buffer.put(vertex_data);
        vertex_data_buffer.rewind();
        glBufferData(GL_ARRAY_BUFFER, vertex_data_buffer, GL_STATIC_DRAW);

        // specify position format and component number of the attributes
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.SIZE/8, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.SIZE/8, (3 * Float.SIZE/8));
        glEnableVertexAttribArray(1);

        index_buffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_buffer);
        ShortBuffer vertex_indices_buffer = BufferUtils.createShortBuffer(vertex_indices.length);
        vertex_indices_buffer.rewind();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertex_indices_buffer, GL_STATIC_DRAW);

        glEnable(GL_CULL_FACE);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        color_texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, color_texture);
        glTexStorage2D(GL_TEXTURE_2D, 9, GL_RGBA8, 512, 512);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        depth_texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depth_texture);
        glTexStorage2D(GL_TEXTURE_2D, 9, GL_DEPTH_COMPONENT32F, 512, 512);

        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, color_texture, 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depth_texture, 0);

        int draw_buffers = GL_COLOR_ATTACHMENT0;
        glDrawBuffers(draw_buffers);
    }
	
	protected void shutdown()
	{
        glDeleteVertexArrays(vao);
        glDeleteProgram(program1);
        glDeleteProgram(program2);
        glDeleteBuffers(position_buffer);
        glDeleteFramebuffers(fbo);
        glDeleteTextures(color_texture);
	}
	
	// Our rendering function
	protected void render(double currentTime) {
		float one = 1.0f;
		
		// TODO: possible error source
		Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                (float)info.getWindowWidth() / (float)info.getWindowHeight(),
                0.1f,
                1000.0f);
		FloatBuffer proj_matrix_buffer = proj_matrix.toFloatBuffer();

        float f = (float)currentTime * 0.3f;
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f);
        mv_matrix.mul(Matrix4x4f.translate(
        		(float)Math.sin(2.1f * f) * 0.5f, 
        		(float)Math.cos(1.7f * f) * 0.5f, 
        		(float)(Math.sin(1.3f * f) * Math.cos(1.5f * f) * 2.0f)));
        mv_matrix.mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f));
        mv_matrix.mul(Matrix4x4f.rotate((float)currentTime * 81.0f, 1.0f, 0.0f, 0.0f));

        FloatBuffer mv_matrix_buffer = mv_matrix.toFloatBuffer();

        
        
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        glViewport(0, 0, 512, 512);
        GLAPIHelper.glClearBuffer4f(GL_COLOR, 0,  0.0f, 0.1f, 0.0f, 1.0f); // green
        GLAPIHelper.glClearBuffer1f(GL_DEPTH, 0, one);

        glUseProgram(program1);

        glUniformMatrix4(proj_location, false, proj_matrix_buffer);
        glUniformMatrix4(mv_location, false, mv_matrix_buffer);
        glDrawArrays(GL_TRIANGLES, 0, 36);

        glBindFramebuffer(GL_FRAMEBUFFER, 0); // release

        
        

        glViewport(0, 0, info.getWindowWidth(), info.getWindowHeight());
        GLAPIHelper.glClearBuffer4f(GL_COLOR, 0,  0.0f, 0.0f, 0.3f, 1.0f); // blue
        GLAPIHelper.glClearBuffer1f(GL_DEPTH, 0, one);

        glBindTexture(GL_TEXTURE_2D, color_texture);

        glUseProgram(program2);

        proj_matrix_buffer.rewind();
        glUniformMatrix4(proj_location2, false, proj_matrix_buffer);
        mv_matrix_buffer.rewind();
        glUniformMatrix4(mv_location2, false, mv_matrix_buffer);

        glDrawArrays(GL_TRIANGLES, 0, 36);

        glBindTexture(GL_TEXTURE_2D, 0);

	}
	

	public static void main(String[] args) {
		new BasicFBO().run();
	}

}
