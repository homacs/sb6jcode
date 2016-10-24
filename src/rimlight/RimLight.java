package rimlight;



import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class RimLight extends Application {
    private int          program = 0;
    class Uniforms
    {
        int           mv_matrix;
        int           proj_matrix;
        int           rim_color;
        int           rim_power;
    } 
    Uniforms uniforms = new Uniforms();


    SBMObject         object = new SBMObject();
    private volatile boolean          paused = false;
    private volatile Vector3f         rim_color = new Vector3f(0.3f, 0.3f, 0.3f);
    private volatile float            rim_power = 2.5f;
    private volatile boolean          rim_enable;

    private double last_time = 0.0;
    private double total_time = 0.0;

	public RimLight() {
		super("OpenGL SuperBible - Rim Lighting");
	}

    protected void startup() throws IOException
    {
        loadShaders();

        object.load(getMediaPath() + "/objects/dragon.sbm");

        glEnable(GL_CULL_FACE);
        //glCullFace(GL_FRONT);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

        if (!paused)
            total_time += (currentTime - last_time);
        last_time = currentTime;

        float f = (float)total_time;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        glUseProgram(program);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());

        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, -5.0f, -20.0f)
                                .mul(Matrix4x4f.rotate(f * 5.0f, 0.0f, 1.0f, 0.0f))
                                .mul(Matrix4x4f.identity());
        glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());

        glUniform3(uniforms.rim_color, (rim_enable ? rim_color : new Vector3f(0.0f)).toFloatBuffer());
        glUniform1f(uniforms.rim_power, rim_power);

        object.render();
    }

    protected void shutdown()
    {
        object.free();
        glDeleteProgram(program);
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action == GLFW.GLFW_PRESS)
        {
            switch (key)
            {
                case 'Q':
                    rim_color.set(0, rim_color.get(0)+ 0.1f);
                    break;
                case 'W':
                    rim_color.set(1, rim_color.get(1)+ 0.1f);
                    break;
                case 'E':
                    rim_color.set(2, rim_color.get(2)+ 0.1f);
                    break;
                case 'R':
                    rim_power *= 1.5f;
                    break;
                case 'A':
                    rim_color.set(0, rim_color.get(0) - 0.1f);
                    break;
                case 'S':
                    rim_color.set(1, rim_color.get(1) - 0.1f);
                    break;
                case 'D':
                    rim_color.set(2, rim_color.get(2) - 0.1f);
                    break;
                case 'F':
                    rim_power /= 1.5f;
                    break;
                case GLFW.GLFW_KEY_Z:
                    rim_enable = !rim_enable;
                    break;
                case GLFW.GLFW_KEY_KP_MULTIPLY:
                	rim_color.mul(1.1f);
                	break;
                case GLFW.GLFW_KEY_KP_DIVIDE:
                	rim_color.mul(1/1.1f);
                	break;
                case 'P':
                    paused = !paused;
                    break;
                case 'L':
                    loadShaders();
                    break;
            }
        }
    }

    void loadShaders() throws IOException
    {
        int vs, fs;

        vs = Shader.load(getMediaPath() + "/shaders/rimlight/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/rimlight/render.fs.glsl", GL_FRAGMENT_SHADER);

        if (program != 0)
            glDeleteProgram(program);

        program = Program.link(true, vs, fs);

        uniforms.mv_matrix = glGetUniformLocation(program, "mv_matrix");
        uniforms.proj_matrix = glGetUniformLocation(program, "proj_matrix");
        uniforms.rim_color = glGetUniformLocation(program, "rim_color");
        uniforms.rim_power = glGetUniformLocation(program, "rim_power");
    }

	public static void main(String[] args) {
		new RimLight().run();
	}

}
