package gstessellate;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;

public class GSTessellate extends Application {
    private int          program;
    private int           mv_location;
    private int           mvp_location;
    private int           stretch_location;
    private int          vao;
    private int          buffer;

	public GSTessellate() {
		super("OpenGL SuperBible - Geometry Shader Tessellation");
	}

	

    protected void startup()
    {
        String vs_source =
            "// Vertex Shader                                                                          \n" +
            "// OpenGL SuperBible                                                                      \n" +
            "#version 410 core                                                                         \n" +
            "                                                                                          \n" +
            "// Incoming per vertex... position and normal                                             \n" +
            "in vec4 vVertex;                                                                          \n" +
            "                                                                                          \n" +
            "void main(void)                                                                           \n" +
            "{                                                                                         \n" +
            "    gl_Position = vVertex;                                                                \n" +
            "}                                                                                         \n";

        String gs_source =
            "// Geometry Shader                                                            \n" +
            "// Graham Sellers                                                             \n" +
            "// OpenGL SuperBible                                                          \n" +
            "#version 410 core                                                             \n" +
            "                                                                              \n" +
            "                                                                              \n" +
            "layout (triangles) in;                                                        \n" +
            "layout (triangle_strip, max_vertices = 12) out;                                \n" +
            "                                                                              \n" +
            "uniform float stretch = 0.7;                                                  \n" +
            "                                                                              \n" +
            "flat out vec4 color;                                                          \n" +
            "                                                                              \n" +
            "uniform mat4 mvpMatrix;                                                       \n" +
            "uniform mat4 mvMatrix;                                                        \n" +
            "                                                                              \n" +
            "void make_face(vec3 a, vec3 b, vec3 c)                                        \n" +
            "{                                                                             \n" +
            "    vec3 face_normal = normalize(cross(c - a, c - b));                        \n" +
            "    vec4 face_color = vec4(1.0, 0.2, 0.4, 1.0) * (mat3(mvMatrix) * face_normal).z;  \n" +
            "    gl_Position = mvpMatrix * vec4(a, 1.0);                                   \n" +
            "    color = face_color;                                                       \n" +
            "    EmitVertex();                                                             \n" +
            "                                                                              \n" +
            "    gl_Position = mvpMatrix * vec4(b, 1.0);                                   \n" +
            "    color = face_color;                                                       \n" +
            "    EmitVertex();                                                             \n" +
            "                                                                              \n" +
            "    gl_Position = mvpMatrix * vec4(c, 1.0);                                   \n" +
            "    color = face_color;                                                       \n" +
            "    EmitVertex();                                                             \n" +
            "                                                                              \n" +
            "    EndPrimitive();                                                           \n" +
            "}                                                                             \n" +
            "                                                                              \n" +
            "void main(void)                                                               \n" +
            "{                                                                             \n" +
            "    int n;                                                                    \n" +
            "    vec3 a = gl_in[0].gl_Position.xyz;                                        \n" +
            "    vec3 b = gl_in[1].gl_Position.xyz;                                        \n" +
            "    vec3 c = gl_in[2].gl_Position.xyz;                                        \n" +
            "                                                                              \n" +
            "    vec3 d = (a + b) * stretch;                                               \n" +
            "    vec3 e = (b + c) * stretch;                                               \n" +
            "    vec3 f = (c + a) * stretch;                                               \n" +
            "                                                                              \n" +
            "    a *= (2.0 - stretch);                                                     \n" +
            "    b *= (2.0 - stretch);                                                     \n" +
            "    c *= (2.0 - stretch);                                                     \n" +

            "    make_face(a, d, f);                                                       \n" +
            "    make_face(d, b, e);                                                       \n" +
            "    make_face(e, c, f);                                                       \n" +
            "    make_face(d, e, f);                                                       \n" +

            "    EndPrimitive();                                                           \n" +
            "}                                                                             \n";

        String fs_source =
            "// Fragment Shader                                                      \n" +
            "// Graham Sellers                                                       \n" +
            "// OpenGL SuperBible                                                    \n" +
            "#version 410 core                                                       \n" +
            "                                                                        \n" +
            "flat in vec4 color;                                                     \n" +
            "                                                                        \n" +
            "out vec4 output_color;                                                  \n" +
            "                                                                        \n" +
            "void main(void)                                                         \n" +
            "{                                                                       \n" +
            "    output_color = color;                                               \n" +
            "}                                                                       \n";

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int gs = Shader.compile(GL_GEOMETRY_SHADER, gs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        
        program = Program.link(true, vs, gs, fs);

        mv_location = glGetUniformLocation(program, "mvMatrix");
        mvp_location = glGetUniformLocation(program, "mvpMatrix");
        stretch_location = glGetUniformLocation(program, "stretch");

        FloatBuffer tetrahedron_verts = BufferUtilsHelper.createFloatBuffer(new float[]
        {
             0.000f,  0.000f,  1.000f,
             0.943f,  0.000f, -0.333f,
            -0.471f,  0.816f, -0.333f,
            -0.471f, -0.816f, -0.333f
        });

        ShortBuffer tetrahedron_indices = BufferUtilsHelper.createShortBuffer(new short[]
        {
            0, 1, 2,
            0, 2, 3,
            0, 3, 1,
            3, 2, 1
        });

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        buffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, BufferUtilsHelper.sizeof(tetrahedron_verts) + BufferUtilsHelper.sizeof(tetrahedron_indices), GL_STATIC_DRAW);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, 0, tetrahedron_indices);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, BufferUtilsHelper.sizeof(tetrahedron_indices), tetrahedron_verts);

        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, BufferUtilsHelper.sizeof(tetrahedron_indices));
        glEnableVertexAttribArray(0);

        glEnable(GL_CULL_FACE);
//        glDisable(GL_CULL_FACE);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        float f = (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -10.5f)
        		.mul(Matrix4x4f.rotate((float)currentTime * 71.0f, 0.0f, 1.0f, 0.0f))
        		.mul(Matrix4x4f.rotate((float)currentTime * 10.0f, 1.0f, 0.0f, 0.0f))
        ;

        glUniformMatrix4(mvp_location, false, proj_matrix.mul(mv_matrix).toFloatBuffer());

        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());

        float stretch = MathHelper.sinf(f * 4.0f) * 0.75f + 1.0f;
        glUniform1f(stretch_location, stretch);

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 12, GL_UNSIGNED_SHORT, MemoryUtil.NULL);
    }

    protected void shutdown()
    {
        glDeleteProgram(program);
        glDeleteVertexArrays(vao);
        glDeleteBuffers(buffer);
    }

	
	
	public static void main(String[] args) {
		new GSTessellate().run();
	}

}
