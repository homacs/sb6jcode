package movingtri;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import sb6.Application;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class MovingTri extends Application {


	private int program;
	private FloatBuffer backgroundColor4f;
	private int vertexArrayObjectId;

	public MovingTri() {
		super("Updating a vertex attribute");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 410 core                                                 \n"+
	            "                                                                  \n"+
	            "layout (location = 0) in vec4 offset;                             \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    const vec4 vertices[] = vec4[](vec4( 0.25, -0.25, 0.5, 1.0),  \n"+
	            "                                   vec4(-0.25, -0.25, 0.5, 1.0),  \n"+
	            "                                   vec4( 0.25,  0.25, 0.5, 1.0)); \n"+
	            "                                                                  \n"+
	            "    // Add 'offset' to our hard-coded vertex position             \n"+
	            "    gl_Position = vertices[gl_VertexID] + offset;                 \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 410 core                                                 \n"+
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
		
		vertexArrayObjectId = glGenVertexArrays();
		glBindVertexArray(vertexArrayObjectId);
		
		
		backgroundColor4f = BufferUtils.createFloatBuffer(4);
	}

	@Override
	protected void shutdown() {
		glDeleteProgram(program);
	}

	@Override
	protected void render(double currentTime) {
		backgroundColor4f.put(0, (float)Math.sin(currentTime) * 0.5f + 0.5f);
		backgroundColor4f.put(1, (float)Math.cos(currentTime) * 0.5f + 0.5f);
		backgroundColor4f.put(2, 0.f);
		backgroundColor4f.put(3, 1.f);
		glClearBuffer(GL_COLOR, 0, backgroundColor4f);
		// Use the program object we created earlier for rendering
		glUseProgram(program);
		// Update the value of input attribute 0
		glVertexAttrib4f(0, (float)Math.sin(currentTime) * 0.5f,
							(float)Math.cos(currentTime) * 0.6f,
							0.0f, 0.0f);
		// Draw one triangle
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	
	public static void main (String[] args) {
		Application app = new MovingTri();
		app.run();
	}

}
