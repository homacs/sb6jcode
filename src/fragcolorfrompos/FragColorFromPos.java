package fragcolorfrompos;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

public class FragColorFromPos extends Application {
	
	private int rendering_program;
	private int vertexArrayObjectId;

	
	public FragColorFromPos() {
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
		glClearBuffer4f(GL_COLOR, 0,0.0f, 0.25f, 0.0f, 1.0f);
		// Use the program object we created earlier for rendering
		glUseProgram(rendering_program);
		// Draw one point
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}
	
	private int compileShaders()
	{
		final boolean INTERPOLATED_COLOR = true;
		int vs;
		int fs;
		String vs_source, fs_source;
		int program;
		if (!INTERPOLATED_COLOR) {
			vs_source = new String(
		            "#version 420 core                                                          \n" +
		            "                                                                           \n" +
		            "void main(void)                                                            \n" +
		            "{                                                                          \n" +
		            "    const vec4 vertices[] = vec4[](vec4( 0.25, -0.25, 0.5, 1.0),           \n" +
		            "                                   vec4(-0.25, -0.25, 0.5, 1.0),           \n" +
		            "                                   vec4( 0.25,  0.25, 0.5, 1.0));          \n" +
		            "                                                                           \n" +
		            "    gl_Position = vertices[gl_VertexID];                                   \n" +
		            "}                                                                          \n"
			);
			
			
			// fragment shader that determines colors for fragments based on 
			// their position (FragCoord)
			fs_source = new String (
		            "#version 420 core                                                          \n" +
		            "                                                                           \n" +
		            "out vec4 color;                                                            \n" +
		            "                                                                           \n" +
		            "void main(void)                                                            \n" +
		            "{                                                                          \n" +
		            "    color = vec4(sin(gl_FragCoord.x * 0.25) * 0.5 + 0.5,                   \n" +
		            "                 cos(gl_FragCoord.y * 0.25) * 0.5 + 0.5,                   \n" +
		            "                 sin(gl_FragCoord.x * 0.15) * cos(gl_FragCoord.y * 0.1),   \n" +
		            "                 1.0);                                                     \n" +
		            "}                                                                          \n"
			);
		} else {
			// Vertex shader assigns each vertex a different color
			vs_source = new String(
		            "#version 420 core                                                          \n" +
		            "                                                                           \n" +
		            "out vec4 vs_color; 														\n" +
		            "void main(void)                                                            \n" +
		            "{                                                                          \n" +
		            "    const vec4 vertices[] = vec4[](vec4( 0.25, -0.25, 0.5, 1.0),           \n" +
		            "                                   vec4(-0.25, -0.25, 0.5, 1.0),           \n" +
		            "                                   vec4( 0.25,  0.25, 0.5, 1.0));          \n" +
		            "    const vec4 colors[] = vec4[](vec4(1.0, 0.0, 0.0, 1.0),                 \n" +
		            "                                 vec4(0.0, 1.0, 0.0, 1.0),                 \n" +
		            "                                 vec4(0.0, 0.0, 1.0, 1.0));                \n" +
		            "                                                                           \n" +
		            "    gl_Position = vertices[gl_VertexID];                                   \n" +
		            "    vs_color = colors[gl_VertexID];                                        \n" +
		            "}                                                                          \n"
			);
			
			
			// Fragment shader just outputs the colour received for the given fragment by the
			// rasterizer. But the rasterizer interpolated the value for each fragment 
			// based on the values given for the vertices of the primitive (triangle). 
			fs_source = new String (
		            "#version 420 core                                                          \n" +
		            "                                                                           \n" +
		            "in vec4 vs_color;                                                          \n" +
		            "out vec4 color;                                                            \n" +
		            "                                                                           \n" +
		            "void main(void)                                                            \n" +
		            "{                                                                          \n" +
		            "    color = vs_color;                                                      \n" +
		            "}                                                                          \n"
			);
		}
		// Create and compile vertex shader
		vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
		Shader.checkCompilerResult(vs, "in memory");

		// Create and compile fragment shader
		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "in memory");
		
		// Create program, attach shaders to it, and link it
		program = glCreateProgram();
		glAttachShader(program, vs);
		glAttachShader(program, fs);
		glLinkProgram(program);
		glValidateProgram(program);
		Program.checkLinkerResult(program);
		
		// Delete the shaders as the program has them now
		glDeleteShader(vs);
		glDeleteShader(fs);
		return program;
	}	
	

	public static void main(String[] args) {
		new FragColorFromPos().run();
	}

}
