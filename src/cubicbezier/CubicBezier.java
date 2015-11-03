package cubicbezier;

import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_DEPTH;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FILL;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_FRONT_AND_BACK;
import static org.lwjgl.opengl.GL11.GL_LINE;
import static org.lwjgl.opengl.GL11.GL_LINES;
import static org.lwjgl.opengl.GL11.GL_POINTS;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glPointSize;
import static org.lwjgl.opengl.GL11.glPolygonMode;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glUniform4;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL30.glMapBufferRange;
import static org.lwjgl.opengl.GL40.GL_PATCHES;
import static org.lwjgl.opengl.GL40.GL_PATCH_VERTICES;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL40.glPatchParameteri;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector4f;

public class CubicBezier extends Application {
	private int tess_program;
	private int draw_cp_program;
	private int patch_vao;
	private int patch_buffer;
	private int cage_indices;
	final float patch_initializer[] =     {
	        -1.0f,  -1.0f,  0.0f,
	        -0.33f, -1.0f,  0.0f,
	         0.33f, -1.0f,  0.0f,
	         1.0f,  -1.0f,  0.0f,

	        -1.0f,  -0.33f, 0.0f,
	        -0.33f, -0.33f, 0.0f,
	         0.33f, -0.33f, 0.0f,
	         1.0f,  -0.33f, 0.0f,

	        -1.0f,   0.33f, 0.0f,
	        -0.33f,  0.33f, 0.0f,
	         0.33f,  0.33f, 0.0f,
	         1.0f,   0.33f, 0.0f,

	        -1.0f,   1.0f,  0.0f,
	        -0.33f,  1.0f,  0.0f,
	         0.33f,  1.0f,  0.0f,
	         1.0f,   1.0f,  0.0f,
	    };

	private FloatBuffer patch_data = BufferUtilsHelper
			.createFloatBuffer(patch_initializer);

	private transient boolean show_points;
	private transient boolean show_cage;
	private transient boolean wireframe;
	private transient boolean paused;

	private double last_time = 0.0;
	private double total_time = 0.0;

	class Uniforms {
		class Patch {
			int mv_matrix;
			int proj_matrix;
			int mvp;
		}

		class ControlPoint {
			int draw_color;
			int mvp;
		}

		Patch patch = new Patch();
		ControlPoint control_point = new ControlPoint();

	}

	Uniforms uniforms = new Uniforms();

	public CubicBezier() {
		super("OpenGL SuperBible - Cubic Bezier Patch");
		super.init();
	}

