package polygonsmooth;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import static sb6.vmath.MathHelper.*;

public class PolygonSmooth extends Application {
    private static final boolean MANY_CUBES = false;
	private int          program;
    private int          vao;
    private int          position_buffer;
    private int          index_buffer;
    private int           mv_location;
    private int           proj_location;

	public PolygonSmooth() {
		super("OpenGL SuperBible - Polygon Smoothing");
	}


    protected void startup()
    {
        String vs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "in vec4 position;                                                  \n" +
            "                                                                   \n" +
            "uniform mat4 mv_matrix;                                            \n" +
            "uniform mat4 proj_matrix;                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = proj_matrix * mv_matrix * position;              \n" +
            "}                                                                  \n"
        ;

        String fs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "out vec4 color;                                                    \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color = vec4(1.0)  ;                                           \n" +
            "}                                                                  \n"
        ;

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
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
    }

    protected void render(double currentTime)
    {

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        //
        // Here is the only significant difference to program LineSmooth!
        //
        glEnable(GL_POLYGON_SMOOTH);

        if (MANY_CUBES) {
	        for (int i = 0; i < 24; i++)
	        {
	            float f = (float)i + (float)currentTime * 0.3f;
	            Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -20.0f)
	                                    .mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f))
	                                    .mul(Matrix4x4f.rotate((float)currentTime * 21.0f, 1.0f, 0.0f, 0.0f))
	                                    .mul(Matrix4x4f.translate(sinf(2.1f * f) * 2.0f,
	                                                     cosf(1.7f * f) * 2.0f,
	                                                     sinf(1.3f * f) * cosf(1.5f * f) * 2.0f));
	            glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
	            glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
	        }
		} else {
	        currentTime = 3.15;
	        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f)
	                                /*.mul(Matrix4x4f.translate(sinf(2.1f * f) * 0.5f,
	                                                    cosf(1.7f * f) * 0.5f,
	                                                    sinf(1.3f * f) * cosf(1.5f * f) * 2.0f)) **/
	        		.mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f))
	                                .mul(Matrix4x4f.rotate((float)currentTime * 81.0f, 1.0f, 0.0f, 0.0f));
	        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
	        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_SHORT, 0);
		}
    }

    protected void shutdown()
    {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
        glDeleteBuffers(position_buffer);
    }

	
	public static void main(String[] args) {
		new PolygonSmooth().run();
	}

}
