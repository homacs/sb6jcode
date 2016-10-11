package simplestencil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

/**
 * This was added to the examples to test stencils.
 * 
 * @author homac
 *
 */
public class SimpleStencil extends Application {

	private int rendering_program;
	private int vertexArrayObjectId;



	public SimpleStencil() {
		super("Simple Stencil Testing Example");
		info.flags.fullscreen = true;
	}

	protected void startup()
	{
		rendering_program = compileTriangleShaders();
		vertexArrayObjectId = glGenVertexArrays();
		glBindVertexArray(vertexArrayObjectId);
		
		glEnable(GL_CULL_FACE);
		
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

	}
	protected void shutdown()
	{
		glDeleteVertexArrays(vertexArrayObjectId);
		glDeleteProgram(rendering_program);
	}
	
	static final int NOOP = GL_KEEP;
	
	// Our rendering function
	protected void render(double currentTime) {
		glClearBuffer4f(GL_COLOR, 0, 0f, 0f, 0f, 1f);
		glClearBuffer1f(GL_DEPTH, 0, 1f);
		// Use the program object we created earlier for rendering
		glUseProgram(rendering_program);

		boolean stencil = true;

		// IMPORTANT NOTE: You need to create a framebuffer (i.e. display) which
		// support (contains) stencil bits! Refer to your display manager library 
		// (e.g. glfw) to learn how to setup your display.
		
		if (stencil) {
			//
			// Setup stencil testing
			//
			// Clear stencil buffer to 0
			glClearBuffer1i(GL_STENCIL, 0, 0);
			glEnable(GL_STENCIL_TEST);
			// Setup stencil state for border rendering
			glStencilFuncSeparate(GL_FRONT, GL_ALWAYS, 1, 0x1);
			glStencilOpSeparate(GL_FRONT, NOOP, NOOP, GL_REPLACE);
		}
		
		//
		// Render hud (here it is just a triangle in the center of the screen)
		//
		glVertexAttrib4f(0, 0f, 0f, 0.0f, 0.0f);
		glDrawArrays(GL_TRIANGLES, 0, 3);

		if (stencil) {
			// Now, border decoration pixels have a stencil value of 1
			// All other pixels have a stencil value of 0.
			
			// Setup stencil state for regular rendering (i.e. reset to original state),
			// fail if pixel would overwrite border
			glStencilFuncSeparate(GL_FRONT, GL_EQUAL, 0, 0x1);
			glStencilOpSeparate(GL_FRONT, NOOP, NOOP, NOOP);
		}
		//
		// Render scene (here it is a moving triangle)
		//
		glVertexAttrib4f(0, (float)Math.sin(currentTime) * 0.5f,
							(float)Math.cos(currentTime) * 0.6f,
							-0.5f, 0.0f);
		glDrawArrays(GL_TRIANGLES, 0, 3);
		
		if (stencil) {
			glDisable(GL_STENCIL_TEST);
		}

	}
	
	private int compileTriangleShaders()
	{
		int vertex_shader;
		int fragment_shader;
		int program;
		// Source code for vertex shader
		String vertex_shader_str = new String(
				"#version 430 core													\n"+
				"																	\n"+
	            "layout (location = 0) in vec4 offset;                              \n"+
	            "out vec4 vs_color;                                                 \n"+
				"void main(void)													\n"+
				"{																	\n"+
				"																	\n"+
				"	// Declare a hard-coded array of positions						\n"+
				"	const vec4 vertices[3] = vec4[3](vec4( 0.25, 0.25, 0., 1.0),	\n"+
				"							 vec4(-0.25, -0.25, 0., 1.0),			\n"+
				"							 vec4( 0.25, -0.25, 0., 1.0));			\n"+
				"																	\n"+
				"	// Index into our array using gl_VertexID						\n"+
	            "    gl_Position = vertices[gl_VertexID] + offset;                  \n"+
	            "    vs_color = offset;                                             \n"+
				"}																	\n"
			);
			
		
		
		// Source code for fragment shader
		String fragment_shader_str = new String (
			"#version 430 core 							\n" +
			"in vec4 vs_color;							\n" +
			"out vec4 color; 							\n" +
			"											\n" +
			"void main(void) 							\n" +
			"{ 											\n" +
			"  color = vec4(0.0, 0.8, 1.0, 1.0) + vs_color;\n" +
			"} \n"
		);
		
		// Create and compile vertex shader
		vertex_shader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertex_shader, vertex_shader_str);
		glCompileShader(vertex_shader);
		Shader.checkCompilerResult(vertex_shader, "in memory");

		// Create and compile fragment shader
		fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragment_shader, fragment_shader_str);
		glCompileShader(fragment_shader);
		Shader.checkCompilerResult(fragment_shader, "in memory");
		
		// Create program, attach shaders to it, and link it
		program = glCreateProgram();
		glAttachShader(program, vertex_shader);
		glAttachShader(program, fragment_shader);
		glLinkProgram(program);
		glValidateProgram(program);

		Program.checkLinkerResult(program);
		
		// Delete the shaders as the program has them now
		glDeleteShader(vertex_shader);
		glDeleteShader(fragment_shader);
		return program;
	}	
	
	
	
	public static void main(String[] args) {
		new SimpleStencil().run();
	}

}
