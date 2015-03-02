package multidrawindirect;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryUtil;

import static sb6.vmath.MathHelper.*;
import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.sbm.SBMObject.SBMSubObjectDecl;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;


/**
 * Java implementation of multidrawindirect.cpp.
 * 
 * Access to the data structure DrawArrasIndirectCommand
 * has been implemented in an facet class (see 
 * {@link DrawArraysIndirectCommandFacet}).
 * <br/><br/>
 * <em>Note:</em> My GL driver complains about the vertex array 
 * buffer and index buffer to be mapped to the client 
 * memory just once at the start. I think this is an 
 * error of the driver, thinking that the buffer is 
 * still mapped to the client through glMapBuffer() 
 * while it has been certainly unmapped.
 * 
 * @author homac
 *
 */

public class MultiDrawIndirect extends Application {
	private static final int NUM_DRAWS           = 50000;

    private int              render_program;

    private SBMObject        object;

    private int              indirect_draw_buffer;
    private int              draw_index_buffer;

    class Uniforms
    {
    	int           time;
    	int           view_matrix;
    	int           proj_matrix;
    	int           viewproj_matrix;
    } 
    private Uniforms uniforms = new Uniforms();

    private final static int MODE_MULTIDRAW = 0;
    private final static int MODE_FIRST = MODE_MULTIDRAW;
    private final static int MODE_SEPARATE_DRAWS = 1;
    private final static int MODE_MAX = MODE_SEPARATE_DRAWS;

    private int             mode;
    private boolean         paused;
    private boolean         vsync;

    
    private double last_time = 0.0;
    private double total_time = 0.0;


    
	public MultiDrawIndirect() {
		super("OpenGL SuperBible - Asteroids");
		render_program = 0;
        mode = MODE_MULTIDRAW;
        paused = false;
        vsync = false;
        
        object = new SBMObject();
	}

	@Override
	protected void startup() throws Throwable {
	    int i;

	    load_shaders();

	    object.load(getMediaPath() + "objects/asteroids.sbm");

	    indirect_draw_buffer = glGenBuffers();
	    System.out.println("indirect_draw_buffer: " + indirect_draw_buffer);
	    glBindBuffer(GL_DRAW_INDIRECT_BUFFER, indirect_draw_buffer);
	    glBufferData(GL_DRAW_INDIRECT_BUFFER,
	                 NUM_DRAWS * DrawArraysIndirectCommandFacet.sizeof(),
	                 GL_STATIC_DRAW);

	    ByteBuffer ptr = glMapBufferRange(GL_DRAW_INDIRECT_BUFFER,
                0,
                NUM_DRAWS * DrawArraysIndirectCommandFacet.sizeof(),
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
	    DrawArraysIndirectCommandFacet cmd = new DrawArraysIndirectCommandFacet(ptr, 0);

	    for (i = 0; i < NUM_DRAWS; i++)
	    {
	    	cmd.offset(i * (int)DrawArraysIndirectCommandFacet.sizeof());
	    	
	        object.get_sub_object_info(i % object.get_sub_object_count(), cmd);
	        cmd.setPrimCount(1);
	        cmd.setBaseInstance(i);
	        
	        
	    }

	    glUnmapBuffer(GL_DRAW_INDIRECT_BUFFER);

	    glBindVertexArray(object.get_vao());

	    draw_index_buffer = glGenBuffers();
	    System.out.println("draw_index_buffer: " + draw_index_buffer);

	    glBindBuffer(GL_ARRAY_BUFFER, draw_index_buffer);
	    glBufferData(GL_ARRAY_BUFFER,
	                 NUM_DRAWS * BufferUtilsHelper.SIZEOF_INTEGER,
	                 GL_STATIC_DRAW);

	    ptr = glMapBufferRange(GL_ARRAY_BUFFER,
	                                   0,
	                                   NUM_DRAWS * BufferUtilsHelper.SIZEOF_INTEGER,
	                                   GL_MAP_WRITE_BIT |
	                                   GL_MAP_INVALIDATE_BUFFER_BIT);
	    IntBuffer draw_index = ptr.asIntBuffer();
	    for (i = 0; i < NUM_DRAWS; i++)
	    {
	        draw_index.put(i,i);
	    }

	    if (!glUnmapBuffer(GL_ARRAY_BUFFER)) {
	    	throw new Error("data corruption");
	    }

	    glVertexAttribIPointer(10, 1, GL_UNSIGNED_INT, 0, MemoryUtil.NULL);
	    glVertexAttribDivisor(10, 1);
	    glEnableVertexAttribArray(10);

	    glEnable(GL_DEPTH_TEST);
	    glDepthFunc(GL_LEQUAL);

	    glEnable(GL_CULL_FACE);
	}

	@Override
	protected void shutdown() {
		// this program apparently runs forever ..
	}

	@Override
	protected void render(double currentTime) {
	    int j;

	        

	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;

	    float t = (float)total_time;

	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	    glClearBuffer4f(GL_COLOR, 0,  0.0f, 0.0f, 0.0f, 0.0f);
	    glClearBuffer1f(GL_DEPTH, 0, 1.0f);

	    Matrix4x4f view_matrix = Matrix4x4f.lookat(new Vector3f(100.0f * cosf(t * 0.023f), 100.0f * cosf(t * 0.023f), 300.0f * sinf(t * 0.037f) - 600.0f),
	    		new Vector3f(0.0f, 0.0f, 260.0f),
	    		Vector3f.normalize(new Vector3f(0.1f - cosf(t * 0.1f) * 0.3f, 1.0f, 0.0f)));
	    
	    Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
	                                                       (float)info.windowWidth / (float)info.windowHeight,
	                                                       1.0f,
	                                                       2000.0f);

	    glUseProgram(render_program);

	    glUniform1f(uniforms.time, t);
	    glUniformMatrix4(uniforms.view_matrix, false, view_matrix.toFloatBuffer());
	    glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());
	    glUniformMatrix4(uniforms.viewproj_matrix, false, proj_matrix .mul (view_matrix) .toFloatBuffer());

