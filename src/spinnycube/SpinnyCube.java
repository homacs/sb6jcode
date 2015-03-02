package spinnycube;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.application.Application;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;


public class SpinnyCube extends Application {

	private static final boolean MANY_CUBES = true;
	private int program;
	private int vao;
	private int mv_location;
	private int proj_location;
	private int buffer;
	private float aspect;
	private Matrix4x4f proj_matrix = new Matrix4x4f();
	private Matrix4x4f mv_matrix = new Matrix4x4f();

	public SpinnyCube() {
		super("OpenGL SuperBible - Spinny Cube");
		info.flags.fullscreen = true;
		info.flags.vsync = false;
	}

	@Override
	protected void startup() {
		String vs_source = "#version 410 core                                                  \n"
				+ "                                                                   \n"
				+ "in vec4 position;                                                  \n"
				+ "                                                                   \n"
				+ "out VS_OUT                                                         \n"
				+ "{                                                                  \n"
				+ "    vec4 color;                                                    \n"
				+ "} vs_out;                                                          \n"
				+ "                                                                   \n"
				+ "uniform mat4 mv_matrix;                                            \n"
				+ "uniform mat4 proj_matrix;                                          \n"
				+ "                                                                   \n"
				+ "void main(void)                                                    \n"
				+ "{                                                                  \n"
				+ "    gl_Position = proj_matrix * mv_matrix * position;              \n"
				+ "    vs_out.color = position * 2.0 + vec4(0.5, 0.5, 0.5, 0.0);      \n"
				+ "}                                                                  \n";
		String fs_source = "#version 410 core                                                  \n"
				+ "                                                                   \n"
				+ "out vec4 color;                                                    \n"
				+ "                                                                   \n"
				+ "in VS_OUT                                                          \n"
				+ "{                                                                  \n"
				+ "    vec4 color;                                                    \n"
				+ "} fs_in;                                                           \n"
				+ "                                                                   \n"
				+ "void main(void)                                                    \n"
				+ "{                                                                  \n"
				+ "    color = fs_in.color;                                           \n"
				+ "}                                                                  \n";

		program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
		Shader.checkCompilerResult(vs, "vs");

		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "fs");

		glAttachShader(program, vs);
		glAttachShader(program, fs);

		glLinkProgram(program);
		glValidateProgram(program);
		Shader.checkLinkerResult(program);
		glDeleteShader(vs);
		glDeleteShader(fs);

		mv_location = glGetUniformLocation(program, "mv_matrix");
		proj_location = glGetUniformLocation(program, "proj_matrix");

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		// define the triangle vertices and their color (ugly green)
		FloatBuffer vertex_positions = BufferUtilsHelper
				.createBuffer(new float[] {
			            -0.25f,  0.25f, -0.25f,
			            -0.25f, -0.25f, -0.25f,
			             0.25f, -0.25f, -0.25f,

			             0.25f, -0.25f, -0.25f,
			             0.25f,  0.25f, -0.25f,
			            -0.25f,  0.25f, -0.25f,

			             0.25f, -0.25f, -0.25f,
			             0.25f, -0.25f,  0.25f,
			             0.25f,  0.25f, -0.25f,

			             0.25f, -0.25f,  0.25f,
			             0.25f,  0.25f,  0.25f,
			             0.25f,  0.25f, -0.25f,

			             0.25f, -0.25f,  0.25f,
			            -0.25f, -0.25f,  0.25f,
			             0.25f,  0.25f,  0.25f,

			            -0.25f, -0.25f,  0.25f,
			            -0.25f,  0.25f,  0.25f,
			             0.25f,  0.25f,  0.25f,

			            -0.25f, -0.25f,  0.25f,
			            -0.25f, -0.25f, -0.25f,
			            -0.25f,  0.25f,  0.25f,

			            -0.25f, -0.25f, -0.25f,
			            -0.25f,  0.25f, -0.25f,
			            -0.25f,  0.25f,  0.25f,

			            -0.25f, -0.25f,  0.25f,
			             0.25f, -0.25f,  0.25f,
			             0.25f, -0.25f, -0.25f,

			             0.25f, -0.25f, -0.25f,
			            -0.25f, -0.25f, -0.25f,
			            -0.25f, -0.25f,  0.25f,

			            -0.25f,  0.25f, -0.25f,
			             0.25f,  0.25f, -0.25f,
			             0.25f,  0.25f,  0.25f,

			             0.25f,  0.25f,  0.25f,
			            -0.25f,  0.25f,  0.25f,
			            -0.25f,  0.25f, -0.25f
				});
		vertex_positions.rewind();

		buffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, buffer);
		glBufferData(GL_ARRAY_BUFFER, vertex_positions, GL_STATIC_DRAW);

		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, MemoryUtil.NULL); 
        glEnableVertexAttribArray(0);

		glEnable(GL_CULL_FACE);
		glFrontFace(GL_CW);

		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);

		
		
		// one fake call for initialisation
		onResize(info.getWindowWidth(),info.getWindowHeight());
	}

	@Override
	protected void shutdown() {
		glDeleteBuffers(buffer);
		glDeleteVertexArrays(vao);
		glDeleteProgram(program);
	}

	@Override
	protected void render(double currentTime) {
		glViewport(0, 0, info.getWindowWidth(), info.getWindowHeight());
		GLAPIHelper.glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f); // green
		GLAPIHelper.glClearBuffer1f(GL_DEPTH, 0, 1.0f);

		glUseProgram(program);

		// transmit projection matrix
		glUniformMatrix4(proj_location, false, proj_matrix.toFloatBuffer());

		if (MANY_CUBES) {
			final boolean OPTIMIZED = true;
			final int numCubes = 24;
			if (OPTIMIZED) {
				Matrix4x4f rot1 = new Matrix4x4f();
				Matrix4x4f rot2 = new Matrix4x4f();
				Matrix4x4f tra2 = new Matrix4x4f();
				for (int i = 0; i < numCubes; i++) {
					float f = (float) i + (float) currentTime * 0.3f;
					mv_matrix.setTranslate(0.0f, 0.0f, -6.0f) 
							.mul (rot1.setRotate((float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f))
							.mul (rot2.setRotate((float) currentTime * 21.0f, 1.0f, 0.0f, 0.0f))
							.mul (tra2.setTranslate(
									(float) Math.sin(2.1f * f) * 2.0f,
									(float) Math.cos(1.7f * f) * 2.0f,
									(float) Math.sin(1.3f * f) * (float) Math.cos(1.5f * f) * 2.0f));
					glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
					glDrawArrays(GL_TRIANGLES, 0, 36);
				}
			} else {
				for (int i = 0; i < numCubes; i++) {
					float f = (float) i + (float) currentTime * 0.3f;
					mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -6.0f);
					mv_matrix.mul(Matrix4x4f.rotate((float) currentTime * 45.0f, 0.0f,
							1.0f, 0.0f));
					mv_matrix.mul(Matrix4x4f.rotate((float) currentTime * 21.0f, 1.0f,
							0.0f, 0.0f));
					mv_matrix.mul(Matrix4x4f.translate(
							(float) Math.sin(2.1f * f) * 2.0f,
							(float) Math.cos(1.7f * f) * 2.0f,
							(float) Math.sin(1.3f * f) * (float) Math.cos(1.5f * f)
									* 2.0f));
					glUniformMatrix4(mv_location, false, mv_matrix.toFloatBuffer());
					glDrawArrays(GL_TRIANGLES, 0, 36);
				}
			}
		} else {
			float f = (float) currentTime * 0.3f;
			Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -4.0f);
			mv_matrix.mul(Matrix4x4f.translate((float) Math.sin(2.1f * f) * 0.5f,
					(float) Math.cos(1.7f * f) * 0.5f,
					(float) Math.sin(1.3f * f) * (float) Math.cos(1.5f * f)
							* 2.0f));
			Matrix4x4f rot = Matrix4x4f.rotate((float) currentTime * 45.0f, 0.0f, 1.0f, 0.0f);
			mv_matrix.mul(rot);
			mv_matrix.mul(Matrix4x4f.rotate((float) currentTime * 81.0f, 1.0f, 0.0f,
					0.0f));
			glUniformMatrix4(mv_location, false,
					mv_matrix.toFloatBuffer());
			glDrawArrays(GL_TRIANGLES, 0, 36);
		}

	}

	protected void onResize(int w, int h) {
		super.onResize(w, h);

		aspect = (float) w / (float) h;
		proj_matrix = Matrix4x4f.perspective(50.0f, aspect, 0.1f, 1000.0f);
	}

	public static void main(String[] args) {
		Application app = new SpinnyCube();
		app.run();
	}

}
