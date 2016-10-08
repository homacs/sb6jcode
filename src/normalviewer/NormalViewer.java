package normalviewer;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

import java.io.IOException;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;

public class NormalViewer extends Application {
    private int          program;
    private int           mv_location;
    private int           proj_location;
    private int           normal_length_location;

    SBMObject     object = new SBMObject();

	public NormalViewer() {
		super("OpenGL SuperBible - Normal Viewer");
	}

	protected void startup() throws IOException
    {
        String vs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "layout (location = 0) in vec4 position;                            \n" +
            "layout (location = 1) in vec3 normal;                              \n" +
            "                                                                   \n" +
            "out VS_OUT                                                         \n" +
            "{                                                                  \n" +
            "    vec3 normal;                                                   \n" +
            "    vec4 color;                                                    \n" +
            "} vs_out;                                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = position;                                        \n" +
            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n" +
            "    vs_out.normal = normalize(normal);                             \n" +
            "}                                                                  \n"
        ;

        String gs_source =
            "#version 410 core                                                      \n" +
            "                                                                       \n" +
            "layout (triangles) in;                                                 \n" +
            "layout (line_strip, max_vertices = 4) out;                             \n" +
            "                                                                       \n" +
            "uniform mat4 mv_matrix;                                                \n" +
            "uniform mat4 proj_matrix;                                              \n" +
            "                                                                       \n" +
            "in VS_OUT                                                              \n" +
            "{                                                                      \n" +
            "    vec3 normal;                                                       \n" +
            "    vec4 color;                                                        \n" +
            "} gs_in[];                                                             \n" +
            "                                                                       \n" +
            "out GS_OUT                                                             \n" +
            "{                                                                      \n" +
            "    vec3 normal;                                                       \n" +
            "    vec4 color;                                                        \n" +
            "} gs_out;                                                              \n" +
            "                                                                       \n" +
            "uniform float normal_length = 0.2;                                     \n" +
            "                                                                       \n" +
            "void main(void)                                                        \n" +
            "{                                                                      \n" +
            "    mat4 mvp = proj_matrix * mv_matrix;                                \n" +
            "    vec3 ab = gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz;     \n" +
            "    vec3 ac = gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz;     \n" +
            "    vec3 face_normal = normalize(cross(ab, ac));                      \n" +
            "                                                                       \n" +
            "    vec4 tri_centroid = (gl_in[0].gl_Position +                        \n" +
            "                         gl_in[1].gl_Position +                        \n" +
            "                         gl_in[2].gl_Position) / 3.0;                  \n" +
            "                                                                       \n" +
            "    gl_Position = mvp * tri_centroid;                                  \n" +
            "    gs_out.normal = gs_in[0].normal;                                   \n" +
            "    gs_out.color = gs_in[0].color;                                     \n" +
            "    EmitVertex();                                                      \n" +
            "                                                                       \n" +
            "    gl_Position = mvp * (tri_centroid +                                \n" +
            "                         vec4(face_normal * normal_length, 0.0));      \n" +
            "    gs_out.normal = gs_in[0].normal;                                   \n" +
            "    gs_out.color = gs_in[0].color;                                     \n" +
            "    EmitVertex();                                                      \n" +
            "    EndPrimitive();                                                    \n" +
            "                                                                       \n" +
            "    gl_Position = mvp * gl_in[0].gl_Position;                          \n" +
            "    gs_out.normal = gs_in[0].normal;                                   \n" +
            "    gs_out.color = gs_in[0].color;                                     \n" +
            "    EmitVertex();                                                      \n" +
            "                                                                       \n" +
            "    gl_Position = mvp * (gl_in[0].gl_Position +                        \n" +
            "                         vec4(gs_in[0].normal * normal_length, 0.0));  \n" +
            "    gs_out.normal = gs_in[0].normal;                                   \n" +
            "    gs_out.color = gs_in[0].color;                                     \n" +
            "    EmitVertex();                                                      \n" +
            "    EndPrimitive();                                                    \n" +
            "}                                                                      \n"
        ;

        String fs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "out vec4 color;                                                    \n" +
            "                                                                   \n" +
            "in GS_OUT                                                          \n" +
            "{                                                                  \n" +
            "    vec3 normal;                                                   \n" +
            "    vec4 color;                                                    \n" +
            "} fs_in;                                                           \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color = vec4(1.0) * abs(normalize(fs_in.normal).z);            \n" +
            "}                                                                  \n"
        ;

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int gs = Shader.compile(GL_GEOMETRY_SHADER, gs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        
        program = Program.link(true, vs, gs, fs);

        mv_location = glGetUniformLocation(program, "mv_matrix");
        proj_location = glGetUniformLocation(program, "proj_matrix");
        normal_length_location = glGetUniformLocation(program, "normal_length");

        object.load(getMediaPath() + "/objects/bunny_1k.sbm");

        // glEnable(GL_CULL_FACE);
        //glCullFace(GL_FRONT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -2.0f)
        		.mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f))
        		.mul(Matrix4x4f.rotate((float)currentTime * 81.0f, 1.0f, 0.0f, 0.0f));
        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());

        glUniform1f(normal_length_location, 0.01f); // sinf((float)currentTime * 8.0f) * cosf((float)currentTime * 6.0f) * 0.3f + 0.5f);

        object.render();
    }

    protected void shutdown()
    {
        object.free();
        glDeleteProgram(program);
    }

	public static void main (String[] args) {
		new NormalViewer().run();
	}
}
