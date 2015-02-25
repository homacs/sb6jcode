package simpletexture;

import java.nio.FloatBuffer;

import sb6.Application;



import sb6.BufferUtilsHelper;
import sb6.Shader;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;


public class SimpleTexture extends Application {
    int      texture;
    int      program;
    int      vao;

    static final String vs_source =
    	    "#version 420 core                                                              \n"+
    	    "                                                                               \n"+
    	    "void main(void)                                                                \n"+
    	    "{                                                                              \n"+
    	    "    const vec4 vertices[] = vec4[](vec4( 0.75, -0.75, 0.5, 1.0),               \n"+
    	    "                                   vec4(-0.75, -0.75, 0.5, 1.0),               \n"+
    	    "                                   vec4( 0.75,  0.75, 0.5, 1.0));              \n"+
    	    "                                                                               \n"+
    	    "    gl_Position = vertices[gl_VertexID];                                       \n"+
    	    "}                                                                              \n";

   	static final String fs_source =
    	    "#version 430 core                                                              \n"+
    	    "                                                                               \n"+
    	    "uniform sampler2D s;                                                           \n"+
    	    "                                                                               \n"+
    	    "out vec4 color;                                                                \n"+
    	    "                                                                               \n"+
    	    "void main(void)                                                                \n"+
    	    "{                                                                              \n"+
    	    "    color = texture(s, gl_FragCoord.xy / textureSize(s, 0));                   \n"+
    	    "}                                                                              \n";

	public SimpleTexture() {
		super("OpenGL SuperBible - Simple Texturing");
	}

	@Override
	protected void startup() throws Throwable {
        // Generate a name for the texture
		texture = glGenTextures();

        // Now bind it to the context using the GL_TEXTURE_2D binding point
        glBindTexture(GL_TEXTURE_2D, texture);

        // Specify the amount of storage we want to use for the texture
        glTexStorage2D(GL_TEXTURE_2D,   // 2D texture
                       8,               // 8 mipmap levels
                       GL_RGBA32F,      // 32-bit floating-point RGBA data
                       256, 256);       // 256 x 256 texels

        // Define some data to upload into the texture
        float[] data = new float[256 * 256 * 4];

        // generate_texture() is a function that fills memory with image data
        generate_texture(data, 256, 256);

        // Assume the texture is already bound to the GL_TEXTURE_2D target
        FloatBuffer data_fb = BufferUtilsHelper.createBuffer(data);
        data_fb.rewind();
        glTexSubImage2D(GL_TEXTURE_2D,  // 2D texture
                        0,              // Level 0
                        0, 0,           // Offset 0, 0
                        256, 256,       // 256 x 256 texels, replace entire image
                        GL_RGBA,        // Four channel data
                        GL_FLOAT,		// Data type
                        data_fb);       // Reference to data


        program = glCreateProgram();
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source);
        glCompileShader(fs);

        Shader.checkCompilerResult(fs,"fs");

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);

        Shader.checkCompilerResult(vs,"vs");

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);

        vao = glGenVertexArrays();
        glBindVertexArray(vao);
	}

	@Override
	protected void render(double currentTime) throws Throwable {
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);

        glUseProgram(program);
        glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	@Override
	protected void shutdown() throws Throwable {
        glDeleteProgram(program);
        glDeleteVertexArrays(vao);
        glDeleteTextures(texture);

	}

	
    void generate_texture(float[] data, int width, int height)
    {
        int x, y;

        for (y = 0; y < height; y++)
        {
            for (x = 0; x < width; x++)
            {
                data[(y * width + x) * 4 + 0] = (float)((x & y) & 0xFF) / 255.0f;
                data[(y * width + x) * 4 + 1] = (float)((x | y) & 0xFF) / 255.0f;
                data[(y * width + x) * 4 + 2] = (float)((x ^ y) & 0xFF) / 255.0f;
                data[(y * width + x) * 4 + 3] = 1.0f;
            }
        }
    }

	public static void main(String[] args) {
		SimpleTexture app = new SimpleTexture();
		app.run();
	}

}
