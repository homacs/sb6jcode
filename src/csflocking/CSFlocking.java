package csflocking;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import static sb6.vmath.MathHelper.*;
import sb6.vmath.Matrix4x4f;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL43.*;

public class CSFlocking extends Application {
    private static final int WORKGROUP_SIZE  = 256;
    private static final int NUM_WORKGROUPS  = 64;
	private static final int FLOCK_SIZE      = (NUM_WORKGROUPS * WORKGROUP_SIZE);
    private int      flock_update_program = 0;
    private int      flock_render_program = 0;

    private int[]    flock_buffer = new int[2];

    private int[]    flock_render_vao = new int[2];
    private int      geometry_buffer;

    static class FlockMember
    {
        Vector3f position;
        int pad1;
        Vector3f velocity;
        int pad2;
        
		public static int sizeof() {
			return 2 * (Vector3f.sizeof() + BufferUtilsHelper.SIZEOF_INTEGER);
		}

		public void write(ByteBuffer stream) {
			position.toByteBuffer(stream);
			stream.putInt(pad1);
			velocity.toByteBuffer(stream);
			stream.putInt(pad2);
		}
    };

    class Uniforms
    {
        class Update
        {
            int       goal;
        } 
        Update update = new Update();
        class Render
        {
            int      mvp;
        } 
        Render render = new Render();
    } 
    private Uniforms uniforms = new Uniforms();

    private int      frame_index = 0;

	public CSFlocking() {
		super("OpenGL SuperBible - Compute Shader Flocking");
	}

