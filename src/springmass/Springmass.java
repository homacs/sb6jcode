package springmass;


import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ADD;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_SUBTRACT;
import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_INT;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_COPY;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.GL_RASTERIZER_DISCARD;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.GL_SEPARATE_ATTRIBS;
import static org.lwjgl.opengl.GL30.GL_TRANSFORM_FEEDBACK_BUFFER;
import static org.lwjgl.opengl.GL30.glBeginTransformFeedback;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glEndTransformFeedback;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glMapBufferRange;
import static org.lwjgl.opengl.GL30.glTransformFeedbackVaryings;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_BUFFER;
import static org.lwjgl.opengl.GL31.glTexBuffer;
import static org.lwjgl.opengl.GL40.*;
import static sb6.vmath.MathHelper.cosf;
import static sb6.vmath.MathHelper.sinf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;

public class Springmass extends Application {
	static int POSITION_A;
	static int POSITION_B;
	static int VELOCITY_A;
	static int VELOCITY_B;
	static int CONNECTION;

    static int POINTS_X            = 50;
    static int POINTS_Y            = 50;
    static int POINTS_TOTAL        = (POINTS_X * POINTS_Y);
    static int CONNECTIONS_TOTAL   = (POINTS_X - 1) * POINTS_Y + (POINTS_Y - 1) * POINTS_X;

    /** list of vertex array object names */
    IntBuffer	m_vao = BufferUtils.createIntBuffer(2);
    
    /** list of vertex buffer object names */
    IntBuffer	m_vbo = BufferUtils.createIntBuffer(5);
    
    /** name of the element array buffer with the indices for  */
    int			m_index_buffer;
    
    /** texture buffer object (TBO) to hold vertex position data 
     * in addition to a regular attribute array.
     * It is bound to TBO and the vertex attribute associated 
     * with the position input to the vertex shader.*/
    IntBuffer	m_pos_tbo = BufferUtils.createIntBuffer(2);
    
    /** Shader program to update point positions based on physical 
     * calculations. */
    int			m_update_program;
    
    /** Shader program to render the image */
    int			m_render_program;
    
    /** */
    int			m_iteration_index;

    volatile boolean		draw_points;
    volatile boolean		draw_lines;
    volatile int			iterations_per_frame;

	public Springmass() {
		super("OpenGL SuperBible - Spring-Mass Simulator");
		
		m_iteration_index = 0;
        m_update_program = 0;
        m_render_program = 0;
        draw_points = true;
        draw_lines = true;
        iterations_per_frame = 16;
        
        init();
        
	}
	
	@Override
	protected void startup() throws Throwable {
        int i, j;

        load_shaders();
        
        /* Vector4f[POINTS_TOTAL] */ 
        FloatBuffer initial_positions = BufferUtils.createFloatBuffer(4 * POINTS_TOTAL); 
        /* Vector3f[POINTS_TOTAL] */ 
        FloatBuffer initial_velocities = BufferUtils.createFloatBuffer(3 * POINTS_TOTAL);
        /* Vector4i[POINTS_TOTAL] */ 
        IntBuffer connection_vectors = BufferUtils.createIntBuffer(4 * POINTS_TOTAL);

        int n = 0;

        for (j = 0; j < POINTS_Y; j++) {
            float fj = (float)j / (float)POINTS_Y;
            for (i = 0; i < POINTS_X; i++) {
                float fi = (float)i / (float)POINTS_X;

                initial_positions.put(n*4 + 0, (fi - 0.5f) * (float)POINTS_X);
                initial_positions.put(n*4 + 1, (fj - 0.5f) * (float)POINTS_Y);
                initial_positions.put(n*4 + 2, 0.6f * sinf(fi) * cosf(fj));
                initial_positions.put(n*4 + 3, 1.0f);
                
                initial_velocities.put(n*3 + 0, 0.0f);
                initial_velocities.put(n*3 + 1, 0.0f);
                initial_velocities.put(n*3 + 2, 0.0f);

                connection_vectors.put(n*4 + 0, -1);
                connection_vectors.put(n*4 + 1, -1);
                connection_vectors.put(n*4 + 2, -1);
                connection_vectors.put(n*4 + 3, -1);

                if (j != (POINTS_Y - 1))
                {
                    if (i != 0)
                        connection_vectors.put(n*4 + 0, n - 1);

                    if (j != 0)
                        connection_vectors.put(n*4 + 1, n - POINTS_X);

                    if (i != (POINTS_X - 1))
                        connection_vectors.put(n*4 + 2, n + 1);

                    if (j != (POINTS_Y - 1))
                        connection_vectors.put(n*4 + 3, n + POINTS_X);
                }
                n++;
            }
        }

        glGenVertexArrays(m_vao);
        glGenBuffers(m_vbo);

        for (i = 0; i < 2; i++) {
            glBindVertexArray(m_vao.get(i));

            // TODO: need this? 
            initial_positions.rewind();
            
            glBindBuffer(GL_ARRAY_BUFFER, m_vbo.get(POSITION_A + i));
            glBufferData(GL_ARRAY_BUFFER, initial_positions, GL_DYNAMIC_COPY);
            glVertexAttribPointer(0, 4, GL_FLOAT, false, 0, BufferUtilsHelper.NULL);
            glEnableVertexAttribArray(0);

            // TODO: need this? 
            initial_velocities.rewind();
            
            glBindBuffer(GL_ARRAY_BUFFER, m_vbo.get(VELOCITY_A + i));
            glBufferData(GL_ARRAY_BUFFER, initial_velocities, GL_DYNAMIC_COPY);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, BufferUtilsHelper.NULL);
            glEnableVertexAttribArray(1);

            // TODO: need this? 
            connection_vectors.rewind();
            
            glBindBuffer(GL_ARRAY_BUFFER, m_vbo.get(CONNECTION));
            glBufferData(GL_ARRAY_BUFFER, connection_vectors, GL_STATIC_DRAW);
            glVertexAttribIPointer(2, 4, GL_INT, 0, BufferUtilsHelper.NULL);
            glEnableVertexAttribArray(2);
        }

