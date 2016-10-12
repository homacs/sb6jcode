package shadowmapping;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;



import java.nio.IntBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.*;
import static sb6.vmath.MathHelper.*;

public class ShadowMapping extends Application {


	private static final int DEPTH_TEXTURE_SIZE = 4096;

	
    private int          light_program = 0;
    private int          view_program = 0;
    private int           show_light_depth_program = 0;

    class Uniforms
    {
        class Light
        {
            int   mvp;
        } 
        class View
        {
            int   mv_matrix;
            int   proj_matrix;
            int   shadow_matrix;
            int   full_shading;
        }
        Light light = new Light();
        View view = new View();
    } 
    private Uniforms uniforms = new Uniforms();

    private int          depth_fbo;
    private int          depth_tex;
    private int          depth_debug_tex;

    class ObjectData
    {
        SBMObject      obj = new SBMObject();
        Matrix4x4f     model_matrix = new Matrix4x4f();
    }
    private ObjectData[] objects = new ObjectData[]{new ObjectData(),new ObjectData(),new ObjectData(),new ObjectData()};

    private Matrix4x4f     light_view_matrix = new Matrix4x4f();
    private Matrix4x4f     light_proj_matrix = new Matrix4x4f();

    private Matrix4x4f     camera_view_matrix = new Matrix4x4f();
    private Matrix4x4f     camera_proj_matrix = new Matrix4x4f();

    private int          quad_vao;

    // mode
    private static final int RENDER_FULL = 0;
    private static final int RENDER_LIGHT = 1;
    private static final int RENDER_DEPTH = 2;
    private volatile int mode = RENDER_FULL;
    
    private volatile boolean paused = false;
    
    private double last_time = 0.0;
    private double total_time = 0.0;

    private static final Matrix4x4f scale_bias_matrix = new Matrix4x4f(
    		new float[]{0.5f, 0.0f, 0.0f, 0.0f},
    		new float[]{0.0f, 0.5f, 0.0f, 0.0f},
    		new float[]{0.0f, 0.0f, 0.5f, 0.0f},
    		new float[]{0.5f, 0.5f, 0.5f, 1.0f});

    
	public ShadowMapping() {
		super("OpenGL SuperBible - Shadow Mapping");
	}

	protected void startup() throws IOException
	{
	    load_shaders();

	    int i;

	    String object_names[] =
	    {
	        getMediaPath() + "/objects/dragon.sbm",
	        getMediaPath() + "/objects/sphere.sbm",
	        getMediaPath() + "/objects/cube.sbm",
	        getMediaPath() + "/objects/torus.sbm"
	    };

	    for (i = 0; i < objects.length; i++)
	    {
	        objects[i].obj.load(object_names[i]);
	    }

	    depth_fbo = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, depth_fbo);

