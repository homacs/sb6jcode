package singletriuni;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.MemoryUtilHelper;
import sb6.application.Application;
import sb6.shader.Shader;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class SingleTriUni extends Application {

	static enum BufferInitMethod {
		BUFFER_INIT_DIRECT,
		BUFFER_INIT_SUBMIT,
		BUFFER_INIT_MAPPED
	}

	BufferInitMethod buffer_init_method = BufferInitMethod.BUFFER_INIT_SUBMIT;
	
	private int program;
	private int vao;
	private int data_buffer;

	private int uniform_color;

	public SingleTriUni() {
		super("Triangle using a uniform variable for its color");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 430 core                                                 \n"+
	            "// one input - its location is defined by opengl                  \n"+
	            "in vec3 position;                                                 \n"+
	            "// one uniform - its location is defined by opengl                \n"+
	            "uniform vec3 color;                                               \n"+
	            "                                                                  \n"+
	            "out vec3 vs_color;                                                \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    // set given position                                         \n"+
	            "    gl_Position = vec4(position,1.0); // convert in vec4          \n"+
	            "    vs_color = color;                                             \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "in vec3 vs_color; // received from vs                             \n"+
	            "out vec4 color;                                                   \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    color = vec4(vs_color, 1.0);  // convert in vec4              \n"+
	            "}                                                                 \n";
		
		program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
		Shader.checkCompilerResult(vs, "vs");
		
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "fs");

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);
		glValidateProgram(program);
        Shader.checkLinkerResult(program);
		glDeleteShader(vs);
		glDeleteShader(fs);
		
		
		
        vao = glGenVertexArrays();
        glBindVertexArray(vao);


        
        // define the triangle vertices and their color (ugly green)
		FloatBuffer data = BufferUtilsHelper.createBuffer(new float[]{
				0.25f,  -0.25f, 0.5f,
				-0.25f, -0.25f, 0.5f,
				0.25f,   0.25f, 0.5f,
		});
		data.rewind(); // prepare FloatBuffer for reading
        int position_components = 3; // the 3 first float components belong to the position
		int data_row_size = (int)MemoryUtilHelper.offsetof(data, 3);
		
		// Generate a name for the buffer
		data_buffer = glGenBuffers();
		// Now bind it to the context using the GL_ARRAY_BUFFER binding point
		glBindBuffer(GL_ARRAY_BUFFER, data_buffer);

		// Specify the storage size and type and submit immediately.
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
		
		// determine vertex attribute index of glsl variable 'position' for vertex positions
		int attr_position = glGetAttribLocation(program, "position");
		if (attr_position == -1) fatal("vertex attribute index for 'position' not found");
		
		// set format of buffer object content for 'position' data 
		glVertexAttribPointer(attr_position, // Attribute 'position'
							position_components, 			// just 3 components
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							data_row_size, // size of one set of data for a vertex (including all attributes!)
							MemoryUtilHelper.offsetof(data, 0)); // Offset of the first vertex component (i.e. x value)
		glEnableVertexAttribArray(attr_position);


		// determine uniform attribute index of glsl variable 'color'
		uniform_color = glGetUniformLocation(program, "color");
		if (uniform_color == -1) fatal("uniform attribute 'color' not found");
		
	}

	@Override
	protected void shutdown() {
		glDeleteProgram(program);
	}

	@Override
	protected void render(double currentTime) {
		GLAPIHelper.glClearBuffer4f(GL_COLOR, 0, (float)Math.sin(currentTime) * 0.5f + 0.5f,(float)Math.cos(currentTime) * 0.5f + 0.5f, 0f, 1f);
		// Use the program object we created earlier for rendering
		glUseProgram(program);
		// set uniform
		glUniform3f(uniform_color, 1.0f, 0.0f, 0.0f);
		
		// Draw one triangle
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	
	public static void main (String[] args) {
		Application app = new SingleTriUni();
		app.run();
	}

}