	    glBindVertexArray(object.get_vao());

	    if (mode == MODE_MULTIDRAW)
	    {
	        glMultiDrawArraysIndirect(GL_TRIANGLES, MemoryUtil.NULL, NUM_DRAWS, 0);
	    }
	    else if (mode == MODE_SEPARATE_DRAWS)
	    {
	    	SBMSubObjectDecl objinfo = new SBMSubObjectDecl();
	    	
	        for (j = 0; j < NUM_DRAWS; j++)
	        {
	            object.get_sub_object_info(j % object.get_sub_object_count(), objinfo);
	            glDrawArraysInstancedBaseInstance(GL_TRIANGLES,
	            		objinfo.getFirst(),
	            		objinfo.getCount(),
	            		1, j);
	        }
	    }
	}

	void load_shaders() throws Throwable
	{
	    int[] shaders = new int[2];

	    shaders[0] = Shader.load(getMediaPath() + "shaders/multidrawindirect/render.vs.glsl", GL_VERTEX_SHADER);
	    shaders[1] = Shader.load(getMediaPath() + "shaders/multidrawindirect/render.fs.glsl", GL_FRAGMENT_SHADER);

	    if (render_program != 0)
	        glDeleteProgram(render_program);

	    render_program = Program.link_from_shaders(shaders, true);

	    uniforms.time            = glGetUniformLocation(render_program, "time");
	    uniforms.view_matrix     = glGetUniformLocation(render_program, "view_matrix");
	    uniforms.proj_matrix     = glGetUniformLocation(render_program, "proj_matrix");
	    uniforms.viewproj_matrix = glGetUniformLocation(render_program, "viewproj_matrix");
	}

	

	protected void onKey(int key, int action) throws Throwable
	{
	    if (action != 0)
	    {
	        switch (key)
	        {
	            case 'P':
	                paused = !paused;
	                break;
	            case 'V':
	                vsync = !vsync;
	                super.setVSync(vsync);
	                break;
	            case 'D':
	                mode++;
	                if (mode > MODE_MAX)
	                    mode = MODE_FIRST;
	                break;
	            case 'R':
	                load_shaders();
	                break;
	        }
	    }
	}

	public static void main (String[] args) {
		Application app = new MultiDrawIndirect();
		app.run();
	}

}
