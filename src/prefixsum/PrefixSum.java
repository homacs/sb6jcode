package prefixsum;

import java.util.Random;

import static sb6.BufferUtilsHelper.*;
import sb6.application.Application;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;


public class PrefixSum extends Application {
	private static final int NUM_ELEMENTS = 2048;
	
    private int[] data_buffer = new int[2];

    private float[] input_data = new float[NUM_ELEMENTS];
    private FloatBuffer input_data_buf = createFloatBuffer(input_data);
    private float[] output_data = new float[NUM_ELEMENTS];

    private int  prefix_sum_prog;

    private Random rng = new Random(0x13371337);
    
	public PrefixSum() {
		super("OpenGL SuperBible - 1D Prefix Sum");
	}

	private float random_float() {
	    return rng.nextFloat();
	}

	
	protected void startup() throws IOException {
		data_buffer[0] = glGenBuffers();
		data_buffer[1] = glGenBuffers();
	
	    glBindBuffer(GL_SHADER_STORAGE_BUFFER, data_buffer[0]);
	    glBufferData(GL_SHADER_STORAGE_BUFFER, NUM_ELEMENTS * SIZEOF_FLOAT, GL_DYNAMIC_DRAW);
	
	    glBindBuffer(GL_SHADER_STORAGE_BUFFER, data_buffer[1]);
	    glBufferData(GL_SHADER_STORAGE_BUFFER, NUM_ELEMENTS * SIZEOF_FLOAT, GL_DYNAMIC_COPY);
	
	    int i;
	
	    for (i = 0; i < NUM_ELEMENTS; i++)
	    {
	        input_data[i] = random_float();
	    }
	
	    prefix_sum(input_data, output_data, NUM_ELEMENTS);
	
	    load_shaders();
	}
	
	protected void render(double currentTime)
	{
	    FloatBuffer ptr;
	
	    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 0, data_buffer[0], 0, SIZEOF_FLOAT * NUM_ELEMENTS);
	    input_data_buf.put(input_data);
	    input_data_buf.rewind();
	    // actually, it would be more efficient to use glMapBuffer instead
	    glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, input_data_buf);
	
	    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 1, data_buffer[1], 0, SIZEOF_FLOAT * NUM_ELEMENTS);
	
	    glUseProgram(prefix_sum_prog);
	    glDispatchCompute(1, 1, 1);
	
	    glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
	    glFinish();
	
	    glBindBufferRange(GL_SHADER_STORAGE_BUFFER, 0, data_buffer[1], 0, SIZEOF_FLOAT * NUM_ELEMENTS);
	    ByteBuffer ptr_b = glMapBufferRange(GL_SHADER_STORAGE_BUFFER, 0, SIZEOF_FLOAT * NUM_ELEMENTS, GL_MAP_READ_BIT);
	    ptr = ptr_b.asFloatBuffer();
	    
	    if (DEBUG) {
	    
		    System.out.printf("%2.2f %2.2f %2.2f %2.2f %2.2f %2.2f %2.2f %2.2f " +
		                    "%2.2f %2.2f %2.2f %2.2f %2.2f %2.2f %2.2f %2.2f\n",
		                    ptr.get(0), ptr.get(1), ptr.get(2), ptr.get(3), ptr.get(4), ptr.get(5), ptr.get(6), ptr.get(7),
		                    ptr.get(8), ptr.get(9), ptr.get(10), ptr.get(11), ptr.get(12), ptr.get(13), ptr.get(14), ptr.get(15));
	    }
	    
	    glUnmapBuffer(GL_SHADER_STORAGE_BUFFER);
	
	    if (DEBUG) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return;
			}
	    }
	    
	    super.requestExit(0);
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
	
	void load_shaders() throws IOException
	{
	    int cs = Shader.load(getMediaPath() + "/shaders/prefixsum/prefixsum.cs.glsl", GL_COMPUTE_SHADER);
	
	    if (prefix_sum_prog != 0)
	        glDeleteProgram(prefix_sum_prog);
	
	    prefix_sum_prog = Program.link(true, cs);
	    Shader.delete(cs);

	    /*
	    prefix_sum_prog = glCreateProgram();
	    glAttachShader(prefix_sum_prog, cs);
	
	    glLinkProgram(prefix_sum_prog);
	
	    int n;
	    glGetIntegerv(GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS, &n);
	    */
	
	    glShaderStorageBlockBinding(prefix_sum_prog, 0, 0);
	    glShaderStorageBlockBinding(prefix_sum_prog, 1, 1);
	}
	
	void prefix_sum(final float[] input, float[] output, int elements)
	{
	    float f = 0.0f;
	    int i;
	
	    for (i = 0; i < elements; i++)
	    {
	        f += input[i];
	        output[i] = f;
	    }
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		new PrefixSum().run();
	}


}
