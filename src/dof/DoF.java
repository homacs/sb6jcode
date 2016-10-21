package dof;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector4f;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.FloatBuffer;

import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

public class DoF extends Application {
	private static final int FBO_SIZE                = 2048;
	
	private int          view_program = 0;
	private int          filter_program = 0;
	private int          display_program = 0;

    class Uniforms
    {
        class dof
        {
            int   focal_distance;
            int   focal_depth;
        } 
        dof dof = new dof();
        
        class View
        {
            int   mv_matrix;
            int   proj_matrix;
            int   full_shading;
            int   diffuse_albedo;
        } 
        View view = new View();
    } 
    Uniforms uniforms = new Uniforms();

    private int          depth_fbo;
    private int          depth_tex;
    private int          color_tex;
    private int          temp_tex;

    private static final int OBJECT_COUNT = 5;
    
    class ObjectData
    {
        SBMObject     obj = new SBMObject();
        Matrix4x4f    model_matrix;
        Vector4f      diffuse_albedo;
    } 
    private ObjectData[] objects = new ObjectData[OBJECT_COUNT];

    private Matrix4x4f     camera_view_matrix;
    private Matrix4x4f     camera_proj_matrix;

    private int          quad_vao;

    private boolean paused;

    private float          focal_distance = 40f;
    private float          focal_depth = 50f;

    private double last_time = 0.0;
    private double total_time = 0.0;

    Matrix4x4f scale_bias_matrix = new Matrix4x4f(new Vector4f(0.5f, 0.0f, 0.0f, 0.0f),
            new Vector4f(0.0f, 0.5f, 0.0f, 0.0f),
            new Vector4f(0.0f, 0.0f, 0.5f, 0.0f),
            new Vector4f(0.5f, 0.5f, 0.5f, 1.0f));

	public DoF() {
		super("OpenGL SuperBible - Depth Of Field");
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
	        getMediaPath() + "/objects/cube.sbm",
	        getMediaPath() + "/objects/cube.sbm",
	    };
	
	    Vector4f[] object_colors =
	    {
	        new Vector4f(1.0f, 0.7f, 0.8f, 1.0f),
	        new Vector4f(0.7f, 0.8f, 1.0f, 1.0f),
	        new Vector4f(0.3f, 0.9f, 0.4f, 1.0f),
	        new Vector4f(0.6f, 0.4f, 0.9f, 1.0f),
	        new Vector4f(0.8f, 0.2f, 0.1f, 1.0f),
	    };
	
	    for (i = 0; i < OBJECT_COUNT; i++)
	    {
	    	objects[i] = new ObjectData();
	        objects[i].obj.load(object_names[i]);
	        objects[i].diffuse_albedo = object_colors[i];
	    }
	
