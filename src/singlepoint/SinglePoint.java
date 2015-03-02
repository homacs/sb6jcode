package singlepoint;


import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.StringTokenizer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import sb6.application.Application;

public class SinglePoint extends Application {

	private int rendering_program;
	private int vertexArrayObjectId;
	private FloatBuffer backgroundColor4f;

	public SinglePoint() {
		super("Rendering our first point");
	}

	protected void startup()
	{
		backgroundColor4f = BufferUtils.createFloatBuffer(4);
		rendering_program = compileShaders();
		vertexArrayObjectId = glGenVertexArrays();
		glBindVertexArray(vertexArrayObjectId);
	}
	protected void shutdown()
	{
		glDeleteVertexArrays(vertexArrayObjectId);
		glDeleteProgram(rendering_program);
	}
	
	// Our rendering function
	protected void render(double currentTime) {
		backgroundColor4f.put(0, (float)Math.sin(currentTime) * 0.5f + 0.5f);
		backgroundColor4f.put(1, (float)Math.cos(currentTime) * 0.5f + 0.5f);
		backgroundColor4f.put(2, 0.f);
		backgroundColor4f.put(3, 1.f);
		glClearBuffer(GL_COLOR, 0, backgroundColor4f);
		// Use the program object we created earlier for rendering
		glUseProgram(rendering_program);
		// set point size to 40 pixels
		glPointSize(40.0f);
		// Draw one point
		glDrawArrays(GL_POINTS, 0, 1);
	}
	
	private int compileShaders()
	{
		int vertex_shader;
		int fragment_shader;
		int program;
		// Source code for vertex shader
		String vertex_shader_str = new String(
			"#version 430 core 							\n" +
			" 											\n" +
			"void main(void) 							\n" +
			"{ 											\n" +
			"   gl_Position = vec4(0.0, 0.0, 0.5, 1.0);	\n" +
			"} 											\n"
		);
		
		
		// Source code for fragment shader
		String fragment_shader_str = new String (
			"#version 430 core 							\n" +
			" 											\n" +
			"out vec4 color; 							\n" +
			"											\n" +
			"void main(void) 							\n" +
			"{ 											\n" +
			"  color = vec4(0.0, 0.8, 1.0, 1.0); 		\n" +
			"} \n"
		);
		
		// Create and compile vertex shader
		vertex_shader = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vertex_shader, vertex_shader_str);
		glCompileShader(vertex_shader);
		checkCompilerResult(vertex_shader, "in memory");

		// Create and compile fragment shader
		fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fragment_shader, fragment_shader_str);
		glCompileShader(fragment_shader);
		checkCompilerResult(fragment_shader, "in memory");
		// Create program, attach shaders to it, and link it
		program = glCreateProgram();
		glAttachShader(program, vertex_shader);
		glAttachShader(program, fragment_shader);
		glLinkProgram(program);
		glValidateProgram(program);

		checkLinkerResult(program);
		
		// Delete the shaders as the program has them now
		glDeleteShader(vertex_shader);
		glDeleteShader(fragment_shader);
		return program;
	}	
	
	private boolean checkLinkerResult(int program) {
		IntBuffer status= BufferUtils.createIntBuffer(1);
		glGetProgram(program, GL_LINK_STATUS, status);
		if (status.get() != GL_TRUE) {
			System.err.println("GLSL linker failed...");
			IntBuffer bufSize = BufferUtils.createIntBuffer(1);
			glGetProgram(program, GL_INFO_LOG_LENGTH, bufSize);
			ByteBuffer infoLog = BufferUtils.createByteBuffer(Character.SIZE/8 * (bufSize.get(0) + 1));
			glGetProgramInfoLog(program, bufSize, infoLog);
			System.err.println( MemoryUtil.memDecodeUTF8(infoLog, bufSize.get(0)));
			glDeleteProgram(program);
			return false;
		}
		return true;
	}

	private boolean checkCompilerResult(int shaderId, String filename) {
		// check result
		IntBuffer status = BufferUtils.createIntBuffer(1);
		
		glGetShader(shaderId, GL_COMPILE_STATUS, status);

		if (status.get() != GL_TRUE) {
			printCompilerErrors(System.err, shaderId, filename);
			glDeleteShader(shaderId);
			return false;
		}
		return true;
	}

	void printCompilerErrors(PrintStream out, int shaderId, String filename) {
		IntBuffer bufSize = BufferUtils.createIntBuffer(1);
		glGetShader(shaderId, GL_INFO_LOG_LENGTH, bufSize);
		ByteBuffer infoLog = BufferUtils.createByteBuffer(Character.SIZE/8 * bufSize.get(0));
		glGetShaderInfoLog(shaderId, bufSize, infoLog);

		StringTokenizer tokenizer = new StringTokenizer(MemoryUtil.memDecodeUTF8(infoLog,bufSize.get(0)), "\n");

		while (tokenizer.hasMoreTokens()) {
			String line = tokenizer.nextToken();
			int messageStart = line.indexOf(')');
			int lineNo = Integer.parseInt(line.substring(line.indexOf('('), messageStart));		
			long columnNo = 0; // currently unknown
			out.println(filename + ':' + lineNo + ':' + columnNo + line.substring(messageStart+1));
		}

	}

	public static void main(String[] args) {
		new SinglePoint().run();
	}

}
