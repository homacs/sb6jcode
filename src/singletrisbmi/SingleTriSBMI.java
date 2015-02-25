package singletrisbmi;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import sb6.Application;
import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.MemoryUtilHelper;
import sb6.Shader;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class SingleTriSBMI extends Application {

	static enum BufferInitMethod {
		BUFFER_INIT_DIRECT,
		BUFFER_INIT_SUBMIT,
		BUFFER_INIT_MAPPED
	}

	BufferInitMethod buffer_init_method = BufferInitMethod.BUFFER_INIT_SUBMIT;
	
	private int program;
	private int vao;
	private int data_buffer;

	public SingleTriSBMI() {
		super("Triangle using a single buffer and multiple inputs");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 430 core                                                 \n"+
	            "// the two inputs - their location is defined by opengl           \n"+
	            "in vec3 position;                                                 \n"+
	            "in vec3 color;                                                    \n"+
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
				0.25f,  -0.25f, 0.5f,   0.f, 1.f, 0.0f,
				-0.25f, -0.25f, 0.5f,   0.f, 1.f, 0.0f,
				0.25f,   0.25f, 0.5f,   0.f, 1.f, 0.0f,
		});
		data.rewind(); // prepare FloatBuffer for reading
        int position_components = 3; // the 3 first float components belong to the position
		int data_row_size = (int)MemoryUtilHelper.offsetof(data, 6);
		
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
							position_components, 			// Now just 3 components
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							data_row_size, // size of one set of data for a vertex (including all attributes!)
							MemoryUtilHelper.offsetof(data, 0)); // Offset of the first vertex component (i.e. x value)
		// now configure automatic vertex fetching to fetch vertices from the 
		// object buffer with the format specified by the 0th attribute of the vertex attribute array
		glEnableVertexAttribArray(attr_position);


		// determine vertex attribute index of glsl variable 'color' for vertex positions
		int attr_color = glGetAttribLocation(program, "color");
		if (attr_color == -1) fatal("vertex attribute index for 'color' not found");
		// set format of buffer object content for 'position' data 
		glVertexAttribPointer(attr_color, // Attribute 'position'
							3, 			// 3 components (rgb) only
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							data_row_size, // size of one set of data for a vertex (including all attributes!)
							MemoryUtilHelper.offsetof(data, 3)); // Offset off first rgb component in a row (i.e. r value)
		// now enable automatic fetching from the object buffer according to the format
		// specified by the 1st attribute of the vertex attribute array
		glEnableVertexAttribArray(attr_color);
		
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
		// Draw one triangle
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	
	public static void main (String[] args) {
		Application app = new SingleTriSBMI();
		app.run();
	}

}
