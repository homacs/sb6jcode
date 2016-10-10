package noperspective;

import java.nio.ByteBuffer;


import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;

public class NoPerspective extends Application {
    private int          program;
    private int          vao;
    private int          tex_checker;
    private volatile boolean            paused;
    private volatile int            use_perspective;
    private double last_time = 0.0;
    private double total_time = 0.0;

    class Uniforms
    {
    	int       mvp;
    	int       use_perspective;
    }
    private Uniforms uniforms = new Uniforms();

	public NoPerspective() {
		super("OpenGL SuperBible - Perspective");
	}
	
    protected void startup()
    {
        String vs_source =
            "#version 410 core                                                                  \n" +
            "                                                                                   \n" +
            "out VS_OUT                                                                         \n" +
            "{                                                                                  \n" +
            "    vec2 tc;                                                                       \n" +
            "    noperspective vec2 tc_np;                                                      \n" +
            "} vs_out;                                                                          \n" +
            "                                                                                   \n" +
            "uniform mat4 mvp;                                                                  \n" +
            "                                                                                   \n" +
            "void main(void)                                                                    \n" +
            "{                                                                                  \n" +
            "    const vec4 vertices[] = vec4[](vec4(-0.5, -0.5, 0.0, 1.0),                     \n" +
            "                                   vec4( 0.5, -0.5, 0.0, 1.0),                     \n" +
            "                                   vec4(-0.5,  0.5, 0.0, 1.0),                     \n" +
            "                                   vec4( 0.5,  0.5, 0.0, 1.0));                    \n" +
            "                                                                                   \n" +
            "    vec2 tc = (vertices[gl_VertexID].xy + vec2(0.5));                              \n" +
            "    vs_out.tc = tc;                                                                \n" +
            "    vs_out.tc_np = tc;                                                             \n" +
            "    gl_Position = mvp * vertices[gl_VertexID];                                     \n" +
            "}                                                                                  \n"
        ;

        String fs_source =
            "#version 410 core                                                 \n" +
            "                                                                  \n" +
            "out vec4 color;                                                   \n" +
            "                                                                  \n" +
            "uniform sampler2D tex_checker;                                    \n" +
            "                                                                  \n" +
            "uniform bool use_perspective = true;                              \n" +
            "                                                                  \n" +
            "in VS_OUT                                                         \n" +
            "{                                                                 \n" +
            "    vec2 tc;                                                      \n" +
            "    noperspective vec2 tc_np;                                     \n" +
            "} fs_in;                                                          \n" +
            "                                                                  \n" +
            "void main(void)                                                   \n" +
            "{                                                                 \n" +
            "    vec2 tc = mix(fs_in.tc_np, fs_in.tc, bvec2(use_perspective));        \n" +
            "    color = texture(tex_checker, tc).rrrr;                        \n" +
            "}                                                                 \n"
        ;


        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);


        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);

        program = Program.link(true, vs, fs);

        uniforms.mvp = glGetUniformLocation(program, "mvp");
        uniforms.use_perspective = glGetUniformLocation(program, "use_perspective");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        ByteBuffer checker_data = BufferUtilsHelper.createByteBuffer(new byte[]
        {
            0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF,
            (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00,
            0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF,
            (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00,
            0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF,
            (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00,
            0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF,
            (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00, (byte)0xFF, 0x00,
        });

        tex_checker = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex_checker);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_R8, 8, 8);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 8, 8, GL_RED, GL_UNSIGNED_BYTE, checker_data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }

    protected void render(double currentTime)
    {

        if (!paused)
            total_time += (currentTime - last_time);
        last_time = currentTime;

        float t = (float)total_time * 14.3f;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -1.5f) 
                                .mul(Matrix4x4f.rotate(t, 0.0f, 1.0f, 0.0f));
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f, 1000.0f);

        glUseProgram(program);

        glUniformMatrix4(uniforms.mvp, false, proj_matrix.mul(mv_matrix).toFloatBuffer());
        glUniform1i(uniforms.use_perspective, use_perspective);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    protected void shutdown()
    {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
    }

    protected void onKey(int key, int action)
    {
        if (action == 1)
        {
            switch (key)
            {
                case 'I': use_perspective = (use_perspective != 0 ? 0 : 1);
                    break;
                case 'P': paused = !paused;
                    break;
                default:
                    break;
            };
        }
    }

	public static void main(String[] args) {
		new NoPerspective().run();
	}
}
