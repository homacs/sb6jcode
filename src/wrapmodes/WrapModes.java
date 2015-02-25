package wrapmodes;


import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import sb6.Application;



import sb6.BufferUtilsHelper;
import sb6.Shader;
import sb6.ktx.KTX;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.system.MemoryUtil.memAddress;


public class WrapModes extends Application {
 
    private int      texture;
    private int      program;
    private int      vao;
    private int		 uniform_offset;

    private final String vs_source =
    	    "#version 410 core                                                              \n"+
    	    "                                                                               \n"+
    	    "uniform vec2 offset;                                                           \n"+
    	    "                                                                               \n"+
    	    "out vec2 tex_coord;                                                            \n"+
    	    "                                                                               \n"+
    	    "void main(void)                                                                \n"+
    	    "{                                                                              \n"+
    	    "    const vec4 vertices[] = vec4[](vec4(-0.45, -0.45, 0.5, 1.0),               \n"+
    	    "                                   vec4( 0.45, -0.45, 0.5, 1.0),               \n"+
    	    "                                   vec4(-0.45,  0.45, 0.5, 1.0),               \n"+
    	    "                                   vec4( 0.45,  0.45, 0.5, 1.0));              \n"+
    	    "                                                                               \n"+
    	    "    gl_Position = vertices[gl_VertexID] + vec4(offset, 0.0, 0.0);              \n"+
    	    "    tex_coord = vertices[gl_VertexID].xy * 3.0 + vec2(0.45 * 3);                    \n"+
    	    "}                                                                              \n";

    private final String fs_source =
    	    "#version 410 core                                                              \n"+
    	    "                                                                               \n"+
    	    "uniform sampler2D s;                                                           \n"+
    	    "                                                                               \n"+
    	    "out vec4 color;                                                                \n"+
    	    "                                                                               \n"+
    	    "in vec2 tex_coord;                                                             \n"+
    	    "                                                                               \n"+
    	    "void main(void)                                                                \n"+
    	    "{                                                                              \n"+
    	    "    color = texture(s, tex_coord);                                             \n"+
    	    "}                                                                              \n";


	public WrapModes() {
		super("OpenGL SuperBible - Texture Wrap Modes");
	}

	
	
	@Override
	protected void startup() throws Throwable {
        // Generate a name for the texture
		texture = glGenTextures();

        // Load texture from file
        KTX.load(getMediaPath() + "textures/rightarrows.ktx", texture);

        // Now bind it to the context using the GL_TEXTURE_2D binding point
        glBindTexture(GL_TEXTURE_2D, texture);

        program = glCreateProgram();
        int fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source);
        glCompileShader(fs);
        
        Shader.checkCompilerResult(fs);

        int vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);

        Shader.checkCompilerResult(vs);

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);

		// determine uniform attribute index of glsl variable 'color'
		uniform_offset = glGetUniformLocation(program, "offset");
		if (uniform_offset == -1) fatal("uniform attribute 'offset' not found");

        vao = glGenVertexArrays();
        glBindVertexArray(vao);
 
	}


	@Override
	protected void render(double currentTime) throws Throwable {
        final float yellow[] = { 0.4f, 0.4f, 0.0f, 1.0f };
        FloatBuffer yellow_fb = BufferUtilsHelper.createFloatBuffer(yellow);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.1f, 0.0f, 1.0f);

        final int wrapmodes[] = { GL_CLAMP_TO_EDGE, GL_REPEAT, GL_CLAMP_TO_BORDER, GL_MIRRORED_REPEAT };
        final float offsets[][] = { {-0.5f, -0.5f},
        							{0.5f, -0.5f},
        							{-0.5f,  0.5f},
        							{ 0.5f,  0.5f} };

        glUseProgram(program);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glTexParameter(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, yellow_fb);

        /**
         * creating four rectangles at different offsets (upper left, upper right, lower left, lower right)
         * and with one of the four different wrap modes applied.
        */
        for (int i = 0; i < 4; i++)
        {
        	FloatBuffer fb = BufferUtilsHelper.createFloatBuffer(offsets[i]);
            glUniform2(uniform_offset, fb);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapmodes[i]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapmodes[i]);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }

  	}

	public static void main(String[] args) {
		WrapModes app = new WrapModes();
		app.run();
	}



	@Override
	protected void shutdown() throws Throwable {
        glDeleteProgram(program);
        glDeleteVertexArrays(vao);
        glDeleteTextures(texture);
	}



	@Override
	protected void onKey(int key, int action) {
        if (action != 0)
        {
            switch (key)
            {
            }
        }

	}

}
