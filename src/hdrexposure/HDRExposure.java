package hdrexposure;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class HDRExposure extends Application {
    private int      texture;
    private int      program;
    private int      vao;
    private volatile float       exposure = 1.0f;
	private int uniform_exposure;

	public HDRExposure() {
		super("OpenGL SuperBible - HDR Exposure");
	}


    protected void startup() throws IOException
    {
        // Load texture from file
        texture = KTX.load(getMediaPath() + "/textures/treelights_2k.ktx");

        // Now bind it to the context using the GL_TEXTURE_2D binding point
        glBindTexture(GL_TEXTURE_2D, texture);


		String vs_source =
		    "#version 420 core                                                              \n" +
		    "                                                                               \n" +
		    "void main(void)                                                                \n" +
		    "{                                                                              \n" +
		    "    const vec4 vertices[] = vec4[](vec4(-1.0, -1.0, 0.5, 1.0),                 \n" +
		    "                                   vec4( 1.0, -1.0, 0.5, 1.0),                 \n" +
		    "                                   vec4(-1.0,  1.0, 0.5, 1.0),                 \n" +
		    "                                   vec4( 1.0,  1.0, 0.5, 1.0));                \n" +
		    "                                                                               \n" +
		    "    gl_Position = vertices[gl_VertexID];                                       \n" +
		    "}                                                                              \n"
		;
		
		String fs_source =
		    "#version 430 core                                                              \n" +
		    "                                                                               \n" +
		    "uniform sampler2D s;                                                           \n" +
		    "                                                                               \n" +
		    "uniform float exposure;														\n" +
		    "																				\n" +
		    "out vec4 color;                                                                \n" +
		    "                                                                               \n" +
		    "void main(void)                                                                \n" +
		    "{                                                                              \n" +
		    "    vec4 c = texture(s, 2.0 * gl_FragCoord.xy / textureSize(s, 0));            \n" +
		    "    c.xyz = vec3(1.0) - exp(-c.xyz * exposure);                                \n" +
		    "    color = c;                                                                 \n" +
		    "}                                                                              \n"
		;

        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        program = Program.link(true, vs, fs);

        uniform_exposure = glGetUniformLocation(program, "exposure");
		if (uniform_exposure == -1) fatal("uniform attribute 'color' not found");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);
    }

    protected void shutdown()
    {
        glDeleteProgram(program);
        glDeleteVertexArrays(vao);
        glDeleteTextures(texture);
    }

    protected void render(double t)
    {
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);

        glUseProgram(program);
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glUniform1f(uniform_exposure, exposure);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    protected void onKey(int key, int action)
    {
        if (action != GLFW.GLFW_PRESS)
            return;

        switch (key)
        {
            case GLFW.GLFW_KEY_KP_ADD:
                    exposure *= 1.1f;
                break;
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                    exposure /= 1.1f;
                break;
        }
    }

	
	
	public static void main(String[] args) {
		new HDRExposure().run();
	}

}
