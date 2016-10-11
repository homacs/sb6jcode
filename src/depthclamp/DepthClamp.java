package depthclamp;

import java.io.IOException;

import org.lwjgl.glfw.GLFW;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.*;

public class DepthClamp extends Application {
    private int          program;
    private int           mv_location;
    private int           proj_location;
	private volatile boolean depthclamp;
	private volatile boolean pause;

    SBMObject     object = new SBMObject();
	private Matrix4x4f mv_matrix;

	public DepthClamp() {
		super("OpenGL SuperBible - Depth Clamping");
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
            "uniform mat4 mv_matrix;                                            \n" +
            "uniform mat4 proj_matrix;                                          \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    gl_Position = proj_matrix * mv_matrix * position;              \n" +
            "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n" +
            "    vs_out.normal = normalize(mat3(mv_matrix) * normal);           \n" +
            "}                                                                  \n"
        ;

        String fs_source =
            "#version 410 core                                                  \n" +
            "                                                                   \n" +
            "out vec4 color;                                                    \n" +
            "                                                                   \n" +
            "in VS_OUT                                                          \n" +
            "{                                                                  \n" +
            "    vec3 normal;                                                   \n" +
            "    vec4 color;                                                    \n" +
            "} fs_in;                                                           \n" +
            "                                                                   \n" +
            "void main(void)                                                    \n" +
            "{                                                                  \n" +
            "    color = vec4(1.0) * abs(normalize(fs_in.normal).z);               \n" +
            "}                                                                  \n"
        ;

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        program = Program.link(true, vs, fs);

        mv_location = glGetUniformLocation(program, "mv_matrix");
        proj_location = glGetUniformLocation(program, "proj_matrix");

        object.load(getMediaPath() + "/objects/bunny_1k.sbm");

        
        mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -2.0f)
                .mul(Matrix4x4f.rotate(45.0f, 0.0f, 1.0f, 0.0f))
                .mul(Matrix4x4f.rotate(81.0f, 1.0f, 0.0f, 0.0f));

        
        glEnable(GL_CULL_FACE);
        //glCullFace(GL_FRONT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        float f = 0.9f * (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(20.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     1.8f,
                                                     1000.0f);
        glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

        if (depthclamp) {
        	glEnable(GL_DEPTH_CLAMP);
        } else {
        	glDisable(GL_DEPTH_CLAMP);
        }
        if (!pause) {
	        mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -2.0f)
	                                .mul(Matrix4x4f.rotate(f * 45.0f, 0.0f, 1.0f, 0.0f))
	                                .mul(Matrix4x4f.rotate(f * 81.0f, 1.0f, 0.0f, 0.0f));
        }
        glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());

        object.render();
    }

    protected void shutdown()
    {
        object.free();
        glDeleteProgram(program);
    }

    
    
	@Override
	protected void onKey(int key, int action) throws Throwable {
		if (action == GLFW.GLFW_RELEASE) return;
		
		switch (key) {
		case GLFW.GLFW_KEY_C:
			depthclamp = !depthclamp;
			break;
		case GLFW.GLFW_KEY_P:
			pause = !pause;
			break;
		}
	}


	public static void main (String[] args) {
		new DepthClamp().run();
	}

}
