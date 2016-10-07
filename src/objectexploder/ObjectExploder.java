package objectexploder;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

import java.io.IOException;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;

public class ObjectExploder extends Application {
    private int           program;
    private int           mv_location;
    private int           proj_location;
    private int           explode_factor_location;

    SBMObject     object = new SBMObject();

	public ObjectExploder() {
		super("OpenGL SuperBible - Exploder");
	}
	

    protected void startup() throws IOException
    {
        final String vs_source =
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
            "uniform mat4 mv_matrix;                                            \n" +
            "uniform mat4 proj_matrix;                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = proj_matrix * mv_matrix * position;              \n" +
            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n" +
            "    vs_out.normal = normalize(mat3(mv_matrix) * normal);           \n" +
            "}                                                                  \n";

        final String gs_source =
            "#version 410 core                                                      \n" +
            "                                                                       \n" +
            "layout (triangles) in;                                                 \n" +
            "layout (triangle_strip, max_vertices = 3) out;                         \n" +
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
            "uniform float explode_factor = 0.2;                                    \n" +
            "                                                                       \n" +
            "void main(void)                                                        \n" +
            "{                                                                      \n" +
            "    vec3 ab = gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz;     \n" +
            "    vec3 ac = gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz;     \n" +
            "    vec3 face_normal = -normalize(cross(ab, ac));                       \n" +
            "    for (int i = 0; i < gl_in.length(); i++)                           \n" +
            "    {                                                                  \n" +
            "        gl_Position = gl_in[i].gl_Position + vec4(face_normal * explode_factor, 0.0);    \n" +
            "        gs_out.normal = gs_in[i].normal;                               \n" +
            "        gs_out.color = gs_in[i].color;                                 \n" +
            "        EmitVertex();                                                  \n" +
            "    }                                                                  \n" +
            "    EndPrimitive();                                                    \n" +
            "}                                                                      \n";

        final String fs_source =
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
            "    color = vec4(1.0) * abs(normalize(fs_in.normal).z);               \n" +
            "}                                                                  \n";

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);

        int gs = Shader.compile(GL_GEOMETRY_SHADER, gs_source);

        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);

        program = Program.link(true, vs, gs, fs);


        mv_location = glGetUniformLocation(program, "mv_matrix");
        proj_location = glGetUniformLocation(program, "proj_matrix");
        explode_factor_location = glGetUniformLocation(program, "explode_factor");

        object.load(this.getMediaPath() + "/objects/bunny_1k.sbm");

        // glEnable(GL_CULL_FACE);
        //glCullFace(GL_FRONT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
    	float speed = 0.05f;
    	
    	currentTime = currentTime * speed;
    	
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -3.0f) 
                                .mul(Matrix4x4f.rotate((float)currentTime * 45.0f, 0.0f, 1.0f, 0.0f)) 
                                .mul(Matrix4x4f.rotate((float)currentTime * 81.0f, 1.0f, 0.0f, 0.0f));
        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());

        glUniform1f(explode_factor_location, (float)Math.sin((float)currentTime * 8.0f) * (float)Math.cos((float)currentTime * 6.0f) * 0.7f + 0.1f);

        object.render();
    }

    protected void shutdown()
    {
        object.free();
        glDeleteProgram(program);
    }

	public static void main(String[] args) {
		ObjectExploder app = new ObjectExploder();
		app.run();
	}

}
