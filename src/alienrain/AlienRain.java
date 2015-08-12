package alienrain;


import java.nio.ByteBuffer;
import java.util.Random;






import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;


public class AlienRain extends Application {

	private Random random = new Random();
	private int render_prog;
	private int render_vao;

	private int tex_alien_array;
	private int rain_buffer;

    float           droplet_x_offset[] = new float[256];
    float           droplet_rot_speed[] = new float[256];
    float           droplet_fall_speed[] = new float[256];



	public AlienRain() {
		super("OpenGL SuperBible - Alien Rain");
	}

	
	
	@Override
	protected void startup() throws Throwable {
        int  vs, fs;

        final String vs_source =
            "#version 410 core                                                      \n"+
            "                                                                       \n"+
            "layout (location = 0) in int alien_index;                              \n"+
            "                                                                       \n"+
            "out VS_OUT                                                             \n"+
            "{                                                                      \n"+
            "    flat int alien;                                                    \n"+
            "    vec2 tc;                                                           \n"+
            "} vs_out;                                                              \n"+
            "                                                                       \n"+
            "struct droplet_t                                                       \n"+
            "{                                                                      \n"+
            "    float x_offset;                                                    \n"+
            "    float y_offset;                                                    \n"+
            "    float orientation;                                                 \n"+
            "    float unused;                                                      \n"+
            "};                                                                     \n"+
            "                                                                       \n"+
            "layout (std140) uniform droplets                                       \n"+
            "{                                                                      \n"+
            "    droplet_t droplet[256];                                            \n"+
            "};                                                                     \n"+
            "                                                                       \n"+
            "void main(void)                                                        \n"+
            "{                                                                      \n"+
            "    const vec2[4] position = vec2[4](vec2(-0.5, -0.5),                 \n"+
            "                                     vec2( 0.5, -0.5),                 \n"+
            "                                     vec2(-0.5,  0.5),                 \n"+
            "                                     vec2( 0.5,  0.5));                \n"+
            "    vs_out.tc = position[gl_VertexID].xy + vec2(0.5);                  \n"+
            "    float co = cos(droplet[alien_index].orientation);                  \n"+
            "    float so = sin(droplet[alien_index].orientation);                  \n"+
            "    mat2 rot = mat2(vec2(co, so),                                      \n"+
            "                    vec2(-so, co));                                    \n"+
            "    vec2 pos = 0.25 * rot * position[gl_VertexID];                     \n"+
            "    gl_Position = vec4(pos.x + droplet[alien_index].x_offset,          \n"+
            "                       pos.y + droplet[alien_index].y_offset,          \n"+
            "                       0.5, 1.0);                                      \n"+
            "    vs_out.alien = alien_index % 64;                                   \n"+
            "}                                                                      \n";

        final String fs_source =
            "#version 410 core                                                      \n"+
            "                                                                       \n"+
            "layout (location = 0) out vec4 color;                                  \n"+
            "                                                                       \n"+
            "in VS_OUT                                                              \n"+
            "{                                                                      \n"+
            "    flat int alien;                                                    \n"+
            "    vec2 tc;                                                           \n"+
            "} fs_in;                                                               \n"+
            "                                                                       \n"+
            "uniform sampler2DArray tex_aliens;                                     \n"+
            "                                                                       \n"+
            "void main(void)                                                        \n"+
            "{                                                                      \n"+
            "    color = texture(tex_aliens, vec3(fs_in.tc, float(fs_in.alien)));   \n"+
            "}                                                                      \n";


        vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);
        
        Shader.checkCompilerResult(vs);

        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source);
        glCompileShader(fs);

        Shader.checkCompilerResult(fs);

        render_prog = glCreateProgram();
        glAttachShader(render_prog, vs);
        glAttachShader(render_prog, fs);
        glLinkProgram(render_prog);

        Program.checkLinkerResult(render_prog);
        
        glDeleteShader(vs);
        glDeleteShader(fs);

        render_vao = glGenVertexArrays();
        glBindVertexArray(render_vao);

        tex_alien_array = KTX.load(getMediaPath() + "/textures/aliens.ktx");
        glBindTexture(GL_TEXTURE_2D_ARRAY, tex_alien_array);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

        rain_buffer = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, rain_buffer);
        glBufferData(GL_UNIFORM_BUFFER, 256 * 4*BufferUtilsHelper.SIZEOF_FLOAT, GL_DYNAMIC_DRAW);

        for (int i = 0; i < 256; i++)
        {
            droplet_x_offset[i] = random.nextFloat() * 2.0f - 1.0f;
            droplet_rot_speed[i] = (random.nextFloat() + 0.5f) * (((i & 1)!= 0) ? -3.0f : 3.0f);
            droplet_fall_speed[i] = random.nextFloat() + 0.2f;
        }

        glBindVertexArray(render_vao);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  
	}


	@Override
	protected void render(double currentTime) throws Throwable {
        float t = (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);

        glUseProgram(render_prog);

        glBindBufferBase(GL_UNIFORM_BUFFER, 0, rain_buffer);
        ByteBuffer ptr = glMapBufferRange(GL_UNIFORM_BUFFER,  0, 256 * BufferUtilsHelper.SIZEOF_FLOAT*4, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        for (int i = 0; i < 256; i++)
        {
            ptr.putFloat(droplet_x_offset[i]);
            ptr.putFloat(2.0f - MathHelper.fmodf((t + (float)i) * droplet_fall_speed[i], 4.31f));
            ptr.putFloat(t * droplet_rot_speed[i]);
            ptr.putFloat(0.0f);
        }
        glUnmapBuffer(GL_UNIFORM_BUFFER);

        int alien_index;
        for (alien_index = 0; alien_index < 256; alien_index++)
        {
            glVertexAttribI1i(0, alien_index);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
  	}

	public static void main(String[] args) {
		AlienRain app = new AlienRain();
		app.run();
	}



	@Override
	protected void shutdown() throws Throwable {
	}

}
