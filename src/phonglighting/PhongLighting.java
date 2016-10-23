package phonglighting;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

public class PhongLighting extends Application {
	private static final boolean MANY_OBJECTS = true;
	
    private int          per_fragment_program = 0;
    private int          per_vertex_program = 0;

    static class UniformsBlock
    {
        Matrix4x4f     mv_matrix;
        Matrix4x4f     view_matrix;
        Matrix4x4f     proj_matrix;
		public static long sizeof() {
			return Matrix4x4f.sizeof() * 3;
		}
		public void write(ByteBuffer stream) {
			FloatBuffer fb = stream.asFloatBuffer();
			mv_matrix.toFloatBuffer(fb);
			view_matrix.toFloatBuffer(fb);
			proj_matrix.toFloatBuffer(fb);
			stream.position((int) (stream.position() + sizeof()));
		}
    };

    private int          uniforms_buffer;

    class Uniforms
    {
        int           diffuse_albedo;
        int           specular_albedo;
        int           specular_power;
    } 
    private Uniforms[] uniforms = new Uniforms[] {new Uniforms(), new Uniforms()};

    private SBMObject     object = new SBMObject();

    private volatile boolean       per_vertex = false;

	public PhongLighting() {
		super("OpenGL SuperBible - Phong Lighting");
	}
	
	protected void startup() throws IOException
	{
	    load_shaders();
	
	    uniforms_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, uniforms_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, UniformsBlock.sizeof(), GL_DYNAMIC_DRAW);
	
	    object.load(getMediaPath() + "/objects/sphere.sbm");
	
	    glEnable(GL_CULL_FACE);
	    glEnable(GL_DEPTH_TEST);
	    glDepthFunc(GL_LEQUAL);
	}
	
	protected void render(double currentTime)
	{
	
	    glUseProgram(per_vertex ? per_vertex_program : per_fragment_program);
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	
	    glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
	    glClearBuffer1f(GL_DEPTH, 0, 1f);
	
	    Vector3f view_position = new Vector3f(0.0f, 0.0f, 50.0f);
	    Matrix4x4f view_matrix = Matrix4x4f.lookat(view_position,
	                                            new Vector3f(0.0f, 0.0f, 0.0f),
	                                            new Vector3f(0.0f, 1.0f, 0.0f));
	
	    UniformsBlock block = new UniformsBlock();
	    
		if (MANY_OBJECTS) {
		    int i, j;
		
		    for (j = 0; j < 7; j++)
		    {
		        for (i = 0; i < 7; i++)
		        {
		            glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniforms_buffer);
		            ByteBuffer stream = glMapBufferRange(GL_UNIFORM_BUFFER,
                                                    0,
                                                    UniformsBlock.sizeof(),
                                                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
		
		            Matrix4x4f model_matrix = Matrix4x4f.translate((float)i * 2.75f - 8.25f, 6.75f - (float)j * 2.25f, 0.0f);
		
		            block.mv_matrix = Matrix4x4f.multiply(view_matrix, model_matrix);
		            block.view_matrix = view_matrix;
		            block.proj_matrix = Matrix4x4f.perspective(50.0f,
		                                                    (float)info.windowWidth / (float)info.windowHeight,
		                                                    0.1f,
		                                                    1000.0f);
		            block.write(stream);
		            glUnmapBuffer(GL_UNIFORM_BUFFER);
		
		            Vector3f specular_albedo = new Vector3f((float)i / 9.0f + 1.0f / 9.0f);
		            glUniform1f(uniforms[per_vertex ? 1 : 0].specular_power, MathHelper.powf(2.0f, (float)j + 2.0f));
		            glUniform3(uniforms[per_vertex ? 1 : 0].specular_albedo, specular_albedo.toFloatBuffer());
		
		            object.render();
		        }
		    }
		} else {
		    glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniforms_buffer);
		    ByteBuffer stream = glMapBufferRange(GL_UNIFORM_BUFFER,
		                                                                0,
		                                                                UniformsBlock.sizeof(),
		                                                                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
		
		    Matrix4x4f model_matrix = Matrix4x4f.scale(7.0f);
		
		    block.mv_matrix = Matrix4x4f.multiply(view_matrix, model_matrix);
		    block.view_matrix = view_matrix;
		    block.proj_matrix = Matrix4x4f.perspective(50.0f,
		                                            (float)info.windowWidth / (float)info.windowHeight,
		                                            0.1f,
		                                            1000.0f);
		    block.write(stream);
		    glUnmapBuffer(GL_UNIFORM_BUFFER);
		
		    glUniform1f(uniforms[per_vertex ? 1 : 0].specular_power, 30.0f);
		    glUniform3(uniforms[per_vertex ? 1 : 0].specular_albedo, new Vector3f(1.0f).toFloatBuffer());
		
		    object.render();
		}
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action == GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case 'R': 
	                load_shaders();
	                break;
	            case 'V':
	                per_vertex = !per_vertex;
	                break;
	        }
	    }
	}
	
	void load_shaders() throws IOException
	{
	    int vs, fs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/phonglighting/per-fragment-phong.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/phonglighting/per-fragment-phong.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (per_fragment_program != 0)
	        glDeleteProgram(per_fragment_program);
	
	    per_fragment_program = Program.link(true, vs, fs);
	
	    uniforms[0].diffuse_albedo = glGetUniformLocation(per_fragment_program, "diffuse_albedo");
	    uniforms[0].specular_albedo = glGetUniformLocation(per_fragment_program, "specular_albedo");
	    uniforms[0].specular_power = glGetUniformLocation(per_fragment_program, "specular_power");
	
	    vs = Shader.load(getMediaPath() + "/shaders/phonglighting/per-vertex-phong.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/phonglighting/per-vertex-phong.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (per_vertex_program != 0)
	        glDeleteProgram(per_vertex_program);
	
	    per_vertex_program = Program.link(true, vs, fs);
	
	    uniforms[1].diffuse_albedo = glGetUniformLocation(per_vertex_program, "diffuse_albedo");
	    uniforms[1].specular_albedo = glGetUniformLocation(per_vertex_program, "specular_albedo");
	    uniforms[1].specular_power = glGetUniformLocation(per_vertex_program, "specular_power");
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
	}
	
	public static void main(String[] args) {
		new PhongLighting().run();
	}


}
