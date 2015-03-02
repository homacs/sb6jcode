package indexedcube;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.system.MemoryUtil;

import static sb6.vmath.MathHelper.*;
import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;


/**
 * Java implementation of indexedcube.cpp
 * 
 * @author homac
 *
 */

public class IndexedCube extends Application {
	
	/// set this constant to false, to draw only a single cube!
    private static final boolean MANY_CUBES = true;
    
	int          program;
    int          vao;
    int          position_buffer;
    int          index_buffer;
    int           mv_location;
    int           proj_location;


	public IndexedCube() {
		super("OpenGL SuperBible - Indexed Cube");
	}

	@Override
	protected void startup() {
        final String vs_source =
            "#version 420 core                                                  \n"+
            "                                                                   \n"+
            "in vec4 position;                                                  \n"+
            "                                                                   \n"+
            "out VS_OUT                                                         \n"+
            "{                                                                  \n"+
            "    vec4 color;                                                    \n"+
            "} vs_out;                                                          \n"+
            "                                                                   \n"+
            "uniform mat4 mv_matrix;                                            \n"+
            "uniform mat4 proj_matrix;                                          \n"+
            "                                                                   \n"+
            "void main(void)                                                    \n"+
            "{                                                                  \n"+
            "    gl_Position = proj_matrix * mv_matrix * position;              \n"+
            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n"+
            "}                                                                  \n";

        final String fs_source =
            "#version 420 core                                                  \n"+
            "                                                                   \n"+
            "out vec4 color;                                                    \n"+
            "                                                                   \n"+
            "in VS_OUT                                                          \n"+
            "{                                                                  \n"+
            "    vec4 color;                                                    \n"+
            "} fs_in;                                                           \n"+
            "                                                                   \n"+
            "void main(void)                                                    \n"+
            "{                                                                  \n"+
            "    color = fs_in.color;                                           \n"+
            "}                                                                  \n";

        
        program = glCreateProgram();
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source);
        glCompileShader(fs);
        Shader.checkCompilerResult(fs);
        
        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);
        Shader.checkCompilerResult(vs);
        
        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);
        Shader.checkLinkerResult(program);
        
        
        mv_location = glGetUniformLocation(program, "mv_matrix");
        proj_location = glGetUniformLocation(program, "proj_matrix");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        
        
        ShortBuffer vertex_indices = BufferUtilsHelper.createShortBuffer(new short[]
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
        });

        FloatBuffer vertex_positions = BufferUtilsHelper.createFloatBuffer(new float []
        {
            -0.25f, -0.25f, -0.25f,
            -0.25f,  0.25f, -0.25f,
             0.25f, -0.25f, -0.25f,
             0.25f,  0.25f, -0.25f,
             0.25f, -0.25f,  0.25f,
             0.25f,  0.25f,  0.25f,
            -0.25f, -0.25f,  0.25f,
            -0.25f,  0.25f,  0.25f,
        });

        position_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, position_buffer);
        glBufferData(GL_ARRAY_BUFFER,
                     vertex_positions,
                     GL_STATIC_DRAW);
		// determine vertex attribute index of glsl variable 'position' for vertex positions
		int attr_position = glGetAttribLocation(program, "position");
		if (attr_position == -1) fatal("vertex attribute index for 'position' not found");

        glVertexAttribPointer(attr_position, 3, GL_FLOAT, false, 0, MemoryUtil.NULL);
        glEnableVertexAttribArray(attr_position);

        index_buffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_buffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                     vertex_indices,
                     GL_STATIC_DRAW);

        glEnable(GL_CULL_FACE);
        // glFrontFace(GL_CW);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

	}

	@Override
	protected void render(double currentTime) {
        int i;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        Application.glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);
        Application.glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        if (MANY_CUBES) {
	        for (i = 0; i < 24; i++)
	        {
	            float f = (float)i + (float)currentTime * 0.3f;
	            Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -20.0f) 
	            				.mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f))
	                            .mul(Matrix4x4f.rotate((float)currentTime * 21.0f, 1.0f, 0.0f, 0.0f))
	                            .mul(Matrix4x4f.translate(sinf(2.1f * f) * 2.0f, 
	                            						cosf(1.7f * f) * 2.0f,
	                                                    sinf(1.3f * f) * cosf(1.5f * f) * 2.0f));
	            glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
	            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
	        }
        } else  {
	        float f = (float)currentTime * 0.3f;
	        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f);
	       	mv_matrix.mul(Matrix4x4f.translate(sinf(2.1f * f) * 0.5f,
	                                                    cosf(1.7f * f) * 0.5f,
	                                                    sinf(1.3f * f) * cosf(1.5f * f) * 2.0f));
	       	mv_matrix.mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f));
	       	mv_matrix.mul(Matrix4x4f.rotate((float)currentTime * 81.0f, 1.0f, 0.0f, 0.0f));
	        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
	        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
        }
	}


	@Override
	protected void shutdown() {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glDeleteBuffers(position_buffer);
	}
	
	public static void main (String[] args) {
		Application app = new IndexedCube();
		app.run();
	}

}
