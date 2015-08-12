package tessellatedtri;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

public class TesselatedTri extends Application {
	private int program;
	private int vertexArrayObjectId;

	
	public TesselatedTri() {
		super("OpenGL SuperBible - Tessellated Triangle");
	}
	
	protected void startup()
	{
		
		program = compileShaders();
		
		vertexArrayObjectId = glGenVertexArrays();
		glBindVertexArray(vertexArrayObjectId);
	
		// set polygone mode to make tesselation result visible
		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
	}
	
	protected void shutdown()
	{
		glDeleteVertexArrays(vertexArrayObjectId);
		glDeleteProgram(program);
	}
	
	// Our rendering function
	protected void render(double currentTime) {
		glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);
		// Use the program object we created earlier for rendering
		glUseProgram(program);

		// Draw patches for the vertices
        glDrawArrays(GL_PATCHES, 0, 3);
	}
	
	private int compileShaders()
	{
		int vs;
		int fs;
		int program;
		// Source code for vertex shader
		String vs_source =
	            "#version 410 core                                                 \n" +
	            "                                                                  \n" +
	            "void main(void)                                                   \n" +
	            "{                                                                 \n" +
	            "    const vec4 vertices[] = vec4[](vec4( 0.25, -0.25, 0.5, 1.0),  \n" +
	            "                                   vec4(-0.25, -0.25, 0.5, 1.0),  \n" +
	            "                                   vec4( 0.25,  0.25, 0.5, 1.0)); \n" +
	            "                                                                  \n" +
	            "    gl_Position = vertices[gl_VertexID];                          \n" +
	            "}                                                                 \n";
		
		String tcs_source =
	            "#version 410 core                                                                 \n" +
	            "                                                                                  \n" +
	            "layout (vertices = 3) out;                                                        \n" +
	            "                                                                                  \n" +
	            "void main(void)                                                                   \n" +
	            "{                                                                                 \n" +
	            "    if (gl_InvocationID == 0)                                                     \n" +
	            "    {                                                                             \n" +
	            "        gl_TessLevelInner[0] = 3.0;                                               \n" +
	            "        gl_TessLevelOuter[0] = 5.0;                                               \n" +
	            "        gl_TessLevelOuter[1] = 5.0;                                               \n" +
	            "        gl_TessLevelOuter[2] = 5.0;                                               \n" +
	            "    }                                                                             \n" +
	            "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;     \n" +
	            "}                                                                                 \n";

		String tes_source =
	            "#version 410 core                                                                 \n" +
	            "                                                                                  \n" +
	            "layout (triangles, equal_spacing, cw) in;                                         \n" +
	            "                                                                                  \n" +
	            "void main(void)                                                                   \n" +
	            "{                                                                                 \n" +
	            "    gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) +                       \n" +
	            "                  (gl_TessCoord.y * gl_in[1].gl_Position) +                       \n" +
	            "                  (gl_TessCoord.z * gl_in[2].gl_Position);                        \n" +
	            "}                                                                                 \n";
		
		String fs_source =
	            "#version 410 core                                                 \n" +
	            "                                                                  \n" +
	            "out vec4 color;                                                   \n" +
	            "                                                                  \n" +
	            "void main(void)                                                   \n" +
	            "{                                                                 \n" +
	            "    color = vec4(0.0, 0.8, 1.0, 1.0);                             \n" +
	            "}                                                                 \n";
	            
		program = glCreateProgram();
		
		// Create and compile vertex shader
		vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
		Shader.checkCompilerResult(vs, "vertex");

        int tcs = glCreateShader(GL_TESS_CONTROL_SHADER);
        glShaderSource(tcs, tcs_source);
        glCompileShader(tcs);
        Shader.checkCompilerResult(tcs, "tess-ctrl");

        int tes = glCreateShader(GL_TESS_EVALUATION_SHADER);
        glShaderSource(tes, tes_source);
        glCompileShader(tes);
        Shader.checkCompilerResult(tes, "tess-eval");

		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "fragment");
		
		// Create program, attach shaders to it, and link it
		program = glCreateProgram();
		glAttachShader(program, vs);
		glAttachShader(program, tcs);
		glAttachShader(program, tes);
		glAttachShader(program, fs);
		glLinkProgram(program);
		glValidateProgram(program);

		Program.checkLinkerResult(program);
		
		// Delete the shaders as the program has them now
		glDeleteShader(vs);
		glDeleteShader(tcs);
		glDeleteShader(tes);
		glDeleteShader(fs);
		return program;
	}	
	

	public static void main(String[] args) {
		new TesselatedTri().run();
	}

}
