package hdrtonemap;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.FloatBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;

public class HDRToneMap extends Application {
    private int      tex_src;
    private int      tex_lut;

    private int      program_naive = 0;
    private int      program_exposure = 0;
    private int      program_adaptive = 0;
    private int      vao;
    private volatile float       exposure = 1f;
    private volatile int         mode = 0;

    class Uniforms
    {
        class Exposure
        {
            int exposure;
        } 
        Exposure exposure = new Exposure();
    }
    Uniforms uniforms = new Uniforms();

	public HDRToneMap() {
		super("OpenGL SuperBible - HDR Tone Mapping");
	}

    protected void startup() throws IOException
    {
        // Load texture from file
        tex_src = KTX.load(getMediaPath() + "/textures/treelights_2k.ktx");

        // Now bind it to the context using the GL_TEXTURE_2D binding point
        glBindTexture(GL_TEXTURE_2D, tex_src);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        load_shaders();

        FloatBuffer exposureLUT   = BufferUtilsHelper.createFloatBuffer(new float[]{ 11.0f, 6.0f, 3.2f, 2.8f, 2.2f, 1.90f, 1.80f, 1.80f, 1.70f, 1.70f,  1.60f, 1.60f, 1.50f, 1.50f, 1.40f, 1.40f, 1.30f, 1.20f, 1.10f, 1.00f });

        tex_lut = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, tex_lut);
        glTexStorage1D(GL_TEXTURE_1D, 1, GL_R32F, 20);
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, 20, GL_RED, GL_FLOAT, exposureLUT);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    }

    protected void shutdown()
    {
        glDeleteProgram(program_adaptive);
        glDeleteProgram(program_exposure);
        glDeleteProgram(program_naive);
        glDeleteVertexArrays(vao);
        glDeleteTextures(tex_src);
        glDeleteTextures(tex_lut);
    }

    protected void render(double t)
    {
        glViewport(0, 0, info.windowWidth, info.windowHeight);

//        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_1D, tex_lut);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tex_src);

        switch (mode)
        {
            case 0:
                glUseProgram(program_naive);
                break;
            case 1:
                glUseProgram(program_exposure);
                glUniform1f(uniforms.exposure.exposure, exposure);
                break;
            case 2:
                glUseProgram(program_adaptive);
                break;
        }
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action != GLFW.GLFW_PRESS)
            return;

        switch (key)
        {
            case '1':
            case '2':
            case '3':
                    mode = key - '1';
                break;
            case 'R':
                    load_shaders();
                break;
            case 'M':
                    mode = (mode + 1) % 3;
                break;
            case GLFW.GLFW_KEY_KP_ADD:
                    exposure *= 1.1f;
                break;
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                    exposure /= 1.1f;
                break;
        }
    }

    void load_shaders() throws IOException
    {
        int vs;
        int fs;

        if (program_naive != 0)
            glDeleteProgram(program_naive);

        vs = Shader.load(getMediaPath() + "/shaders/hdrtonemap/tonemap.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/hdrtonemap/tonemap_naive.fs.glsl", GL_FRAGMENT_SHADER);
        program_naive = Program.link(true, vs, fs);

        glDeleteShader(fs);

        fs = Shader.load(getMediaPath() + "/shaders/hdrtonemap/tonemap_adaptive.fs.glsl", GL_FRAGMENT_SHADER);

        if (program_adaptive != 0)
            glDeleteProgram(program_adaptive);

        program_adaptive = Program.link(true, vs, fs);

        glDeleteShader(fs);

        fs = Shader.load(getMediaPath() + "/shaders/hdrtonemap/tonemap_exposure.fs.glsl", GL_FRAGMENT_SHADER);

        if (program_exposure != 0)
            glDeleteProgram(program_exposure);

        program_exposure = Program.link(true, vs, fs);

        uniforms.exposure.exposure = glGetUniformLocation(program_exposure, "exposure");

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

	public static void main(String[] args) {
		new HDRToneMap().run();
	}

}
