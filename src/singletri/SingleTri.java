package singletri;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


import sb6.Application;
import sb6.Shader;

public class SingleTri extends Application {
	private int rendering_program;
	private int vertexArrayObjectId;

	
	public SingleTri() {
		super("Our very first OpenGL triangle");
	}
	
	protected void startup()
	{
		rendering_program = compileShaders();
		vertexArrayObjectId = glGenVertexArrays();
		glBindVertexArray(vertexArrayObjectId);
	}
	protected void shutdown()
	{
		glDeleteVertexArrays(vertexArrayObjectId);
		glDeleteProgram(rendering_program);
	}
	
	// Our rendering function
	protected void render(double currentTime) {
		glClearBuffer4f(GL_COLOR, 0, 
						(float)Math.sin(currentTime) * 0.5f + 0.5f,
						(float)Math.cos(currentTime) * 0.5f + 0.5f,
						0.f, 1.0f);
		// Use the program object we created earlier for rendering
		glUseProgram(rendering_program);
		// set point size to 40 pixels
		glPointSize(40.0f);
		// Draw one point
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}
	
	private int compileShaders()
	{
		int vertex_shader;
		int fragment_shader;
		int program;
		// Source code for vertex shader
		String vertex_shader_str = new String(
			"#version 430 core													\n"+
			"																	\n"+
			"void main(void)													\n"+
			"{																	\n"+
			"																	\n"+
			"	// Declare a hard-coded array of positions						\n"+
			"	const vec4 vertices[3] = vec4[3](vec4( 0.25, -0.25, 0.5, 1.0),	\n"+
			"							 vec4(-0.25, -0.25, 0.5, 1.0),			\n"+
			"							 vec4( 0.25, 0.25, 0.5, 1.0));			\n"+
			"																	\n"+
			"	// Index into our array using gl_VertexID						\n"+
			"	gl_Position = vertices[gl_VertexID];							\n"+
			"}																	\n"
		);
		
		
		// Source code for fragment shader
		String fragment_shader_str = new String (
			"#version 430 core 							\n" +
			" 											\n" +
			"out vec4 color; 							\n" +
			"											\n" +
			"void main(void) 							\n" +
			"{ 											\n" +
			"  color = vec4(0.0, 0.8, 1.0, 1.0); 		\n" +
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

		Shader.checkLinkerResult(program);
		
		// Delete the shaders as the program has them now
		glDeleteShader(vertex_shader);
		glDeleteShader(fragment_shader);
		return program;
	}	
	

	public static void main(String[] args) {
		new SingleTri().run();
	}

}
