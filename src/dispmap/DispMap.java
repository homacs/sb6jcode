package dispmap;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT;
import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL13.GL_TEXTURE1;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL40.GL_PATCHES;
import static org.lwjgl.opengl.GL40.GL_PATCH_VERTICES;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL40.glPatchParameteri;
import static sb6.vmath.MathHelper.cosf;
import static sb6.vmath.MathHelper.sinf;

import java.io.IOException;

import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;

public class DispMap extends Application {
    int          program;
    int          vao;
    int          tex_displacement;
    int          tex_color;
    float           dmap_depth;
    boolean            enable_displacement;
    boolean            wireframe;
    boolean            enable_fog;
    boolean            paused;

    class Uniforms
    {
        int       mvp_matrix;
        int       mv_matrix;
        int       proj_matrix;
        int       dmap_depth;
        int       enable_fog;
    }
    Uniforms uniforms = new Uniforms();

	
	public DispMap() {
		super("OpenGL SuperBible - Displacement Mapping");
		program = 0;
        enable_displacement = true;
        wireframe = false;
        enable_fog = true;
        paused = false;
        
        init();
	}

	@Override
	protected void startup() throws Throwable {
        load_shaders();

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        glPatchParameteri(GL_PATCH_VERTICES, 4);

        glEnable(GL_CULL_FACE);

        tex_displacement = KTX.load(getMediaPath() + "/textures/terragen1.ktx");
        glActiveTexture(GL_TEXTURE1);
        tex_color = KTX.load(getMediaPath() + "/textures/terragen_color.ktx");
	}

    double last_time = 0.0;
    double total_time = 0.0;
    
	@Override
	protected void render(double currentTime) throws Throwable {
        final float one = 1.0f;

        if (!paused)
            total_time += (currentTime - last_time);
        last_time = currentTime;

        float t = (float)total_time * 0.03f;
        float r = sinf(t * 5.37f) * 15.0f + 16.0f;
        float h = cosf(t * 4.79f) * 2.0f + 3.2f;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        GLAPIHelper.glClearBuffer4f(GL_COLOR, 0,  0.85f, 0.95f, 1.0f, 1.0f); // sky blue
        glClearBuffer1f(GL_DEPTH, 0, one);

        Matrix4x4f mv_matrix = /* vmath::translate(0.0f, 0.0f, -1.4f) *
                                vmath::translate(0.0f, -0.4f, 0.0f) * */
                                // vmath::rotate((float)currentTime * 6.0f, 0.0f, 1.0f, 0.0f) *
                                Matrix4x4f.lookat(new Vector3f(sinf(t) * r, h, cosf(t) * r), new Vector3f(0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f, 1000.0f);

        glUseProgram(program);

        glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.mvp_matrix, false, proj_matrix.mul(mv_matrix).toFloatBuffer());
        glUniform1f(uniforms.dmap_depth, enable_displacement ? dmap_depth : 0.0f);
        glUniform1i(uniforms.enable_fog, enable_fog ? 1 : 0);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        if (wireframe)
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        else
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glDrawArraysInstanced(GL_PATCHES, 0, 4, 64 * 64);
	}

	@Override
	protected void shutdown() throws Throwable {
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
	}

    protected void onKey(int key, int action) throws IOException
    {
        if (action == 1)
        {
            switch (key)
            {
                case GLFW_KEY_KP_ADD: dmap_depth += 0.1f;
                    break;
                case GLFW_KEY_KP_SUBTRACT: dmap_depth -= 0.1f;
                    break;
                case 'F': enable_fog = !enable_fog;
                    break;
                case 'D': enable_displacement = !enable_displacement;
                    break;
                case 'W': wireframe = !wireframe;
                    break;
                case 'P': paused = !paused;
                    break;
                case 'R':
                        load_shaders();
                    break;
                default:
                    break;
            };
        }
    }

	private void load_shaders() throws IOException {
	    if (program != 0)
	        glDeleteProgram(program);

	    int vs = Shader.load(getMediaPath() + "/shaders/dispmap/dispmap.vs.glsl", GL_VERTEX_SHADER);
	    int tcs = Shader.load(getMediaPath() + "/shaders/dispmap/dispmap.tcs.glsl", GL_TESS_CONTROL_SHADER);
	    int tes = Shader.load(getMediaPath() + "/shaders/dispmap/dispmap.tes.glsl", GL_TESS_EVALUATION_SHADER);
	    int fs = Shader.load(getMediaPath() + "/shaders/dispmap/dispmap.fs.glsl", GL_FRAGMENT_SHADER);

	    program = glCreateProgram();

	    glAttachShader(program, vs);
	    glAttachShader(program, tcs);
	    glAttachShader(program, tes);
	    glAttachShader(program, fs);

	    glLinkProgram(program);

	    uniforms.mv_matrix = glGetUniformLocation(program, "mv_matrix");
	    uniforms.mvp_matrix = glGetUniformLocation(program, "mvp_matrix");
	    uniforms.proj_matrix = glGetUniformLocation(program, "proj_matrix");
	    uniforms.dmap_depth = glGetUniformLocation(program, "dmap_depth");
	    uniforms.enable_fog = glGetUniformLocation(program, "enable_fog");
	    dmap_depth = 6.0f;
	}
	
	public static void main(String[] args) {
		new DispMap().run();
	}
}
