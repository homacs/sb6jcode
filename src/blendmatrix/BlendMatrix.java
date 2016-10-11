package blendmatrix;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.application.Application;

import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;

public class BlendMatrix extends Application {
    private int          program;
    private int          vao;
    private int          position_buffer;
    private int          index_buffer;
    private int           mv_location;
    private int           proj_location;

	public BlendMatrix() {
		super("OpenGL SuperBible - Blending Functions");
	}

    protected void startup()
    {
        String vs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "in vec4 position;                                                  \n" +
            "                                                                   \n" +
            "out VS_OUT                                                         \n" +
            "{                                                                  \n" +
            "    vec4 color0;                                                   \n" +
            "    vec4 color1;                                                   \n" +
            "} vs_out;                                                          \n" +
            "                                                                   \n" +
            "uniform mat4 mv_matrix;                                            \n" +
            "uniform mat4 proj_matrix;                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = proj_matrix * mv_matrix * position;              \n" +
            "    vs_out.color0 = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);     \n" +
            "    vs_out.color1 = vec4(0.5, 0.5, 0.5, 0.0) - position * 2.0;     \n" +
            "}                                                                  \n"
        ;

        String fs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "layout (location = 0, index = 0) out vec4 color0;                  \n" +
            "layout (location = 0, index = 1) out vec4 color1;                  \n" +
            "                                                                   \n" +
            "in VS_OUT                                                          \n" +
            "{                                                                  \n" +
            "    vec4 color0;                                                   \n" +
            "    vec4 color1;                                                   \n" +
            "} fs_in;                                                           \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color0 = vec4(fs_in.color0.xyz, 1.0);                          \n" +
            "    color1 = vec4(fs_in.color0.xyz, 1.0);                          \n" +
            "}                                                                  \n"
        ;

        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        int vs =Shader.compile(GL_VERTEX_SHADER, vs_source);
        program = Program.link(true, vs, fs);

        mv_location = glGetUniformLocation(program, "mv_matrix");
        proj_location = glGetUniformLocation(program, "proj_matrix");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        ShortBuffer vertex_indices = BufferUtilsHelper.createShortBuffer(new short[]
        {
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

        glEnable(GL_CULL_FACE);
        // glFrontFace(GL_CW);

        //glEnable(GL_DEPTH_TEST);
        //glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        int i, j;
        final FloatBuffer orange = BufferUtilsHelper.createFloatBuffer(new float[]{ 0.6f, 0.4f, 0.1f, 1.0f });
        final float one = 1.0f;

        final int[] blend_func =
        {
            GL_ZERO,
            GL_ONE,
            GL_SRC_COLOR,
            GL_ONE_MINUS_SRC_COLOR,
            GL_DST_COLOR,
            GL_ONE_MINUS_DST_COLOR,
            GL_SRC_ALPHA,
            GL_ONE_MINUS_SRC_ALPHA,
            GL_DST_ALPHA,
            GL_ONE_MINUS_DST_ALPHA,
            GL_CONSTANT_COLOR,
            GL_ONE_MINUS_CONSTANT_COLOR,
            GL_CONSTANT_ALPHA,
            GL_ONE_MINUS_CONSTANT_ALPHA,
            GL_SRC_ALPHA_SATURATE,
            GL_SRC1_COLOR,
            GL_ONE_MINUS_SRC1_COLOR,
            GL_SRC1_ALPHA,
            GL_ONE_MINUS_SRC1_ALPHA
        };
        
        final int num_blend_funcs = blend_func.length;
        final float x_scale = 20.0f / (float)num_blend_funcs;
        final float y_scale = 16.0f / (float)num_blend_funcs;
        final float t = (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer(GL_COLOR, 0, orange);
        glClearBuffer1f(GL_DEPTH, 0, one);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        glEnable(GL_BLEND);
        glBlendColor(0.2f, 0.5f, 0.7f, 0.5f);
        
        Matrix4x4f rot1 = Matrix4x4f.rotate(t * -45.0f, 0.0f, 1.0f, 0.0f);
        Matrix4x4f rot2 = Matrix4x4f.rotate(t * -21.0f, 1.0f, 0.0f, 0.0f);
         
        
        for (j = 0; j < num_blend_funcs; j++)
        {
            for (i = 0; i < num_blend_funcs; i++)
            {
            	Matrix4x4f mv_matrix = Matrix4x4f.translate(9.5f - x_scale * (float)i,
                        7.5f - y_scale * (float)j,
                        -18.0f) 
				       .mul(rot1)
				       .mul(rot2);
				glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
				
                glBlendFunc(blend_func[i], blend_func[j]);
                glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
            }
        }
    }

    protected void shutdown()
    {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glDeleteBuffers(position_buffer);
    }

	public static void main(String[] args) {
		new BlendMatrix().run();
	}

}
