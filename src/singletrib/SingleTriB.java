package singletrib;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.MemoryUtilHelper;
import sb6.application.Application;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class SingleTriB extends Application {

	static enum BufferInitMethod {
		BUFFER_INIT_DIRECT,
		BUFFER_INIT_SUBMIT,
		BUFFER_INIT_MAPPED
	}

	BufferInitMethod buffer_init_method = BufferInitMethod.BUFFER_INIT_SUBMIT;
	
	private int program;
	private int vao;
	private int buffer;

	public SingleTriB() {
		super("Triangle with Vertex Positions via Buffer Object");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "layout (location = 0) in vec4 position;                           \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    // set given position                                         \n"+
	            "    gl_Position = position;                                       \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "out vec4 color;                                                   \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    color = vec4(0.0, 0.8, 1.0, 1.0);                             \n"+
	            "}                                                                 \n";
		
		program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);

		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);

		glDeleteShader(vs);
		glDeleteShader(fs);
		
		
		
        vao = glGenVertexArrays();
        glBindVertexArray(vao);


        
        // define the triangle vertices
		FloatBuffer data_buffer = BufferUtilsHelper.createBuffer(new float[]{
				0.25f, -0.25f, 0.5f, 1.0f,
				-0.25f, -0.25f, 0.5f, 1.0f,
				0.25f, 0.25f, 0.5f, 1.0f
		});
		data_buffer.rewind(); // prepare FloatBuffer for reading
		int buffer_size = BufferUtilsHelper.sizeof(data_buffer);

        
		
		// Generate a name for the buffer
		buffer = glGenBuffers();
		// Now bind it to the context using the GL_ARRAY_BUFFER binding point
		glBindBuffer(GL_ARRAY_BUFFER, buffer);

		// Here we test the methods to transmit data (main memory) to the buffer object (graphics memory)
		switch (buffer_init_method) {
		case BUFFER_INIT_DIRECT:
			// Specify the storage and submit immediately.
			glBufferData(GL_ARRAY_BUFFER, data_buffer, GL_STATIC_DRAW);
			break;
		case BUFFER_INIT_SUBMIT:
			// Specify the amount of storage we want to use for the buffer
			// without providing data now.
			glBufferData(GL_ARRAY_BUFFER, buffer_size, GL_STATIC_DRAW);
			
			// submitting data to the buffer object
			glBufferSubData(GL_ARRAY_BUFFER, 0, data_buffer);
			break;
		case BUFFER_INIT_MAPPED:
			// Specify the amount of storage we want to use for the buffer
			// without providing data now.
			glBufferData(GL_ARRAY_BUFFER, buffer_size, GL_STATIC_DRAW);
			// Get a pointer to the buffer’s data store
			ByteBuffer ptr = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY);
			// Copy our data into it...
			MemoryUtilHelper.memcpy(data_buffer, ptr, buffer_size);
			// Tell OpenGL that we’re done with the pointer
			glUnmapBuffer(GL_ARRAY_BUFFER);
			break;
		}
		
		
		// Now, describe the data to OpenGL, tell it where it is and which format it has
		glVertexAttribPointer(0, 		// Attribute 0
							4, 			// Four components
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							0, 			// Tightly packed
							MemoryUtil.NULL); // Offset zero (NULL pointer)
		// now turn configure automatic vertex fetching to fetch vertices from the 
		// object buffer specified by the 0th attribute of the vertex attribute array
		glEnableVertexAttribArray(0);
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
		Application app = new SingleTriB();
		app.run();
	}

}
