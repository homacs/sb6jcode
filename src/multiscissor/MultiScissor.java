package multiscissor;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL41.*;

public class MultiScissor extends Application {
    private int          program;
    private int          vao;
    private int          position_buffer;
    private int          index_buffer;
    private int          uniform_buffer;

	public MultiScissor() {
		super("OpenGL SuperBible - Multiple Scissors");
	}
	
    protected void startup()
    {
        String vs_source =
            "#version 420 core                                                  \n" +
            "                                                                   \n" +
            "in vec4 position;                                                  \n" +
            "                                                                   \n" +
            "out VS_OUT                                                         \n" +
            "{                                                                  \n" +
            "    vec4 color;                                                    \n" +
            "} vs_out;                                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = position;                                        \n" +
            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n" +
            "}                                                                  \n"
        ;

        String gs_source =
            "#version 420 core                                                  \n" +
            "                                                                   \n" +
            "layout (triangles, invocations = 4) in;                            \n" +
            "layout (triangle_strip, max_vertices = 3) out;                     \n" +
            "                                                                   \n" +
            "layout (std140, binding = 0) uniform transform_block               \n" +
            "{                                                                  \n" +
            "    mat4 mvp_matrix[4];                                            \n" +
            "};                                                                 \n" +
            "                                                                   \n" +
            "in VS_OUT                                                          \n" +
            "{                                                                  \n" +
            "    vec4 color;                                                    \n" +
            "} gs_in[];                                                         \n" +
            "                                                                   \n" +
            "out GS_OUT                                                         \n" +
            "{                                                                  \n" +
            "    vec4 color;                                                    \n" +
            "} gs_out;                                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    for (int i = 0; i < gl_in.length(); i++)                       \n" +
            "    {                                                              \n" +
            "        gs_out.color = gs_in[i].color;                             \n" +
            "        gl_Position = mvp_matrix[gl_InvocationID] *                \n" +
            "                      gl_in[i].gl_Position;                        \n" +
            "        gl_ViewportIndex = gl_InvocationID;                        \n" +
            "        EmitVertex();                                              \n" +
            "    }                                                              \n" +
            "    EndPrimitive();                                                \n" +
            "}                                                                  \n"
        ;

        String fs_source =
            "#version 420 core                                                  \n" +
            "                                                                   \n" +
            "out vec4 color;                                                    \n" +
            "                                                                   \n" +
            "in GS_OUT                                                          \n" +
            "{                                                                  \n" +
            "    vec4 color;                                                    \n" +
            "} fs_in;                                                           \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color = fs_in.color;                                           \n" +
            "}                                                                  \n"
        ;

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int gs = Shader.compile(GL_GEOMETRY_SHADER, gs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);

        program = Program.link(true, vs, gs, fs);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        ShortBuffer vertex_indices = BufferUtilsHelper.createShortBuffer(new short[]{
            0, 1, 2,
            2, 1, 3,
            2, 3, 4,
            4, 3, 5,
            4, 5, 6,
            6, 5, 7,
            6, 7, 0,
            0, 7, 1,
            6, 0, 2,
            2, 4, 6,
            7, 5, 3,
            7, 3, 1
        });

        FloatBuffer vertex_positions = BufferUtilsHelper.createFloatBuffer(new float[]
        {
            -0.25f, -0.25f, -0.25f,
            -0.25f,  0.25f, -0.25f,
             0.25f, -0.25f, -0.25f,
             0.25f,  0.25f, -0.25f,
             0.25f, -0.25f,  0.25f,
             0.25f,  0.25f,  0.25f,
            -0.25f, -0.25f,  0.25f,
            -0.25f,  0.25f,  0.25f,
        });

        position_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, position_buffer);
        glBufferData(GL_ARRAY_BUFFER,
                     vertex_positions,
                     GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, MemoryUtil.NULL);
        glEnableVertexAttribArray(0);

        index_buffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_buffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER,
                     vertex_indices,
                     GL_STATIC_DRAW);

        uniform_buffer = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, uniform_buffer);
        glBufferData(GL_UNIFORM_BUFFER, 4 * Matrix4x4f.sizeof(), GL_DYNAMIC_DRAW);

        glEnable(GL_CULL_FACE);
        // glFrontFace(GL_CW);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        int i;

        glDisable(GL_SCISSOR_TEST);

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        // Turn on scissor testing
        glEnable(GL_SCISSOR_TEST);

        // Each rectangle will be 7/16 of the screen
        int scissor_width = (7 * info.windowWidth) / 16;
        int scissor_height = (7 * info.windowHeight) / 16;

        // Four rectangles - lower left first...
        glScissorIndexed(0,
                         0, 0,
                         scissor_width, scissor_height);

        // Lower right...
        glScissorIndexed(1,
                         info.windowWidth - scissor_width, 0,
                         scissor_width, scissor_height);

        // Upper left...
        glScissorIndexed(2,
                         0, info.windowHeight - scissor_height,
                         scissor_width, scissor_height);

        // Upper right...
        glScissorIndexed(3,
                         info.windowWidth - scissor_width,
                         info.windowHeight - scissor_height,
                         scissor_width, scissor_height);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(20.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);

        glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniform_buffer);
        ByteBuffer bb_mv_matrix_array = glMapBufferRange(GL_UNIFORM_BUFFER,
                                                                        0,
                                                                        4 * Matrix4x4f.sizeof(),
                                                                        GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        FloatBuffer mv_matrix_array = bb_mv_matrix_array.asFloatBuffer();
        for (i = 0; i < 4; i++)
        {
        	Matrix4x4f mv_matrix = new Matrix4x4f(proj_matrix)
        						.mul(Matrix4x4f.translate(0.0f, 0.0f, -2.0f))
                                .mul(Matrix4x4f.rotate((float)currentTime * 45.0f * (float)(i + 1), 0.0f, 1.0f, 0.0f))
                                .mul(Matrix4x4f.rotate((float)currentTime * 81.0f * (float)(i + 1), 1.0f, 0.0f, 0.0f));
        	mv_matrix.toFloatBuffer(mv_matrix_array);
        }

        glUnmapBuffer(GL_UNIFORM_BUFFER);

        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
    }

    protected void shutdown()
    {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glDeleteBuffers(position_buffer);
    }

	public static void main(String[] args) {
		new MultiScissor().run();
	}

}