	    depth_fbo = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, depth_fbo);
	
	    depth_tex = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, depth_tex);
	    glTexStorage2D(GL_TEXTURE_2D, 11, GL_DEPTH_COMPONENT32F, FBO_SIZE, FBO_SIZE);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	
	    color_tex = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, color_tex);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, FBO_SIZE, FBO_SIZE);
	
	    temp_tex = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, temp_tex);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, FBO_SIZE, FBO_SIZE);
	
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depth_tex, 0);
	    glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, color_tex, 0);
	
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
	
	    Vector3f view_position = new Vector3f(0.0f, 0.0f, 40.0f);
	
	    camera_proj_matrix = Matrix4x4f.perspective(50.0f,
	                                            (float)info.windowWidth / (float)info.windowHeight,
	                                            2.0f,
	                                            300.0f);
	
	    camera_view_matrix = Matrix4x4f.lookat(view_position,
	                                       new Vector3f(0.0f),
	                                       new Vector3f(0.0f, 1.0f, 0.0f));
	
	    objects[0].model_matrix = Matrix4x4f.translate(5.0f, 0.0f, 20.0f)
	                              .mul(Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f))
	                              .mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	                              .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
	
	    objects[1].model_matrix = Matrix4x4f.translate(-5.0f, 0.0f, 0.0f)
	    		.mul( Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f))
	                              .mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	                              .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
	
	    objects[2].model_matrix = Matrix4x4f.translate(-15.0f, 0.0f, -20.0f)
	    		.mul(Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f))
	                              .mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	                              .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
	
	    objects[3].model_matrix = Matrix4x4f.translate(-25.0f, 0.0f, -40.0f)
	    		.mul(Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f))
	                              .mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	                              .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
	
	    objects[4].model_matrix = Matrix4x4f.translate(-35.0f, 0.0f, -60.0f)
	    		.mul(Matrix4x4f.rotate(f * 14.5f, 0.0f, 1.0f, 0.0f))
	                              .mul(Matrix4x4f.rotate(20.0f, 1.0f, 0.0f, 0.0f))
	                              .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
	
	    glEnable(GL_DEPTH_TEST);
	    render_scene(total_time);
	
	    glUseProgram(filter_program);
	
	    glBindImageTexture(0, color_tex, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
	    glBindImageTexture(1, temp_tex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
	
	    glDispatchCompute(info.windowHeight, 1, 1);
	
	    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	
	    glBindImageTexture(0, temp_tex, 0, false, 0, GL_READ_ONLY, GL_RGBA32F);
	    glBindImageTexture(1, color_tex, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
	
	    glDispatchCompute(info.windowWidth, 1, 1);
	
	    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	
	    glBindTexture(GL_TEXTURE_2D, color_tex);
	    glDisable(GL_DEPTH_TEST);
	    glUseProgram(display_program);
	    glUniform1f(uniforms.dof.focal_distance, focal_distance);
	    glUniform1f(uniforms.dof.focal_depth, focal_depth);
	    glBindVertexArray(quad_vao);
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}
	
	private void render_scene(double currentTime)
	{
	
	    glBindFramebuffer(GL_FRAMEBUFFER, depth_fbo);
	
	    glDrawBuffers(GL_COLOR_ATTACHMENT0);
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	    glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	    glClearBuffer1f(GL_DEPTH, 0, 1f);
	    glUseProgram(view_program);
	    glUniformMatrix4(uniforms.view.proj_matrix, false, camera_proj_matrix.toFloatBuffer());
	
	    glClearBuffer1f(GL_DEPTH, 0, 1f);
	
	    
	    Matrix4x4f mv_matrix;
	    FloatBuffer diffuse_albedo_buf = BufferUtils.createFloatBuffer(4);
	    int i;
	    for (i = 0; i < OBJECT_COUNT; i++)
	    {
	        mv_matrix = Matrix4x4f.multiply(camera_view_matrix, objects[i].model_matrix);
	        glUniformMatrix4(uniforms.view.mv_matrix, false, mv_matrix.toFloatBuffer());
	        objects[i].diffuse_albedo.toFloatBuffer(diffuse_albedo_buf);
	        diffuse_albedo_buf.rewind();
	        glUniform3(uniforms.view.diffuse_albedo, diffuse_albedo_buf);
	        objects[0].obj.render();
	    }
	
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action == GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case 'Q':
	                focal_distance *= 1.1f;
	                break;
	            case'A':
	                focal_distance /= 1.1f;
	                break;
	            case 'W':
	                focal_depth *= 1.1f;
	                break;
	            case 'S':
	                focal_depth /= 1.1f;
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
	    int vs, fs, cs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/dof/render.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/dof/render.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (view_program != 0)
	        glDeleteProgram(view_program);
	
	    view_program = Program.link(true, vs, fs);
	    Shader.delete(vs);
	    Shader.delete(fs);
	
	    uniforms.view.proj_matrix = glGetUniformLocation(view_program, "proj_matrix");
	    uniforms.view.mv_matrix = glGetUniformLocation(view_program, "mv_matrix");
	    uniforms.view.full_shading = glGetUniformLocation(view_program, "full_shading");
	    uniforms.view.diffuse_albedo = glGetUniformLocation(view_program, "diffuse_albedo");
	
	    vs = Shader.load(getMediaPath() + "/shaders/dof/display.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/dof/display.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (display_program != 0)
	        glDeleteProgram(display_program);
	    
	    
	    display_program = Program.link(true, vs, fs);
	    Shader.delete(vs);
	    Shader.delete(fs);
	
	    uniforms.dof.focal_distance = glGetUniformLocation(display_program, "focal_distance");
	    uniforms.dof.focal_depth = glGetUniformLocation(display_program, "focal_depth");
	
	    cs = Shader.load(getMediaPath() + "/shaders/dof/gensat.cs.glsl", GL_COMPUTE_SHADER);
	
	    if (filter_program != 0)
	        glDeleteProgram(filter_program);
	
	    filter_program = Program.link(true, cs);
	    Shader.delete(cs);
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		new DoF().run();
	}


}
