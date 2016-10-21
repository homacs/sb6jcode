package sb6.shader;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_INFO_LOG_LENGTH;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetShader;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_CONTROL_SHADER;
import static org.lwjgl.opengl.GL40.GL_TESS_EVALUATION_SHADER;
import static org.lwjgl.opengl.GL43.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.StringTokenizer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import sb6.FileSystem;

public class Shader {

	public static final int INVALID_SHADER_ID = -1;
	
	
	public static int load(String filename, int shaderType) throws IOException {
		return compileShader(shaderType, new File(filename));
	}
	
	public static int compileShader(int shaderType, File file) throws IOException {
		  int shaderId = INVALID_SHADER_ID;
		  String code = FileSystem.readTextFile(file);
		  shaderId  = compile(shaderType, code, file.getName());
		  return shaderId;
	}
	
	public static int compile(int shaderType, String sourceCode) {
		return compile(shaderType, sourceCode, "in-memory");
	}
	
	public static int compile(int shaderType, String sourceCode, String sourceName) {
		int shaderId = INVALID_SHADER_ID;
		if (checkShaderType(shaderType)) {
			checkASCII(sourceCode);
			
			// Create and compile vertex shader
			shaderId = glCreateShader(shaderType);
			glShaderSource(shaderId, sourceCode);
			glCompileShader(shaderId);
			if (!checkCompilerResult(shaderId, sourceName)) {
				glDeleteShader(shaderId);
				return INVALID_SHADER_ID;
			}
		} else {
			System.err.println("Unsupported shader type " + shaderType);
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
		case GL_COMPUTE_SHADER:
			return true;
		default: 
			return false;
		}
	}
	
	
	public static boolean checkCompilerResult(int shaderId, String filename) {
		if (shaderId == INVALID_SHADER_ID) {
			System.err.println("Invalid shader id!");
			return false;
		}
		// check result
		IntBuffer status = BufferUtils.createIntBuffer(1);
		glGetShader(shaderId, GL_COMPILE_STATUS, status);

		if (status.get(0) != GL_TRUE) {
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
	

	public static boolean checkCompilerResult(int shader) {
		return checkCompilerResult(shader, "in-memory");
	}

	public static void delete(int shader) {
		glDeleteShader(shader);
	}


}
