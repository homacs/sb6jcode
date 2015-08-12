package sb6.shader;

import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

public class Program {

	public static final int INVALID_PROGRAM_ID = -1;

	
	public static int link_from_shaders(int[] shaders) {
		return link_from_shaders(shaders, true, true);
	}
	public static int link_from_shaders(int[] shaders, boolean delete_shaders) {
		return link_from_shaders(shaders, delete_shaders, true);
	}
	public static int link_from_shaders(int[] shaders, boolean delete_shaders,
					boolean check_errors) {
		int i;

		int program;

		program = glCreateProgram();

		for (i = 0; i < shaders.length; i++) {
			glAttachShader(program, shaders[i]);
		}

		link(program, check_errors);
		
		if (delete_shaders) {
			for (i = 0; i < shaders.length; i++) {
				glDeleteShader(shaders[i]);
			}
		}

		return program;
	}
	
	/** Links an assembled program and checks for errors. */
	public static int link(int program, boolean check_errors) {
		glLinkProgram(program);

		if (check_errors) {
			if (!checkLinkerResult(program)) {
				glDeleteProgram(program);
				return INVALID_PROGRAM_ID;
			}
		}
		return program;
	}

	/** attaches shaders and links in one pass */
	public static int link (boolean check_errors, int ... shaderIds) {
		int programId = glCreateProgram();
		for(int shaderId : shaderIds) {
			glAttachShader(programId, shaderId);
		}
		
		return link(programId, check_errors);
	}

	public static boolean checkLinkerResult(int program) {
		glValidateProgram(program);
		
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


}
