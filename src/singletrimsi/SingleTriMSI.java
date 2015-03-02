package singletrimsi;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.shader.Shader;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class SingleTriMSI extends Application {
	
	private int program;
	private int vao;
	private int position_buffer;

	private int color_buffer;

	public SingleTriMSI() {
		super("Triangle with Multiple Shader Inputs");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "layout (location = 0) in vec3 position;                           \n"+
	            "layout (location = 1) in vec3 color;                              \n"+
	            "                                                                  \n"+
	            "out vec3 vs_color;                                                \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    // set given position                                         \n"+
	            "    gl_Position = vec4(position,1.0);                             \n"+
	            "    vs_color = color;                                             \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "in vec3 vs_color;                                                 \n"+
	            "out vec4 color;                                                   \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    color = vec4(vs_color, 1.0);                                  \n"+
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


        
        // define the triangle vertices
		FloatBuffer positions = BufferUtilsHelper.createBuffer(new float[]{
				0.25f, -0.25f, 0.5f,
				-0.25f, -0.25f, 0.5f,
				0.25f, 0.25f, 0.5f
		});
		positions.rewind(); // prepare FloatBuffer for reading
        
		// Generate a name for the buffer
		position_buffer = glGenBuffers();
		// Now bind it to the context using the GL_ARRAY_BUFFER binding point
		glBindBuffer(GL_ARRAY_BUFFER, position_buffer);

		// Specify the storage and submit immediately.
		glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW);
		
		// determine vertex attribute index of glsl variable 'position' for vertex positions
		int attr_position = glGetAttribLocation(program, "position");
		if (attr_position == -1) fatal("vertex attribute index for 'position' not found");
		
		// set format of buffer object content for 'position' data 
		glVertexAttribPointer(attr_position, // Attribute 'position'
							3, 			// Now just 3 components
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							0, 			// Tightly packed
							MemoryUtil.NULL); // Offset zero (NULL pointer)
		// now configure automatic vertex fetching to fetch vertices from the 
		// object buffer with the format specified by the 0th attribute of the vertex attribute array
		glEnableVertexAttribArray(attr_position);


		// green color for all vertices
		FloatBuffer color = BufferUtilsHelper.createBuffer(new float[]{
				0.f, 1.f, 0.0f,
				0.f, 1.f, 0.0f,
				0.f, 1.f, 0.0f,
		});
		color.rewind(); // prepare FloatBuffer for reading
		
		// Generate a name for the buffer
		color_buffer = glGenBuffers();
		// Now bind it to the context using the GL_ARRAY_BUFFER binding point
		glBindBuffer(GL_ARRAY_BUFFER, color_buffer);

		// Specify the storage and submit immediately.
		glBufferData(GL_ARRAY_BUFFER, color, GL_STATIC_DRAW);
		

		// determine vertex attribute index of glsl variable 'color' for vertex positions
		int attr_color = glGetAttribLocation(program, "color");
		// set format of buffer object content for 'position' data 
		glVertexAttribPointer(attr_color, // Attribute 'position'
							3, 			// 3 components (rgb) only
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							0, 			// Tightly packed
							MemoryUtil.NULL); // Offset zero (NULL pointer)
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
		Application app = new SingleTriMSI();
		app.run();
	}

}