	    depth_tex = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, depth_tex);
	    glTexStorage2D(GL_TEXTURE_2D, 11, GL_DEPTH_COMPONENT32F, DEPTH_TEXTURE_SIZE, DEPTH_TEXTURE_SIZE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

	    glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depth_tex, 0);

	    depth_debug_tex = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, depth_debug_tex);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, DEPTH_TEXTURE_SIZE, DEPTH_TEXTURE_SIZE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

	    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, depth_debug_tex, 0);

	    glBindTexture(GL_TEXTURE_2D, 0);
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);

	    glEnable(GL_DEPTH_TEST);

	    quad_vao = glGenVertexArrays();
	    glBindVertexArray(quad_vao);
	}

	protected void render(double currentTime)
	{
	    

	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;

	    float f = (float)total_time + 30.0f;

	    Vector3f light_position = new Vector3f(20.0f, 20.0f, 20.0f);
	    Vector3f view_position = new Vector3f(0.0f, 0.0f, 40.0f);

	    light_proj_matrix = Matrix4x4f.frustum(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 200.0f);
	    light_view_matrix = Matrix4x4f.lookat(light_position,
	                                      new Vector3f(0.0f), new Vector3f(0.0f, 1.0f, 0.0f));

	    camera_proj_matrix = Matrix4x4f.perspective(50.0f,
	                                            (float)info.windowWidth / (float)info.windowHeight,
	                                            1.0f,
	                                            200.0f);

	    camera_view_matrix = Matrix4x4f.lookat(view_position,
	    		new Vector3f(0.0f),
	    		new Vector3f(0.0f, 1.0f, 0.0f));

	    objects[0].model_matrix = Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f)
	    		.mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	    		.mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));

	    objects[1].model_matrix = Matrix4x4f.rotate(f * 3.7f, 0.0f, 1.0f, 0.0f)
	    		.mul(Matrix4x4f.translate(sinf(f * 0.37f) * 12.0f, cosf(f * 0.37f) * 12.0f, 0.0f))
	    		.mul(Matrix4x4f.scale(2.0f));

	    objects[2].model_matrix = Matrix4x4f.rotate(f * 6.45f, 0.0f, 1.0f, 0.0f)
	    		.mul(Matrix4x4f.translate(sinf(f * 0.25f) * 10.0f, cosf(f * 0.25f) * 10.0f, 0.0f))
	    		.mul(Matrix4x4f.rotate(f * 99.0f, 0.0f, 0.0f, 1.0f))
	    		.mul(Matrix4x4f.scale(2.0f));

	    objects[3].model_matrix = Matrix4x4f.rotate(f * 5.25f, 0.0f, 1.0f, 0.0f)
	    		.mul(Matrix4x4f.translate(sinf(f * 0.51f) * 14.0f, cosf(f * 0.51f) * 14.0f, 0.0f))
	    		.mul(Matrix4x4f.rotate(f * 120.3f, 0.707106f, 0.0f, 0.707106f))
	    		.mul(Matrix4x4f.scale(2.0f));

	    glEnable(GL_DEPTH_TEST);
	    render_scene(total_time, true);

	    if (mode == RENDER_DEPTH)
	    {
	        glDisable(GL_DEPTH_TEST);
	        glBindVertexArray(quad_vao);
	        glUseProgram(show_light_depth_program);
	        glBindTexture(GL_TEXTURE_2D, depth_debug_tex);
	        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	    }
	    else
	    {
	        render_scene(total_time, false);
	    }
	}

	private void render_scene(double currentTime, boolean from_light)
	{

	    Matrix4x4f light_vp_matrix = Matrix4x4f.multiply(light_proj_matrix, light_view_matrix);
	    Matrix4x4f shadow_sbpv_matrix = Matrix4x4f.multiply(scale_bias_matrix, light_proj_matrix).mul(light_view_matrix);

	    if (from_light)
	    {
	        glBindFramebuffer(GL_FRAMEBUFFER, depth_fbo);
	        glViewport(0, 0, DEPTH_TEXTURE_SIZE, DEPTH_TEXTURE_SIZE);
	        glEnable(GL_POLYGON_OFFSET_FILL);
	        glPolygonOffset(4.0f, 4.0f);
	        glUseProgram(light_program);
	        IntBuffer buffs = BufferUtilsHelper.createIntBuffer(new int[]{ GL_COLOR_ATTACHMENT0 });
	        glDrawBuffers(buffs);
	        glClearBuffer1f(GL_COLOR, 0, 0f);
	    }
	    else
	    {
	        glViewport(0, 0, info.windowWidth, info.windowHeight);
	        glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	        glUseProgram(view_program);
	        glActiveTexture(GL_TEXTURE0);
	        glBindTexture(GL_TEXTURE_2D, depth_tex);
	        glUniformMatrix4(uniforms.view.proj_matrix, false, camera_proj_matrix.toFloatBuffer());
	        glDrawBuffer(GL_BACK);
	    }

	    glClearBuffer1f(GL_DEPTH, 0, 1.0f);

	    int i;
	    for (i = 0; i < 4; i++)
	    {
	        Matrix4x4f model_matrix = objects[i].model_matrix;
	        if (from_light)
	        {
	            glUniformMatrix4(uniforms.light.mvp, false, Matrix4x4f.multiply(light_vp_matrix, objects[i].model_matrix).toFloatBuffer());
	        }
	        else
	        {
	            Matrix4x4f shadow_matrix = Matrix4x4f.multiply(shadow_sbpv_matrix, model_matrix);
	            glUniformMatrix4(uniforms.view.shadow_matrix, false, shadow_matrix.toFloatBuffer());
	            glUniformMatrix4(uniforms.view.mv_matrix, false, Matrix4x4f.multiply(camera_view_matrix, objects[i].model_matrix).toFloatBuffer());
	            glUniform1i(uniforms.view.full_shading, mode == RENDER_FULL ? 1 : 0);
	        }
	        objects[i].obj.render();
	    }

	    if (from_light)
	    {
	        glDisable(GL_POLYGON_OFFSET_FILL);
	        glBindFramebuffer(GL_FRAMEBUFFER, 0);
	    }
	    else
	    {
	        glBindTexture(GL_TEXTURE_2D, 0);
	    }
	}

	protected void onKey(int key, int action) throws IOException
	{
	    if (action == GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case '1':
	                mode = RENDER_FULL;
	                break;
	            case '2':
	                mode = RENDER_LIGHT;
	                break;
	            case '3':
	                mode = RENDER_DEPTH;
	                break;
	            case 'R': 
	                load_shaders();
	                break;
	            case 'P':
	                paused = !paused;
	                break;
	        }
	    }
	}

	void load_shaders() throws IOException
	{
	    int vs;
	    int fs;

	    vs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-light.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-light.fs.glsl", GL_FRAGMENT_SHADER);

	    if (light_program != 0)
	        glDeleteProgram(light_program);

	    light_program = glCreateProgram();
	    glAttachShader(light_program, vs);
	    glAttachShader(light_program, fs);
	    glLinkProgram(light_program);

	    glDeleteShader(vs);
	    glDeleteShader(fs);

	    uniforms.light.mvp = glGetUniformLocation(light_program, "mvp");

	    vs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-camera.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-camera.fs.glsl", GL_FRAGMENT_SHADER);

	    if (light_program != 0)
	        glDeleteProgram(view_program);

	    view_program = glCreateProgram();
	    glAttachShader(view_program, vs);
	    glAttachShader(view_program, fs);
	    glLinkProgram(view_program);

	    glDeleteShader(vs);
	    glDeleteShader(fs);

	    uniforms.view.proj_matrix = glGetUniformLocation(view_program, "proj_matrix");
	    uniforms.view.mv_matrix = glGetUniformLocation(view_program, "mv_matrix");
	    uniforms.view.shadow_matrix = glGetUniformLocation(view_program, "shadow_matrix");
	    uniforms.view.full_shading = glGetUniformLocation(view_program, "full_shading");

	    if (show_light_depth_program != 0)
	        glDeleteProgram(show_light_depth_program);

	    show_light_depth_program = glCreateProgram();

	    vs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-light-view.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/shadowmapping/shadowmapping-light-view.fs.glsl", GL_FRAGMENT_SHADER);

	    glAttachShader(show_light_depth_program, vs);
	    glAttachShader(show_light_depth_program, fs);
	    glLinkProgram(show_light_depth_program);

	    glDeleteShader(vs);
	    glDeleteShader(fs);
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
			new ShadowMapping().run();
	}

}
