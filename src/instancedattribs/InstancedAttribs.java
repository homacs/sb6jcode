package instancedattribs;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

import java.nio.FloatBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Shader;


/**
 * Java implementation of instancedattribs.cpp
 * 
 * @author homac
 *
 */

public class InstancedAttribs extends Application {
    int      square_buffer;
    int      square_vao;

    int      square_program;

	private static final String square_vs_source = 
		    "#version 410 core                                                               \n"+
		    "                                                                                \n"+
		    "layout (location = 0) in vec4 position;                                         \n"+
		    "layout (location = 1) in vec4 instance_color;                                   \n"+
		    "layout (location = 2) in vec4 instance_position;                                \n"+
		    "                                                                                \n"+
		    "out Fragment                                                                    \n"+
		    "{                                                                               \n"+
		    "    vec4 color;                                                                 \n"+
		    "} fragment;                                                                     \n"+
		    "                                                                                \n"+
		    "void main(void)                                                                 \n"+
		    "{                                                                               \n"+
		    "    gl_Position = (position + instance_position) * vec4(0.25, 0.25, 1.0, 1.0);    \n"+
		    "    fragment.color = instance_color;                                            \n"+
		    "}                                                                               \n";


		private static final String square_fs_source =
		    "#version 410 core                                                                \n"+
		    "precision highp float;                                                           \n"+
		    "                                                                                 \n"+
		    "in Fragment                                                                      \n"+
		    "{                                                                                \n"+
		    "    vec4 color;                                                                  \n"+
		    "} fragment;                                                                      \n"+
		    "                                                                                 \n"+
		    "out vec4 color;                                                                  \n"+
		    "                                                                                 \n"+
		    "void main(void)                                                                  \n"+
		    "{                                                                                \n"+
		    "    color = fragment.color;                                                      \n"+
		    "}                                                                                \n";


	public InstancedAttribs() {
		super("OpenGL SuperBible - Instanced Attributes");
	}

	@Override
	protected void startup() throws Throwable {
        final FloatBuffer square_vertices = BufferUtilsHelper.createFloatBuffer(new float[]
        {
            -1.0f, -1.0f, 0.0f, 1.0f,
             1.0f, -1.0f, 0.0f, 1.0f,
             1.0f,  1.0f, 0.0f, 1.0f,
            -1.0f,  1.0f, 0.0f, 1.0f
        });

        final FloatBuffer instance_colors = BufferUtilsHelper.createFloatBuffer(new float[]
        {
            1.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f
        });

        final FloatBuffer instance_positions = BufferUtilsHelper.createFloatBuffer(new float[]
        {
            -2.0f, -2.0f, 0.0f, 0.0f,
             2.0f, -2.0f, 0.0f, 0.0f,
             2.0f,  2.0f, 0.0f, 0.0f,
            -2.0f,  2.0f, 0.0f, 0.0f
        });


        square_vao = glGenVertexArrays();
        square_buffer = glGenBuffers();
        glBindVertexArray(square_vao);
        glBindBuffer(GL_ARRAY_BUFFER, square_buffer);
        glBufferData(GL_ARRAY_BUFFER, BufferUtilsHelper.sizeof(square_vertices) 
        		+ BufferUtilsHelper.sizeof(instance_colors) 
        		+ BufferUtilsHelper.sizeof(instance_positions), GL_STATIC_DRAW);

        int offset = 0;
        glBufferSubData(GL_ARRAY_BUFFER, offset, square_vertices);
        glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, offset);
        offset += BufferUtilsHelper.sizeof(square_vertices);

        glBufferSubData(GL_ARRAY_BUFFER, offset, instance_colors);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, offset);
        offset += BufferUtilsHelper.sizeof(instance_colors);
        
        glBufferSubData(GL_ARRAY_BUFFER, offset, instance_positions);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, 0, offset);
        offset += BufferUtilsHelper.sizeof(instance_positions);


        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glVertexAttribDivisor(1, 1);
        glVertexAttribDivisor(2, 1);

        square_program = glCreateProgram();

        int square_vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(square_vs, square_vs_source);
        glCompileShader(square_vs);
        Shader.checkCompilerResult(square_vs);
        glAttachShader(square_program, square_vs);
        
        int square_fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(square_fs, square_fs_source);
        glCompileShader(square_fs);
        Shader.checkCompilerResult(square_fs);
        glAttachShader(square_program, square_fs);

        glLinkProgram(square_program);
        Shader.checkLinkerResult(square_program);
        
        glDeleteShader(square_vs);
        glDeleteShader(square_fs);

	}

	@Override
	protected void shutdown() {
        glDeleteProgram(square_program);
        glDeleteBuffers(square_buffer);
        glDeleteVertexArrays(square_vao);
	}
	
	@Override
	protected void render(double currentTime) {
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);

        glUseProgram(square_program);
        glBindVertexArray(square_vao);
        glDrawArraysInstanced(GL_TRIANGLE_FAN, 0, 4, 4);
	}


	public static void main (String[] args) {
		Application app = new InstancedAttribs();
		app.run();
	}

}
