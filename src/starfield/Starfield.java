package starfield;


import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public class Starfield extends Application {

	private static final int NUM_STARS           = 2000;
	static final int seed = 0x13371337;
	private static Random rng = new Random(seed);
	private static float random_float() {
		// random number in range [0 .. 1[
	    return rng.nextFloat();
	}
	
    private int          render_prog;
    private int          star_vao;
    private int          star_buffer;

    class Uniforms
    {
        int         time;
        int         proj_matrix;
    } 

    Uniforms uniforms = new Uniforms();

    
    static class Star
    {
        Vector3f     position = new Vector3f();
        Vector3f     color = new Vector3f();
		public static int sizeof() {
			return Vector3f.sizeof() * 2;
		}
		public void write(ByteBuffer stream) {
			position.toByteBuffer(stream);
			color.toByteBuffer(stream);
		}
    };

    
    
	public Starfield() {
		super("OpenGL SuperBible - Starfield");
	}

    protected void startup() throws IOException
    {
        int  vs, fs;

        String fs_source =
            "#version 410 core                                              \n" +
            "                                                               \n" +
            "layout (location = 0) out vec4 color;                          \n" +
            "                                                               \n" +
            "uniform sampler2D tex_star;                                    \n" +
            "flat in vec4 starColor;                                        \n" +
            "                                                               \n" +
            "void main(void)                                                \n" +
            "{                                                              \n" +
            "    color = starColor * texture(tex_star, gl_PointCoord);      \n" +
            "}                                                              \n"
        ;

        String vs_source =
            "#version 410 core                                              \n" +
            "                                                               \n" +
            "layout (location = 0) in vec4 position;                        \n" +
            "layout (location = 1) in vec4 color;                           \n" +
            "                                                               \n" +
            "uniform float time;                                            \n" +
            "uniform mat4 proj_matrix;                                      \n" +
            "                                                               \n" +
            "flat out vec4 starColor;                                       \n" +
            "                                                               \n" +
            "void main(void)                                                \n" +
            "{                                                              \n" +
            "    vec4 newVertex = position;                                 \n" +
            "                                                               \n" +
            "    newVertex.z += time;                                       \n" +
            "    newVertex.z = fract(newVertex.z);                          \n" +
            "                                                               \n" +
            "    // calc size based on distance                             \n" +
            "    float size = (20.0 * newVertex.z * newVertex.z);           \n" +
            "                                                               \n" +
            "    // dim color based on distance (same way as size)          \n" +
            "    starColor = smoothstep(1.0, 7.0, size) * color;            \n" +
            "                                                               \n" +
            "    newVertex.z = (999.9 * newVertex.z) - 1000.0;              \n" +
            "    gl_Position = proj_matrix * newVertex;                     \n" +
            "    gl_PointSize = size;                                       \n" +
            "}                                                              \n"
        ;

        vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);

        render_prog = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.time = glGetUniformLocation(render_prog, "time");
        uniforms.proj_matrix = glGetUniformLocation(render_prog, "proj_matrix");

        // Note: KTX.load creates and binds the texture buffer object 
        // and loads the texture! Since we do not use other textures, 
        // we can leave it bound to be used later during rendering.
        KTX.load(getMediaPath() + "/textures/star.ktx");

        star_vao = glGenVertexArrays();
        glBindVertexArray(star_vao);

        star_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, star_buffer);
        glBufferData(GL_ARRAY_BUFFER, NUM_STARS * Star.sizeof(), GL_STATIC_DRAW);

        ByteBuffer stream = glMapBufferRange(GL_ARRAY_BUFFER, 0, NUM_STARS * Star.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        Star star = new Star();
        int i;

        for (i = 0; i < 1000; i++)
        {
            star.position.set(0, (random_float() * 2.0f - 1.0f) * 100.0f);
            star.position.set(1, (random_float() * 2.0f - 1.0f) * 100.0f);
            star.position.set(2, random_float());
            star.color.set(0, 0.8f + random_float() * 0.2f);
            star.color.set(1, 0.8f + random_float() * 0.2f);
            star.color.set(2, 0.8f + random_float() * 0.2f);
            
            star.write(stream);
        }

        glUnmapBuffer(GL_ARRAY_BUFFER);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, Star.sizeof(), MemoryUtil.NULL);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, Star.sizeof(), Vector3f.sizeof());
        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
    }

    protected void render(double currentTime)
    {
        float t = (float)currentTime;
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f,
                                                     1000.0f);

        t *= 0.1f;
        t -= Math.floor(t);

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        glUseProgram(render_prog);

        glUniform1f(uniforms.time, t);
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());

        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE);

        glBindVertexArray(star_vao);

        // tell OpenGL rasterizer to use the gl_PointSize output provided by our vertex shader.
        glEnable(GL_PROGRAM_POINT_SIZE);
        // run renderer, interpreting vertices as points
        glDrawArrays(GL_POINTS, 0, NUM_STARS);
    }

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		new Starfield().run();
	}


}
