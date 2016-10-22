package prefixsum2d;

import sb6.application.Application;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

public class PrefixSum2D extends Application {
	private static final int NUM_ELEMENTS = 2048;

    private int[] images = new int[3];

    private int  prefix_sum_prog = 0;
    private int  show_image_prog = 0;
    private int  dummy_vao = 0;
    
	public PrefixSum2D() {
		super("OpenGL SuperBible - 2D Prefix Sum");
	}
	
	
	protected void startup() throws IOException
	{
	    int i;
	
	    images[0] = glGenTextures();
	    images[1] = glGenTextures();
	    images[2] = glGenTextures();
	
	    images[0] = KTX.load(getMediaPath() + "/textures/salad-gray.ktx");
	
	    for (i = 1; i < 3; i++)
	    {
	        glBindTexture(GL_TEXTURE_2D, images[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, NUM_ELEMENTS, NUM_ELEMENTS);
	    }
	
	    dummy_vao = glGenVertexArrays();
	    glBindVertexArray(dummy_vao);
	
	    load_shaders();
	}
	
	protected void render(double currentTime)
	{
	    glUseProgram(prefix_sum_prog);
	
	    glBindImageTexture(0, images[0], 0, false, 0, GL_READ_ONLY, GL_R32F);
	    glBindImageTexture(1, images[1], 0, false, 0, GL_WRITE_ONLY, GL_R32F);
	
	    
	    int num_work_groups = NUM_ELEMENTS;
	    
	    glDispatchCompute(num_work_groups, 1, 1);
	
	    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	
	    glBindImageTexture(0, images[1], 0, false, 0, GL_READ_ONLY, GL_R32F);
	    glBindImageTexture(1, images[2], 0, false, 0, GL_WRITE_ONLY, GL_R32F);
	
	    glDispatchCompute(num_work_groups, 1, 1);
	
	    glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);
	
	    glBindTexture(GL_TEXTURE_2D, images[2]);
	
	    glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, images[2]);
	
	    glUseProgram(show_image_prog);
	
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	    glBindVertexArray(dummy_vao);
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action != GLFW.GLFW_PRESS)
	        return;
	
	    switch (key)
	    {
	        case 'R':   load_shaders();
	            break;
	    }
	}
	
	private void load_shaders() throws IOException
	{
	    int cs = Shader.load(getMediaPath() + "/shaders/prefixsum2d/prefixsum2d.cs.glsl", GL_COMPUTE_SHADER);
	
	    if (prefix_sum_prog != 0)
	        glDeleteProgram(prefix_sum_prog);
	
	    prefix_sum_prog = Program.link(true, cs);
	
	
	    int vs = Shader.load(getMediaPath() + "/shaders/prefixsum2d/showimage.vs.glsl", GL_VERTEX_SHADER);
	    int fs = Shader.load(getMediaPath() + "/shaders/prefixsum2d/showimage.fs.glsl", GL_FRAGMENT_SHADER);
	
	    show_image_prog = Program.link(true, vs, fs);
	}
	

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		new PrefixSum2D().run();
	}


}