	protected void startup() throws IOException {
		load_shaders();

		patch_vao = glGenVertexArrays();
		glBindVertexArray(patch_vao);

		patch_buffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, patch_buffer);
		glBufferData(GL_ARRAY_BUFFER, patch_data, GL_DYNAMIC_DRAW);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, MemoryUtil.NULL);
		glEnableVertexAttribArray(0);

		final short indices[] =     {
		        0, 1, 1, 2, 2, 3,
		        4, 5, 5, 6, 6, 7,
		        8, 9, 9, 10, 10, 11,
		        12, 13, 13, 14, 14, 15,

		        0, 4, 4, 8, 8, 12,
		        1, 5, 5, 9, 9, 13,
		        2, 6, 6, 10, 10, 14,
		        3, 7, 7, 11, 11, 15
		    };

		cage_indices = glGenBuffers();
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cage_indices);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER,
				BufferUtilsHelper.createShortBuffer(indices), GL_STATIC_DRAW);
	}

	protected void render(double currentTime) {

		int i;

		if (!paused)
			total_time += (currentTime - last_time);
		last_time = currentTime;

		float t = (float) total_time;

		glViewport(0, 0, info.windowWidth, info.windowHeight);
		GLAPIHelper.glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 0.0f);
		GLAPIHelper.glClearBuffer1f(GL_DEPTH, 0, 1.0f);

		glEnable(GL_DEPTH_TEST);

		//
		// In the original of this example the programmer used the invalidate bit
		// which invalidates the current content and requires us to reinitialise it.
		//
		// We instead decided to initialise it once and modify only those elements of
		// the vectors which actually change. I guess, the effect will be marginal, 
		// since the hardware will still copy just larger chunks of memory from user 
		// to GC space.
		//
		//		ByteBuffer bptr = glMapBufferRange(GL_ARRAY_BUFFER, 0,
		//				BufferUtilsHelper.sizeof(patch_data), GL_MAP_WRITE_BIT
		//						| GL_MAP_INVALIDATE_BUFFER_BIT);
		if (!paused) {
			ByteBuffer bptr = glMapBufferRange(GL_ARRAY_BUFFER, 0,
					BufferUtilsHelper.sizeof(patch_data), GL_MAP_WRITE_BIT);
			FloatBuffer p = bptr.asFloatBuffer();
			
	
			
			// TODO: do we really need to write this every time again?
			//		p.put(patch_initializer);
	
			for (i = 0; i < 16; i++) {
				float fi = (float) i / 16.0f;
				p.put(i * 3 + 2, MathHelper.sinf(t * (0.2f + fi * 0.3f)));
			}
			
			
			glUnmapBuffer(GL_ARRAY_BUFFER);
		}
		glUseProgram(tess_program);

		Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
				(float) info.windowWidth / (float) info.windowHeight, 1.0f,
				1000.0f);
		Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f)
				.mul(Matrix4x4f.rotate(t * 10.0f, 0.0f, 1.0f, 0.0f))
				.mul(Matrix4x4f.rotate(t * 17.0f, 1.0f, 0.0f, 0.0f));
		Matrix4x4f mvp_matrix = Matrix4x4f.multiply(proj_matrix, mv_matrix);
		
		glUniformMatrix4(uniforms.patch.mv_matrix, false,
				BufferUtilsHelper.createFloatBuffer(mv_matrix.getData()));
		glUniformMatrix4(uniforms.patch.proj_matrix, false,
				BufferUtilsHelper.createFloatBuffer(proj_matrix.getData()));
		glUniformMatrix4(uniforms.patch.mvp, false, BufferUtilsHelper
				.createFloatBuffer(mvp_matrix.getData()));

		if (wireframe) {
			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		} else {
			glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		}

		glPatchParameteri(GL_PATCH_VERTICES, 16);
		glDrawArrays(GL_PATCHES, 0, 16);

		glUseProgram(draw_cp_program);
		glUniformMatrix4(uniforms.control_point.mvp, false, BufferUtilsHelper
				.createFloatBuffer(mvp_matrix.getData()));

		if (show_points) {
			glPointSize(9.0f);
			glUniform4(uniforms.control_point.draw_color,
					BufferUtilsHelper.createFloatBuffer(new Vector4f(0.2f,
							0.7f, 0.9f, 1.0f).getData()));
			glDrawArrays(GL_POINTS, 0, 16);
		}

		if (show_cage) {
			glUniform4(uniforms.control_point.draw_color,
					BufferUtilsHelper.createFloatBuffer(new Vector4f(0.7f,
							0.9f, 0.2f, 1.0f).getData()));
			glDrawElements(GL_LINES, 48, GL_UNSIGNED_SHORT, MemoryUtil.NULL);
		}
	}
	void load_shaders() throws IOException
	{
	    if (tess_program != 0)
	        glDeleteProgram(tess_program);

	    int shaders[] = new int[4];

	    shaders[0] = Shader.load(getMediaPath() + "/shaders/cubicbezier/cubicbezier.vs.glsl", GL_VERTEX_SHADER);
	    shaders[1] = Shader.load(getMediaPath() + "/shaders/cubicbezier/cubicbezier.tcs.glsl", GL_TESS_CONTROL_SHADER);
	    shaders[2] = Shader.load(getMediaPath() + "/shaders/cubicbezier/cubicbezier.tes.glsl", GL_TESS_EVALUATION_SHADER);
	    shaders[3] = Shader.load(getMediaPath() + "/shaders/cubicbezier/cubicbezier.fs.glsl", GL_FRAGMENT_SHADER);

	    tess_program = Program.link_from_shaders(shaders, true, true);

	    uniforms.patch.mv_matrix = glGetUniformLocation(tess_program, "mv_matrix");
	    uniforms.patch.proj_matrix = glGetUniformLocation(tess_program, "proj_matrix");
	    uniforms.patch.mvp = glGetUniformLocation(tess_program, "mvp");

	    if (draw_cp_program != 0)
	        glDeleteProgram(draw_cp_program);

	    int vs = Shader.load(getMediaPath() + "/shaders/cubicbezier/draw-control-points.vs.glsl", GL_VERTEX_SHADER);
	    int fs = Shader.load(getMediaPath() + "/shaders/cubicbezier/draw-control-points.fs.glsl", GL_FRAGMENT_SHADER);

	    draw_cp_program = Program.link(true, vs, fs);

	    uniforms.control_point.draw_color = glGetUniformLocation(draw_cp_program, "draw_color");
	    uniforms.control_point.mvp = glGetUniformLocation(draw_cp_program, "mvp");
	}

	protected void onKey(int key, int action) throws IOException
	{
	    if (action != 0)
	    {
	        switch (key)
	        {
	            case 'C': show_cage = !show_cage;
	                break;
	            case 'X': show_points = !show_points;
	                break;
	            case 'W': wireframe = !wireframe;
	                break;
	            case 'P': paused = !paused;
	                break;
	            case 'R':
	                load_shaders();
	                break;
	            default:
	                break;
	        }
	    }
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}
	
	public static void main (String[] args) {
		CubicBezier app = new CubicBezier();
		app.run();
	}
}