        glGenTextures(m_pos_tbo);
        glBindTexture(GL_TEXTURE_BUFFER, m_pos_tbo.get(0));
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, m_vbo.get(POSITION_A));
        glBindTexture(GL_TEXTURE_BUFFER, m_pos_tbo.get(1));
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, m_vbo.get(POSITION_B));

        int lines = (POINTS_X - 1) * POINTS_Y + (POINTS_Y - 1) * POINTS_X;

        m_index_buffer = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_index_buffer);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, lines * 2 * BufferUtilsHelper.SIZEOF_INTEGER, GL_STATIC_DRAW);


        ByteBuffer pointer = glMapBufferRange(GL_ELEMENT_ARRAY_BUFFER, 0, lines * 2 * BufferUtilsHelper.SIZEOF_INTEGER, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        IntBuffer e = pointer.asIntBuffer();
        
        for (j = 0; j < POINTS_Y; j++)  
        {
            for (i = 0; i < POINTS_X - 1; i++)
            {
                e.put(i + j * POINTS_X);
                e.put(1 + i + j * POINTS_X);
            }
        }

        for (i = 0; i < POINTS_X; i++)
        {
            for (j = 0; j < POINTS_Y - 1; j++)
            {
                e.put(i + j * POINTS_X);
                e.put(1 + i + j * POINTS_X);
            }
        }
        // TODO: need this?
        e.rewind();
        
        glUnmapBuffer(GL_ELEMENT_ARRAY_BUFFER);	
    }

	@Override
	protected void shutdown() throws Throwable {
        glDeleteProgram(m_update_program);
        glDeleteBuffers(m_vbo);
        glDeleteVertexArrays(m_vao);
	}
	
	@Override
	protected void render(double currentTime) throws Throwable {
		//
		// In this example, the GPU is used for two purposes:
		// 1. it calculates the new positions of the points
		// 2. based on the new positions it renders a grid 
		//    with points and connecting lines.
		//
		
        int i;
        
        //
        // Activate the shader program for the physics
        //
        glUseProgram(m_update_program);
        
        //
        // Disable rasterisation, meaning: we do not generate a 
        // visible result in this first pass.
        //
        glEnable(GL_RASTERIZER_DISCARD);
        
        
        for (i = iterations_per_frame; i != 0; --i)
        {
            glBindVertexArray(m_vao.get(m_iteration_index & 1));
            glBindTexture(GL_TEXTURE_BUFFER, m_pos_tbo.get(m_iteration_index & 1));
            m_iteration_index++;
            glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 0, m_vbo.get(POSITION_A + (m_iteration_index & 1)));
            glBindBufferBase(GL_TRANSFORM_FEEDBACK_BUFFER, 1, m_vbo.get(VELOCITY_A + (m_iteration_index & 1)));
            glBeginTransformFeedback(GL_POINTS);
            glDrawArrays(GL_POINTS, 0, POINTS_TOTAL);
            glEndTransformFeedback();
        }

        //
        // Reenable rasterisation so we can produce a visual output 
        // in the following pass
        //
        glDisable(GL_RASTERIZER_DISCARD);

        
        //
        // Now we render the actual image
        //
        
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);

        
        // switch to the render shader program
        glUseProgram(m_render_program);

        // draw points and lines
        if (draw_points)
        {
            glPointSize(4.0f);
            glDrawArrays(GL_POINTS, 0, POINTS_TOTAL);
        }

        if (draw_lines)
        {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_index_buffer);
            glDrawElements(GL_LINES, CONNECTIONS_TOTAL * 2, GL_UNSIGNED_INT, 0);
        }
	}
	protected void onKey(int key, int action) throws IOException
	    {
	        if (action != 0)
	        {
	            switch (key)
	            {
	                case 'R': load_shaders();
	                    break;
	                case 'L': draw_lines = !draw_lines;
	                    break;
	                case 'P': draw_points = !draw_points;
	                    break;
	                case GLFW_KEY_KP_ADD: iterations_per_frame++;
	                    break;
	                case GLFW_KEY_KP_SUBTRACT: iterations_per_frame--;
	                    break;
	            }
	        }
	    }

    private void load_shaders() throws IOException
    {
        int vs;
        int fs;

        vs = Shader.load(getMediaPath() + "/shaders/springmass/update.vs.glsl", GL_VERTEX_SHADER);
       
        if (m_update_program != 0)
            glDeleteProgram(m_update_program);
        m_update_program = glCreateProgram();
        glAttachShader(m_update_program, vs);

        final String tf_varyings[] = 
        {
            "tf_position_mass",
            "tf_velocity"
        };

        glTransformFeedbackVaryings(m_update_program, tf_varyings, GL_SEPARATE_ATTRIBS);

        Program.link(m_update_program, true);

        glDeleteShader(vs);

        vs = Shader.load(getMediaPath() + "/shaders/springmass/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/springmass/render.fs.glsl", GL_FRAGMENT_SHADER);

        Program.link_from_shaders(new int[]{vs, fs}, m_render_program != 0, true);
    }

    public static void main (String[] args) {
    	Springmass springmass = new Springmass();
    	springmass.run();
    }

}
