package tessmodes;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL40.GL_PATCHES;
import static org.lwjgl.opengl.GL40.GL_PATCH_VERTICES;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL40.glPatchParameteri;
import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

public class TessModes  extends Application{
	static final int MAX_PROGRAMS = 4;
    int[]        program = new int[MAX_PROGRAMS];
    int          program_index;
    int          vao;

	public TessModes() {
		super("OpenGL SuperBible - Tessellation Modes");
		program_index = 0;
		init();
	}

	@Override
	protected void startup() throws Throwable {
        final String vs_source =
            "#version 420 core                                                 \n" +
            "                                                                  \n" +
            "void main(void)                                                   \n" +
            "{                                                                 \n" +
            "    const vec4 vertices[] = vec4[](vec4( 0.4, -0.4, 0.5, 1.0),    \n" +
            "                                   vec4(-0.4, -0.4, 0.5, 1.0),    \n" +
            "                                   vec4( 0.4,  0.4, 0.5, 1.0),    \n" +
            "                                   vec4(-0.4,  0.4, 0.5, 1.0));   \n" +
            "                                                                  \n" +
            "    gl_Position = vertices[gl_VertexID];                          \n" +
            "}                                                                 \n"
        ;

        final String tcs_source_triangles =
            "#version 420 core                                                                 \n" +
            "                                                                                  \n" +
            "layout (vertices = 3) out;                                                        \n" +
            "                                                                                  \n" +
            "void main(void)                                                                   \n" +
            "{                                                                                 \n" +
            "    if (gl_InvocationID == 0)                                                     \n" +
            "    {                                                                             \n" +
            "        gl_TessLevelInner[0] = 5.0;                                               \n" +
            "        gl_TessLevelOuter[0] = 8.0;                                               \n" +
            "        gl_TessLevelOuter[1] = 8.0;                                               \n" +
            "        gl_TessLevelOuter[2] = 8.0;                                               \n" +
            "    }                                                                             \n" +
            "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;     \n" +
            "}                                                                                 \n"
        ;

        final String tes_source_triangles =
            "#version 420 core                                                                 \n" +
            "                                                                                  \n" +
            "layout (triangles) in;                                                            \n" +
            "                                                                                  \n" +
            "void main(void)                                                                   \n" +
            "{                                                                                 \n" +
            "    gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) +                       \n" +
            "                  (gl_TessCoord.y * gl_in[1].gl_Position) +                       \n" +
            "                  (gl_TessCoord.z * gl_in[2].gl_Position);                        \n" +
            "}                                                                                 \n"
        ;

        final String tes_source_triangles_as_points =
            "#version 420 core                                                                 \n" +
            "                                                                                  \n" +
            "layout (triangles, point_mode) in;                                                \n" +
            "                                                                                  \n" +
            "void main(void)                                                                   \n" +
            "{                                                                                 \n" +
            "    gl_Position = (gl_TessCoord.x * gl_in[0].gl_Position) +                       \n" +
            "                  (gl_TessCoord.y * gl_in[1].gl_Position) +                       \n" +
            "                  (gl_TessCoord.z * gl_in[2].gl_Position);                        \n" +
            "}                                                                                 \n"
        ;

        final String tcs_source_quads =
            "#version 420 core                                                                   \n" +
            "                                                                                    \n" +
            "layout (vertices = 4) out;                                                          \n" +
            "                                                                                    \n" +
            "void main(void)                                                                     \n" +
            "{                                                                                   \n" +
            "    if (gl_InvocationID == 0)                                                       \n" +
            "    {                                                                               \n" +
            "        gl_TessLevelInner[0] = 9.0;                                                 \n" +
            "        gl_TessLevelInner[1] = 7.0;                                                 \n" +
            "        gl_TessLevelOuter[0] = 3.0;                                                 \n" +
            "        gl_TessLevelOuter[1] = 5.0;                                                 \n" +
            "        gl_TessLevelOuter[2] = 3.0;                                                 \n" +
            "        gl_TessLevelOuter[3] = 5.0;                                                 \n" +
            "    }                                                                               \n" +
            "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;       \n" +
            "}                                                                                   \n"
        ;

        final String tes_source_quads =
            "#version 420 core                                                                    \n" +
            "                                                                                     \n" +
            "layout (quads) in;                                                                   \n" +
            "                                                                                     \n" +
            "void main(void)                                                                      \n" +
            "{                                                                                    \n" +
            "    vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);       \n" +
            "    vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);       \n" +
            "    gl_Position = mix(p1, p2, gl_TessCoord.y);                                       \n" +
            "}                                                                                    \n"
        ;

        final String tcs_source_isolines =
            "#version 420 core                                                                   \n" +
            "                                                                                    \n" +
            "layout (vertices = 4) out;                                                          \n" +
            "                                                                                    \n" +
            "void main(void)                                                                     \n" +
            "{                                                                                   \n" +
            "    if (gl_InvocationID == 0)                                                       \n" +
            "    {                                                                               \n" +
            "        gl_TessLevelOuter[0] = 5.0;                                                 \n" +
            "        gl_TessLevelOuter[1] = 5.0;                                                 \n" +
            "    }                                                                               \n" +
            "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;       \n" +
            "}                                                                                   \n"
        ;

        /*
        final String tes_source_isolines =
            "#version 420 core                                                                    \n"
            "                                                                                     \n"
            "layout (isolines, equal_spacing, cw) in;                                             \n"
            "                                                                                     \n"
            "void main(void)                                                                      \n"
            "{                                                                                    \n"
            "    vec4 p1 = mix(gl_in[0].gl_Position, gl_in[1].gl_Position, gl_TessCoord.x);       \n"
            "    vec4 p2 = mix(gl_in[2].gl_Position, gl_in[3].gl_Position, gl_TessCoord.x);       \n"
            "    gl_Position = mix(p1, p2, gl_TessCoord.y);                                       \n"
            "}                                                                                    \n"
        ;
        */

        final String tes_source_isolines =
            "#version 420 core                                                                    \n" +
            "                                                                                     \n" +
            "layout (isolines) in;                                                                \n" +
            "                                                                                     \n" +
            "void main(void)                                                                      \n" +
            "{                                                                                    \n" +
            "    float r = (gl_TessCoord.y + gl_TessCoord.x / gl_TessLevelOuter[0]);              \n" +
            "    float t = gl_TessCoord.x * 2.0 * 3.14159;                                        \n" +
            "    gl_Position = vec4(sin(t) * r, cos(t) * r, 0.5, 1.0);                            \n" +
            "}                                                                                    \n"
        ;

        final String fs_source =
            "#version 420 core                                                  \n" +
            "                                                                   \n" +
            "out vec4 color;                                                    \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color = vec4(1.0);                                             \n" +
            "}                                                                  \n"
        ;

        int i;

        String vs_sources[] =
        {
            vs_source, 
            vs_source, 
            vs_source, 
            vs_source
        };

        String tcs_sources[] =
        {
            tcs_source_quads, 
            tcs_source_triangles, 
            tcs_source_triangles, 
            tcs_source_isolines
        };

        String tes_sources[] =
        {
            tes_source_quads, 
            tes_source_triangles, 
            tes_source_triangles_as_points, 
            tes_source_isolines
        };

        String fs_sources[] =
        {
            fs_source, 
            fs_source, 
            fs_source, 
            fs_source
        };

        for (i = 0; i < MAX_PROGRAMS; i++)
        {
            program[i] = glCreateProgram();
            
            int vs = Shader.compile(GL_VERTEX_SHADER, vs_sources[i]);
            int tcs = Shader.compile(GL_TESS_CONTROL_SHADER, tcs_sources[i]);
            int tes = Shader.compile(GL_TESS_EVALUATION_SHADER, tes_sources[i]);
            int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_sources[i]);

            program[i] = Program.link(true, vs, tcs, tes, fs);
            

            glDeleteShader(vs);
            glDeleteShader(tcs);
            glDeleteShader(tes);
            glDeleteShader(fs);
        }

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glPatchParameteri(GL_PATCH_VERTICES, 4);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		
	}

	@Override
	protected void render(double currentTime) throws Throwable {
        GLAPIHelper.glClearBuffer4f(GL_COLOR, 0,  0.0f, 0.0f, 0.0f, 0.0f); // black


        glUseProgram(program[program_index]);
        glDrawArrays(GL_PATCHES, 0, 4);
	}

	@Override
	protected void shutdown() throws Throwable {
        int i;
        glDeleteVertexArrays(vao);

        for (i = 0; i < MAX_PROGRAMS; i++)
        {
            glDeleteProgram(program[i]);
        }
	}


    protected void onKey(int key, int action)
    {
        if (action == 0)
            return;

        switch (key)
        {
            case 'M': program_index = (program_index + 1) % MAX_PROGRAMS;
                break;
        }
    }

    
    public static void main (String[] args) {
    	new TessModes().run();
    }
}
