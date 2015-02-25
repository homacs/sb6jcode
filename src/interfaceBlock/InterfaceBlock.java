package interfaceBlock;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import sb6.Application;
import sb6.Shader;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class InterfaceBlock extends Application {


	private int program;
	private FloatBuffer backgroundColor4f;
	private int vertexArrayObjectId;

	public InterfaceBlock() {
		super("Interface Blocks");
	}

	@Override
	protected void startup() {
		String vs_source = 
	            "#version 410 core                                                 \n"+
   	            "                                                                  \n"+
	            "// 'offset' and 'color' are input vertex attributes               \n"+
	            "layout (location = 0) in vec4 offset;                             \n"+
	            "layout (location = 1) in vec4 color;                              \n"+
	            "                                                                  \n"+
	            "// Declare VS_OUT as an output interface block    				   \n"+
	            "out VS_OUT {													   \n"+
	            "   vec4 color; 												   \n"+
	            "} vs_out;                                                         \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    const vec4 vertices[] = vec4[](vec4( 0.25, -0.25, 0.5, 1.0),  \n"+
	            "                                   vec4(-0.25, -0.25, 0.5, 1.0),  \n"+
	            "                                   vec4( 0.25,  0.25, 0.5, 1.0)); \n"+
	            "                                                                  \n"+
	            "    // Add 'offset' to our hard-coded vertex position             \n"+
	            "    gl_Position = vertices[gl_VertexID] + offset;                 \n"+
	            "                                                                  \n"+
	            "    // Output a fixed value for vs_color						   \n"+
	            "    vs_out.color = color;                                         \n"+
	            "                                                                  \n"+
	            "                                                                  \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 410 core                                                 \n"+
   	            "                                                                  \n"+
   	            "in VS_OUT { 													   \n"+
   	            "    vec4 color;                                                   \n"+
   	            "} fs_in;                                                          \n"+
   	            "                                                                  \n"+
	            "out vec4 color;                                                   \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "                                                                  \n"+
	            "    // simply assign the color we were given to the output color  \n"+
	            "    color = fs_in.color;				                           \n"+
	            "}                                                                 \n";
		
		program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
        Shader.checkCompilerResult(vs, "vertex");

		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
        Shader.checkCompilerResult(fs, "fragment");

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);
        Shader.checkLinkerResult(program);
        
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
		float r = (float)Math.sin(currentTime) * 0.5f + 0.5f;
		float g = (float)Math.cos(currentTime) * 0.5f + 0.5f;
		float b = 0.f;
		backgroundColor4f.put(0, r);
		backgroundColor4f.put(1, g);
		backgroundColor4f.put(2, b);
		backgroundColor4f.put(3, 1.f);
		glClearBuffer(GL_COLOR, 0, backgroundColor4f);
		// Use the program object we created earlier for rendering
		glUseProgram(program);
		// Update the value of input attribute 0 (offset)
		glVertexAttrib4f(0, (float)Math.sin(currentTime) * 0.5f,
							(float)Math.cos(currentTime) * 0.6f,
							0.0f, 0.0f);
		// Update the value of input attribute 1 (color) 
		// with the complementary color of the background
		glVertexAttrib4f(1, 1.f-r, 1f-g, 1.f-b, 1.0f);
		// Draw one triangle
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	
	public static void main (String[] args) {
		Application app = new InterfaceBlock();
		app.run();
	}

}
