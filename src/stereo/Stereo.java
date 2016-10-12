package stereo;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.FloatBuffer;



import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import static sb6.vmath.MathHelper.*;


public class Stereo extends Application {
    private double last_time = 0.0;
    private double total_time = 0.0;
    private Matrix4x4f scale_bias_matrix = new Matrix4x4f(
    		new float[]{0.5f, 0.0f, 0.0f, 0.0f},
    		new float[]{0.0f, 0.5f, 0.0f, 0.0f},
    		new float[]{0.0f, 0.0f, 0.5f, 0.0f},
    		new float[]{0.5f, 0.5f, 0.5f, 1.0f}
            );

    private int          view_program;

    static class Uniforms
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
            int   specular_albedo;
            int   diffuse_albedo;
        }
        
        Light light = new Light();
        View view = new View();
    }
    Uniforms uniforms = new Uniforms();

    
    private static class ObjectData
    {
        SBMObject      obj = new SBMObject();
        Matrix4x4f     model_matrix;
    }
    ObjectData[] objects = new ObjectData[]{ new ObjectData(), new ObjectData(), new ObjectData(), new ObjectData()};
    private int OBJECT_COUNT = objects.length;

    Matrix4x4f     light_view_matrix = new Matrix4x4f();
    Matrix4x4f     light_proj_matrix = new Matrix4x4f();

    Matrix4x4f[]   camera_view_matrix = new Matrix4x4f[]{new Matrix4x4f(), new Matrix4x4f()};
    Matrix4x4f     camera_proj_matrix = new Matrix4x4f();

    private int     quad_vao;

    float           separation;

    static final int RENDER_FULL = 0;
    static final int RENDER_LIGHT = 1;
    static final int RENDER_DEPTH = 2;
    private volatile int mode = RENDER_FULL;
    private volatile boolean paused;
    
	public Stereo() {
		super("OpenGL SuperBible - Texture Coordinates");
	}

	
	

	@Override
	protected void init() {
		super.init();
	    info.flags.fullscreen = true;
	    info.flags.stereo = true;
	}




	protected void startup() throws IOException
	{
	    load_shaders();
	
	    int i;
	
	    String[] object_names =
	    {
	        getMediaPath() + "/objects/dragon.sbm",
	        getMediaPath() + "/objects/sphere.sbm",
	        getMediaPath() + "/objects/cube.sbm",
	        getMediaPath() + "/objects/torus.sbm"
	    };
	
	    for (i = 0; i < OBJECT_COUNT; i++)
	    {
	        objects[i].obj.load(object_names[i]);
	    }
	
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
	
	    camera_view_matrix[0] = Matrix4x4f.lookat(Vector3f.sub(view_position, new Vector3f(separation, 0.0f, 0.0f)),
	    		new Vector3f(0.0f, 0.0f, -50.0f),
	    		new Vector3f(0.0f, 1.0f, 0.0f));
	
	    camera_view_matrix[1] = Matrix4x4f.lookat(Vector3f.add(view_position, new Vector3f(separation, 0.0f, 0.0f)),
	    		new Vector3f(0.0f, 0.0f, -50.0f),
	    		new Vector3f(0.0f, 1.0f, 0.0f));
	
	    objects[0].model_matrix = Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f)
	                              .mulRotate(20.0f, 1.0f, 0.0f, 0.0f) 
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
	
	    render_scene(total_time);
	}
	
	protected void render_scene(double currentTime)
	{
	    Matrix4x4f shadow_sbpv_matrix = Matrix4x4f.multiply(scale_bias_matrix, light_proj_matrix).mul(light_view_matrix);
	
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	    glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	    glUseProgram(view_program);
	    glActiveTexture(GL_TEXTURE0);
	    glUniformMatrix4(uniforms.view.proj_matrix, false, camera_proj_matrix.toFloatBuffer());
	    glDrawBuffer(GL_BACK);
	
	    int i, j;
	
	    final FloatBuffer[] diffuse_colors =
	    {
	        BufferUtilsHelper.createFloatBuffer(new float[]{1.0f, 0.6f, 0.3f}),
	        BufferUtilsHelper.createFloatBuffer(new float[]{0.2f, 0.8f, 0.9f}),
	        BufferUtilsHelper.createFloatBuffer(new float[]{0.3f, 0.9f, 0.4f}),
	        BufferUtilsHelper.createFloatBuffer(new float[]{0.5f, 0.2f, 1.0f})
	    };
	
	    for (j = 0; j < 2; j++)
	    {
	        int buffs[] = { GL_BACK_LEFT, GL_BACK_RIGHT };
	        glDrawBuffer(buffs[j]);
	        glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	        glClearBuffer1f(GL_DEPTH, 0, 1f);
	        for (i = 0; i < 4; i++)
	        {
	            Matrix4x4f model_matrix = objects[i].model_matrix;
	            Matrix4x4f shadow_matrix = Matrix4x4f.multiply(shadow_sbpv_matrix, model_matrix);
	            glUniformMatrix4(uniforms.view.shadow_matrix, false, shadow_matrix.toFloatBuffer());
	            Matrix4x4f camera_object_mv_matrix = Matrix4x4f.multiply(camera_view_matrix[j], objects[i].model_matrix);
	            glUniformMatrix4(uniforms.view.mv_matrix, false, camera_object_mv_matrix.toFloatBuffer());
	            glUniform1i(uniforms.view.full_shading, mode == RENDER_FULL ? 1 : 0);
	            glUniform3(uniforms.view.diffuse_albedo, diffuse_colors[i]);
	            objects[i].obj.render();
	        }
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
	            case 'Z':
	                separation += 0.05f;
	                break;
	            case 'X':
	                separation -= 0.05f;
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
	
	protected void load_shaders() throws IOException
	{
	    int vs;
	    int fs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/stereo/stereo-render.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/stereo/stereo-render.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (view_program != 0)
	        glDeleteProgram(view_program);
	
	    view_program = Program.link(true, vs, fs);
	
	    glDeleteShader(vs);
	    glDeleteShader(fs);
	
	    uniforms.view.proj_matrix = glGetUniformLocation(view_program, "proj_matrix");
	    uniforms.view.mv_matrix = glGetUniformLocation(view_program, "mv_matrix");
	    uniforms.view.shadow_matrix = glGetUniformLocation(view_program, "shadow_matrix");
	    uniforms.view.full_shading = glGetUniformLocation(view_program, "full_shading");
	    uniforms.view.specular_albedo = glGetUniformLocation(view_program, "specular_albedo");
	    uniforms.view.diffuse_albedo = glGetUniformLocation(view_program, "diffuse_albedo");
	}


	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		System.err.println("Please note: I was not able to test this functionality,\n"
						 + "because my machine does not support stereo rendering.  \n"
						 + "Please review the code and remove this notice.");
		System.exit(-1);

		// this starts the application, once you have remove the previous line.
		new Stereo().run();
	}


}
