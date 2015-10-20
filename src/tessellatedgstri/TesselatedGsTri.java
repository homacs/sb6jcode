package tessellatedgstri;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

/**
 * Application of Tesselation and Geometry Shader to a triangle.
 * 
 * @author homac
 *
 */
public class TesselatedGsTri extends Application {
	private int program;
	private int vertexArrayObjectId;

	
	public TesselatedGsTri() {
		super("OpenGL SuperBible - Tessellation and Geometry Shaders");
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

		// larger points
		glPointSize(5.0f);
		// Draw patches for the vertices
        glDrawArrays(GL_PATCHES, 0, 3);
	}
	
	private int compileShaders()
	{
		int vs;
		int fs;
		int program;
		//
		// Source code for vertex shader. It creates three 
		// vertices for the triangle in a plane parallel to 
		// the viewer in a distance of 0.5 units.
		// The vertex shader is called once for each vertex 
		// and its task is to return the position of the 
		// vertex.
		// According to the 3rd parameter of glDrawArrays, we
		// give in method render(), it will be called three 
		// times. With each call the gl_VertexID is incremented 
		// by 1. So, each time the vertex shader returns one of
		// the vertices of the triangle declared in 'vertices'.
		// The order of vertices returned by the vertex shader
		// has to match the winding order selected in the 
		// tesselation shader. There we expect clock-wise winding 
		// order, so we have to return vertices beginning at one 
		// corner of the triangle in clock-wise order.
		//
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

		//
		// This is the source code of the tesselation control 
		// shader. Given a patch (non-tesselated polygon from the 
		// vertex fetching stage), its task is to set the tesselation 
		// levels for inner and outer region of the triangle.
		// The tcs is called for each vertex too. We set the 
		// tesselation levels just once when the first vertex comes in.
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
	            "// given patch will be tesslated into triangles with equal spacing                \n" +
	            "layout (triangles, equal_spacing, cw) in;                                         \n" +
	            "                                                                                  \n" +
	            "void main(void)                                                                   \n" +
	            "{                                                                                 \n" +
	            "    // gl_TessCoord contains the barymetric coordinate of the current             \n" +
	            "    // vertex of a triangle generated by the tesselation engine.                  \n" +
	            "    // gl_in provides access to all vertices of the original polygon              \n" +
	            "    // which is tesselated.                                                       \n" +
	            "    // We have to transform the position of the vertex back into cartesian coords \n" +
	            "    gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) +                       \n" +
	            "                  (gl_TessCoord.y * gl_in[1].gl_Position) +                       \n" +
	            "                  (gl_TessCoord.z * gl_in[2].gl_Position);                        \n" +
	            "}                                                                                 \n";
		
		String gs_source =
	            "#version 410 core                                                                  \n" +
	            "                                                                                   \n" +
	            "layout (triangles) in;                                                             \n" +
	            "layout (points, max_vertices = 3) out;                                             \n" +
	            "                                                                                   \n" +
	            "void main(void)                                                                    \n" +
	            "{                                                                                  \n" +
	            "    int i;                                                                         \n" +
	            "                                                                                   \n" +
	            "    for (i = 0; i < gl_in.length(); i++)                                           \n" +
	            "    {                                                                              \n" +
	            "        gl_Position = gl_in[i].gl_Position;                                        \n" +
	            "        EmitVertex();                                                              \n" +
	            "    }                                                                              \n" +
	            "    // actually not needed, because EndPrimitive() is called automatically         \n" +
	            "	 // after leaving main                                                          \n" +
	            "    EndPrimitive();                                                                \n" +
	            "}                                                                                  \n";
		
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

        int gs = glCreateShader(GL_GEOMETRY_SHADER);
        glShaderSource(gs, gs_source);
        glCompileShader(gs);
        Shader.checkCompilerResult(tes, "gemeotry");

		fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "fragment");
		
		// Create program, attach shaders to it, and link it
		program = glCreateProgram();
		glAttachShader(program, vs);
		glAttachShader(program, tcs);
		glAttachShader(program, tes);
		glAttachShader(program, gs);
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
		new TesselatedGsTri().run();
	}

}
