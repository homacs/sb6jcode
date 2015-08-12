package clipdistance;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*; // framebuffer texture
import static org.lwjgl.opengl.GL42.*; // textures
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector4f;
import sb6.vmath.VectorNf;
import static sb6.vmath.MathHelper.*;

public class ClipDistance extends Application {
	
    SBMObject	object = new SBMObject();
    int			render_program;
    boolean		paused;

	int   uniforms_proj_matrix;
	int   uniforms_mv_matrix;
	int   uniforms_clip_plane;
	int   uniforms_clip_sphere;

	public ClipDistance() {
		super("OpenGL SuperBible - Clip Distance");
		
		init();
	}

	@Override
	protected void startup() throws Throwable {
		object.load(getMediaPath() + "/objects/dragon.sbm");
		
	    load_shaders();
	}

    static double last_time = 0.0;
    static double total_time = 0.0;
    
	@Override
	protected void render(double currentTime) throws Throwable {
	    final float one = 1.0f;


	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;

	    float f = (float)total_time;

        GLAPIHelper.glClearBuffer4f(GL_COLOR, 0,  0.0f, 0.0f, 0.0f, 0.0f); // black
        GLAPIHelper.glClearBuffer1f(GL_DEPTH, 0, one);

	    glUseProgram(render_program);

	    Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
	                                                 (float)info.windowWidth / (float)info.windowHeight,
	                                                 0.1f,
	                                                 1000.0f);

	    Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -15.0f).mul(
	    						Matrix4x4f.rotate(f * 0.34f, 0.0f, 1.0f, 0.0f) ).mul(
	                            Matrix4x4f.translate(0.0f, -4.0f, 0.0f));

	    Matrix4x4f plane_matrix = Matrix4x4f.rotate(f * 6.0f, 1.0f, 0.0f, 0.0f) .mul(
	                               Matrix4x4f.rotate(f * 7.3f, 0.0f, 1.0f, 0.0f));

	    float[] data = new float[4];
		plane_matrix.getColumn(0, data);
	    Vector4f plane = new Vector4f(data);
	    plane.set(3, 0.0f);
	    plane = Vector4f.normalize(plane);

	    Vector4f clip_sphere = new Vector4f(sinf(f * 0.7f) * 3.0f, cosf(f * 1.9f) * 3.0f, sinf(f * 0.1f) * 3.0f, cosf(f * 1.7f) + 2.5f);

	    
	    glUniformMatrix4(uniforms_proj_matrix, false, BufferUtilsHelper.createFloatBuffer(proj_matrix.getData()));
	    glUniformMatrix4(uniforms_mv_matrix, false, BufferUtilsHelper.createFloatBuffer(mv_matrix.getData()));
	    glUniform4(uniforms_clip_plane, BufferUtilsHelper.createFloatBuffer(plane.getData()));
	    glUniform4(uniforms_clip_sphere, BufferUtilsHelper.createFloatBuffer(clip_sphere.getData()));

	    glEnable(GL_DEPTH_TEST);
	    glEnable(GL_CLIP_DISTANCE0);
	    glEnable(GL_CLIP_DISTANCE1);

	    object.render();
		
	}


	private void load_shaders() throws IOException {
	    if (render_program != 0)
	        glDeleteProgram(render_program);

	    int shaders[] =
	    {
	        Shader.load(getMediaPath() + "/shaders/clipdistance/render.vs.glsl", GL_VERTEX_SHADER),
	        Shader.load(getMediaPath() + "/shaders/clipdistance/render.fs.glsl", GL_FRAGMENT_SHADER)
	    };

	    render_program = Program.link_from_shaders(shaders, true);

	    uniforms_proj_matrix = glGetUniformLocation(render_program, "proj_matrix");
	    uniforms_mv_matrix = glGetUniformLocation(render_program, "mv_matrix");
	    uniforms_clip_plane = glGetUniformLocation(render_program, "clip_plane");
	    uniforms_clip_sphere = glGetUniformLocation(render_program, "clip_sphere");
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action != 0)
	    {
	        switch (key)
	        {
	            case 'P':
	                paused = !paused;
	                break;
	            case 'R': 
	                load_shaders();
	                break;
	        }
	    }
	}

	@Override
	protected void shutdown() throws Throwable {
		// intentionally empty (straight forward port)
	}

	public static void main (String[] args) {
		new ClipDistance().run();
	}

}