    protected void startup() throws IOException
    {
        // This is position and normal data for a paper airplane
        Vector3f geometry[] =
        {
            // Positions
            new Vector3f(-5.0f, 1.0f, 0.0f),
            new Vector3f(-1.0f, 1.5f, 0.0f),
            new Vector3f(-1.0f, 1.5f, 7.0f),
            new Vector3f(0.0f, 0.0f, 0.0f),
            new Vector3f(0.0f, 0.0f, 10.0f),
            new Vector3f(1.0f, 1.5f, 0.0f),
            new Vector3f(1.0f, 1.5f, 7.0f),
            new Vector3f(5.0f, 1.0f, 0.0f),

            // Normals
            new Vector3f(0.0f),
            new Vector3f(0.0f),
            new Vector3f(0.107f, -0.859f, 0.00f),
            new Vector3f(0.832f, 0.554f, 0.00f),
            new Vector3f(-0.59f, -0.395f, 0.00f),
            new Vector3f(-0.832f, 0.554f, 0.00f),
            new Vector3f(0.295f, -0.196f, 0.00f),
            new Vector3f(0.124f, 0.992f, 0.00f),
        };

        load_shaders();

        flock_buffer[0] = glGenBuffers();
        flock_buffer[1] = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, flock_buffer[0]);
        glBufferData(GL_SHADER_STORAGE_BUFFER, FLOCK_SIZE * FlockMember.sizeof(), GL_DYNAMIC_COPY);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, flock_buffer[1]);
        glBufferData(GL_SHADER_STORAGE_BUFFER, FLOCK_SIZE * FlockMember.sizeof(), GL_DYNAMIC_COPY);

        int i;

        geometry_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, geometry_buffer);

        // we are using glMapBuffer instead of glBufferData(GL_ARRAY_BUFFER, 
        // (long)(Vector3f.sizeof() * geometry.length), geometry, GL_STATIC_DRAW);
        // because it is more efficient in our case.
        glBufferData(GL_ARRAY_BUFFER, (long)(Vector3f.sizeof() * geometry.length), GL_STATIC_DRAW);
        ByteBuffer stream = glMapBuffer(GL_ARRAY_BUFFER, GL_WRITE_ONLY);
        
        
        for (Vector3f v : geometry) {
        	v.toByteBuffer(stream);
        }
        glUnmapBuffer(GL_ARRAY_BUFFER);
        
        flock_render_vao[0] = glGenVertexArrays();
        flock_render_vao[1] = glGenVertexArrays();

        for (i = 0; i < 2; i++)
        {
            glBindVertexArray(flock_render_vao[i]);
            glBindBuffer(GL_ARRAY_BUFFER, geometry_buffer);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, MemoryUtil.NULL);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, (8 * Vector3f.sizeof()));

            glBindBuffer(GL_ARRAY_BUFFER, flock_buffer[i]);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, FlockMember.sizeof(), MemoryUtil.NULL);
            glVertexAttribPointer(3, 3, GL_FLOAT, false, FlockMember.sizeof(), Matrix4x4f.sizeof());
            glVertexAttribDivisor(2, 1);
            glVertexAttribDivisor(3, 1);

            glEnableVertexAttribArray(0);
            glEnableVertexAttribArray(1);
            glEnableVertexAttribArray(2);
            glEnableVertexAttribArray(3);
        }

        glBindBuffer(GL_ARRAY_BUFFER, flock_buffer[0]);
        stream = glMapBufferRange(GL_ARRAY_BUFFER, 0, FLOCK_SIZE * FlockMember.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        FlockMember fm = new FlockMember();
        Vector3f subtrahend = new Vector3f(0.5f);
        for (i = 0; i < FLOCK_SIZE; i++)
        {
            fm.position = (Vector3f.random().sub(subtrahend)).mul(300.0f);
            fm.velocity = (Vector3f.random().sub(subtrahend));
            fm.write(stream);
        }

        glUnmapBuffer(GL_ARRAY_BUFFER);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double T)
    {
        float t = (float)T;

        glUseProgram(flock_update_program);

        Vector3f goal = new Vector3f(sinf(t * 0.34f),
                                       cosf(t * 0.29f),
                                       sinf(t * 0.12f) * cosf(t * 0.5f));

        goal = goal.mul(new Vector3f(35.0f, 25.0f, 60.0f));
        FloatBuffer goal_fb = BufferUtils.createFloatBuffer(3);
        goal.toFloatBuffer(goal_fb);
        goal_fb.rewind();
        glUniform3(uniforms.update.goal, goal_fb);

        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, flock_buffer[frame_index]);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, flock_buffer[frame_index ^ 1]);

        glDispatchCompute(NUM_WORKGROUPS, 1, 1);

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        glUseProgram(flock_render_program);

        Matrix4x4f mv_matrix = Matrix4x4f.lookat(new Vector3f(0.0f, 0.0f, -400.0f),
        										new Vector3f(0.0f, 0.0f, 0.0f),
        										new Vector3f(0.0f, 1.0f, 0.0f));
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     3000.0f);
        Matrix4x4f mvp = Matrix4x4f.multiply(proj_matrix, mv_matrix);

        glUniformMatrix4(uniforms.render.mvp, false, mvp.toFloatBuffer());

        glBindVertexArray(flock_render_vao[frame_index]);

        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 8, FLOCK_SIZE);

        frame_index ^= 1; // toggle by XOR
    }

    private void load_shaders() throws IOException
    {
        if (flock_update_program != 0)
            glDeleteProgram(flock_update_program);

        if (flock_render_program != 0)
            glDeleteProgram(flock_render_program);

        int vs;
        int fs;
        int cs;

        cs = Shader.load(getMediaPath() + "/shaders/flocking/flocking.cs.glsl", GL_COMPUTE_SHADER);

        flock_update_program = Program.link(true, cs);

        uniforms.update.goal = glGetUniformLocation(flock_update_program, "goal");

        vs = Shader.load(getMediaPath() + "/shaders/flocking/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/flocking/render.fs.glsl", GL_FRAGMENT_SHADER);

        flock_render_program = Program.link(true, vs, fs);

        uniforms.render.mvp = glGetUniformLocation(flock_render_program, "mvp");
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action != GLFW.GLFW_PRESS)
        {
            switch (key)
            {
                case 'R': 
                    load_shaders();
                    break;
            }
        }
    }

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String[] args) {
		new CSFlocking().run();
	}


}
