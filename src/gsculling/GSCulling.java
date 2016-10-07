package gsculling;

import java.io.IOException;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class GSCulling extends Application {


	private int program;
    private int mv_location;
    private int mvp_location;
    private int viewpoint_location;

    private SBMObject object = new SBMObject();
	
    public GSCulling() {
		super("OpenGL SuperBible - Geometry Shader Culling");
        init();
		// TODO Auto-generated constructor stub
	}
	
    protected void startup() throws IOException
    {
        final String vs_source =
            "#version 410 core                                                                       \n" +
            "                                                                                        \n" +
            "// Incoming per vertex... position and normal                                           \n" +
            "in vec4 vVertex;                                                                        \n" +
            "in vec3 vNormal;                                                                        \n" +
            "                                                                                        \n" +
            "out Vertex                                                                              \n" +
            "{                                                                                       \n" +
            "    vec3 normal;                                                                        \n" +
            "    vec4 color;                                                                         \n" +
            "} vertex;                                                                               \n" +
            "                                                                                        \n" +
            "uniform vec3 vLightPosition = vec3(-10.0, 40.0, 200.0);                                 \n" +
            "uniform mat4 mvMatrix;                                                                  \n" +
            "                                                                                        \n" +
            "void main(void)                                                                         \n" +
            "{                                                                                       \n" +
            "    // Get surface normal in eye coordinates                                            \n" +
            "    vec3 vEyeNormal = mat3(mvMatrix) * normalize(vNormal);                              \n" +
            "                                                                                        \n" +
            "    // Get vertex position in eye coordinates                                           \n" +
            "    vec4 vPosition4 = mvMatrix * vVertex;                                               \n" +
            "    vec3 vPosition3 = vPosition4.xyz / vPosition4.w;                                    \n" +
            "                                                                                        \n" +
            "    // Get vector to light source                                                       \n" +
            "    vec3 vLightDir = normalize(vLightPosition - vPosition3);                            \n" +
            "                                                                                        \n" +
            "    // Dot product gives us diffuse intensity                                           \n" +
            "    vertex.color = vec4(0.7, 0.6, 1.0, 1.0) * abs(dot(vEyeNormal, vLightDir));          \n" +
            "                                                                                        \n" +
            "    gl_Position = vVertex;                                                              \n" +
            "    vertex.normal = vNormal;                                                            \n" +
            "}                                                                                       \n";

        final String gs_source =
            "#version 410 core                                                       \n" +
            "                                                                        \n" +
            "layout (triangles) in;                                                  \n" +
            "layout (triangle_strip, max_vertices = 3) out;                          \n" +
            "                                                                        \n" +
            "in Vertex                                                               \n" +
            "{                                                                       \n" +
            "    vec3 normal;                                                        \n" +
            "    vec4 color;                                                         \n" +
            "} vertex[];                                                             \n" +
            "                                                                        \n" +
            "out vec4 color;                                                         \n" +
            "                                                                        \n" +
            "uniform vec3 vLightPosition;                                            \n" +
            "uniform mat4 mvpMatrix;                                                 \n" +
            "uniform mat4 mvMatrix;                                                  \n" +
            "                                                                        \n" +
            "uniform vec3 viewpoint;                                                 \n" +
            "                                                                        \n" +
            "void main(void)                                                         \n" +
            "{                                                                       \n" +
            "    int n;                                                              \n" +
            "                                                                        \n" +
            "    vec3 ab = gl_in[1].gl_Position.xyz - gl_in[0].gl_Position.xyz;      \n" +
            "    vec3 ac = gl_in[2].gl_Position.xyz - gl_in[0].gl_Position.xyz;      \n" +
            "    vec3 normal = normalize(cross(ab, ac));                             \n" +
            "    vec3 transformed_normal = (mat3(mvMatrix) * normal);                \n" +
            "    vec4 worldspace = /* mvMatrix * */ gl_in[0].gl_Position;            \n" +
            "    vec3 vt = normalize(viewpoint - worldspace.xyz);                    \n" +
            "                                                                        \n" +
            "    if (dot(normal, vt) > 0.0) {                                        \n" +
            "        for (n = 0; n < 3; n++) {                                       \n" +
            "            gl_Position = mvpMatrix * gl_in[n].gl_Position;             \n" +
            "            color = vertex[n].color;                                    \n" +
            "            EmitVertex();                                               \n" +
            "        }                                                               \n" +
            "        EndPrimitive();                                                 \n" +
            "    }                                                                   \n" +
            "}                                                                       \n";

        final String fs_source =
            "// GS culling example                                                   \n" +
            "// Fragment Shader                                                      \n" +
            "// Graham Sellers                                                       \n" +
            "// OpenGL SuperBible                                                    \n" +
            "#version 410 core                                                       \n" +
            "                                                                        \n" +
            "in vec4 color;                                                          \n" +
            "                                                                        \n" +
            "out vec4 output_color;                                                  \n" +
            "                                                                        \n" +
            "void main(void)                                                         \n" +
            "{                                                                       \n" +
            "    output_color = color;                                               \n" +
            "}                                                                       \n";


        
        

        int vs = Shader.compile(GL20.GL_VERTEX_SHADER, vs_source);
        int gs = Shader.compile(GL32.GL_GEOMETRY_SHADER, gs_source);
        int fs = Shader.compile(GL20.GL_FRAGMENT_SHADER, fs_source);
        program = Program.link(true, vs, gs, fs);

        mv_location = glGetUniformLocation(program, "mvMatrix");
        mvp_location = glGetUniformLocation(program, "mvpMatrix");
        viewpoint_location = glGetUniformLocation(program, "viewpoint");

        object.load(getMediaPath() + "/objects/bunny_1k.sbm");

        glDisable(GL_CULL_FACE);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        float f = (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        // black background
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        // depth 1
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -1.5f)/* 
        						.mul(Matrix4x4f.rotate((float)currentTime * 5.0f, 0.0f, 1.0f, 0.0f)) 
                                .mul(Matrix4x4f.rotate((float)currentTime * 100.0f, 1.0f, 0.0f, 0.0f))*/;

        glUniformMatrix4(mvp_location, false, proj_matrix.mul(mv_matrix).toFloatBuffer());

        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());

        float vViewpoint[] = { 	(float)Math.sin(f * 2.1f) * 70.0f, 
        						(float)Math.cos(f * 1.4f) * 70.0f, 
        						(float)Math.sin(f * 0.7f) * 70.0f };
        glUniform3f(viewpoint_location, vViewpoint[0], vViewpoint[1], vViewpoint[2]);

        object.render();
    }

    protected void shutdown()
    {
        object.free();
        glDeleteProgram(program);
    }

    public static void main(String[] args) {
    	GSCulling gsculling = new GSCulling();
    	gsculling.run();
    }

}
