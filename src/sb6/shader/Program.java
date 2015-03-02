package sb6.shader;

import static org.lwjgl.opengl.GL20.*;

public class Program {

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

		glLinkProgram(program);

		if (check_errors) {
			Shader.checkLinkerResult(program);
		}

		if (delete_shaders) {
			for (i = 0; i < shaders.length; i++) {
				glDeleteShader(shaders[i]);
			}
		}

		return program;
	}

}
