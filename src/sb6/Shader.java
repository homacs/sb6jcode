package sb6;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgram;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glValidateProgram;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.StringTokenizer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

public class Shader {

	public static final int INVALID_PROGRAM_ID = -1;
	public static final int INVALID_SHADER_ID = -1;
	
	
	public static int load(String filename, int shaderType) throws IOException {
		return compileShader(shaderType, new File(filename));
	}


	
	public static int compileShader(int shaderType, File file) throws IOException {
		  int shaderId = INVALID_SHADER_ID;
		  String code = FileSystem.readTextFile(file);
		  shaderId  = compile(shaderType, code);
		  return shaderId;
	}
	
	public static int compile(int shaderType, String sourceCode) {
		int shaderId = INVALID_SHADER_ID;
		if (checkShaderType(shaderType)) {
			checkASCII(sourceCode);
			
			// Create and compile vertex shader
			shaderId = glCreateShader(shaderType);
			glShaderSource(shaderId, sourceCode);
			glCompileShader(shaderId);
			if (!checkCompilerResult(shaderId, "in memory")) {
				glDeleteShader(shaderId);
				return INVALID_SHADER_ID;
			}
		}
		return shaderId;
	}

	private static void checkASCII(String sourceCode) {
		for(char c : sourceCode.toCharArray()) {
			if (c < 0 || c > 128) {
				throw new Error("shader source code contains non-ascii character '" + c + "'. Compilation or linking would fail. Aborting.");
			}
		}
	}



	private static boolean checkShaderType(int shader_type) {
		
		switch(shader_type) {
		case GL_VERTEX_SHADER:
		case GL_FRAGMENT_SHADER:
		case GL_GEOMETRY_SHADER:
		case GL_TESS_CONTROL_SHADER:
		case GL_TESS_EVALUATION_SHADER:
			return true;
		default: 
			return false;
		}
	}
	
	
	public static boolean checkCompilerResult(int shaderId, String filename) {
		// check result
		IntBuffer status = BufferUtils.createIntBuffer(1);
		
		glGetShader(shaderId, GL_COMPILE_STATUS, status);

		if (status.get() != GL_TRUE) {
			printCompilerErrors(System.err, shaderId, filename);
			return false;
		}
		return true;
	}

	static void printCompilerErrors(PrintStream out, int shaderId, String filename) {
		IntBuffer bufSize = BufferUtils.createIntBuffer(1);
		glGetShader(shaderId, GL_INFO_LOG_LENGTH, bufSize);
		ByteBuffer infoLog = BufferUtils.createByteBuffer(Character.SIZE/8 * bufSize.get(0));
		glGetShaderInfoLog(shaderId, bufSize, infoLog);

		StringTokenizer tokenizer = new StringTokenizer(MemoryUtil.memDecodeUTF8(infoLog,bufSize.get(0)), "\n");

		while (tokenizer.hasMoreTokens()) {
			String line = tokenizer.nextToken();
			int messageStart = line.indexOf(')');
			int lineNo = Integer.parseInt(line.substring(line.indexOf('(')+1, messageStart));		
			long columnNo = 0; // currently unknown
			out.println(filename + ':' + lineNo + ':' + columnNo + line.substring(messageStart+1));
		}

	}
	public static int linkProgram (int ... shaderIds) {
		int programId = glCreateProgram();
		for(int shaderId : shaderIds) {
			glAttachShader(programId, shaderId);
		}
		glLinkProgram(programId);
		glValidateProgram(programId);

		if (!checkLinkerResult(programId)) {
			glDeleteProgram(programId);
			return INVALID_PROGRAM_ID;
		}
		
		return programId;
	}
	
	public static boolean checkLinkerResult(int program) {
		IntBuffer status= BufferUtils.createIntBuffer(1);
		glGetProgram(program, GL_LINK_STATUS, status);
		if (status.get() != GL_TRUE) {
			System.err.println("GLSL linker failed...");
			IntBuffer bufSize = BufferUtils.createIntBuffer(1);
			glGetProgram(program, GL_INFO_LOG_LENGTH, bufSize);
			ByteBuffer infoLog = BufferUtils.createByteBuffer(Character.SIZE/8 * (bufSize.get(0) + 1));
			glGetProgramInfoLog(program, bufSize, infoLog);
			System.err.println( MemoryUtil.memDecodeUTF8(infoLog, bufSize.get(0)));
			return false;
		}
		return true;
	}

	public static void checkCompilerResult(int shader) {
		checkCompilerResult(shader, "in-memory");
	}


}
