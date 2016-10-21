package shapedpoints;


import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class ShapedPoints extends Application {
    private int          render_prog;
    private int          render_vao;

	public ShapedPoints() {
		super("OpenGL SuperBible - Shaped Points");
	}


    protected void startup()
    {
        int  vs, fs;

        String fs_source =
            "#version 410 core                                              \n" +
            "                                                               \n" +
            "layout (location = 0) out vec4 color;                          \n" +
            "                                                               \n" +
            "flat in int shape;                                             \n" +
            "                                                               \n" +
            "void main(void)                                                \n" +
            "{                                                              \n" +
            "    color = vec4(1.0);                                         \n" +
            "    vec2 p = gl_PointCoord * 2.0 - vec2(1.0);                  \n" +
            "    if (shape == 0)                                            \n" +
            "    {                                                          \n" +
            "        // simple disc shape                                   \n" +
            "        if (dot(p, p) > 1.0)                                   \n" +
            "            discard;                                           \n" +
            "    }                                                          \n" +
            "    else if (shape == 1)                                       \n" +
            "    {                                                          \n" +
            "        // Hollow circle                                       \n" +
            "        if (dot(p, p) > sin(atan(p.y, p.x) * 5.0))             \n" +
            "            discard;                                           \n" +
            "    }                                                          \n" +
            "    else if (shape == 2)                                       \n" +
            "    {                                                          \n" +
            "        // Flower shape                                        \n" +
            "        if (abs(0.8 - dot(p, p)) > 0.2)                        \n" +
            "            discard;                                           \n" +
            "    }                                                          \n" +
            "    else if (shape == 3)                                       \n" +
            "    {                                                          \n" +
            "        // Bowtie                                              \n" +
            "        if (abs(p.x) < abs(p.y))                               \n" +
            "            discard;                                           \n" +
            "    }                                                          \n" +
            "}                                                              \n"
        ;

        String vs_source =
            "#version 410 core                                                      \n" +
            "                                                                       \n" +
            "flat out int shape;                                                    \n" +
            "                                                                       \n" +
            "void main(void)                                                        \n" +
            "{                                                                      \n" +
            "    const vec4[4] position = vec4[4](vec4(-0.4, -0.4, 0.5, 1.0),       \n" +
            "                                     vec4( 0.4, -0.4, 0.5, 1.0),       \n" +
            "                                     vec4(-0.4,  0.4, 0.5, 1.0),       \n" +
            "                                     vec4( 0.4,  0.4, 0.5, 1.0));      \n" +
            "    gl_Position = position[gl_VertexID];                               \n" +
            "    shape = gl_VertexID;                                               \n" +
            "}                                                                      \n"
        ;

        vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        render_prog = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        render_vao = glGenVertexArrays();
        glBindVertexArray(render_vao);
    }

    protected void render(double currentTime)
    {
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(render_prog);

        glPointSize(200.0f);
        glBindVertexArray(render_vao);
        glDrawArrays(GL_POINTS, 0, 4);
    }
	

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		new ShapedPoints().run();
	}


}
