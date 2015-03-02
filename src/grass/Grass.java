package grass;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

import static sb6.vmath.MathHelper.*;
import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;


/**
 * Java implementation of indexedcube.cpp
 * 
 * @author homac
 *
 */

public class Grass extends Application {
    int      grass_buffer;
    int      grass_vao;

    int      grass_program;

    int      tex_grass_color;
    int      tex_grass_length;
    int      tex_grass_orientation;
    int      tex_grass_bend;

    class Uniforms
    {
        int   mvpMatrix;
    }
    Uniforms uniforms = new Uniforms();


	static final String grass_vs_source =
	    "// Vertex Shader                                                                                            \n"+
	    "// Graham Sellers                                                                                           \n"+
	    "// OpenGL SuperBible                                                                                        \n"+
	    "#version 420 core                                                                                           \n"+
	    "                                                                                                            \n"+
	    "// Incoming per vertex position                                                                             \n"+
	    "in vec4 vVertex;                                                                                            \n"+
	    "                                                                                                            \n"+
	    "// Output varyings                                                                                          \n"+
	    "out vec4 color;                                                                                             \n"+
	    "                                                                                                            \n"+
	    "uniform mat4 mvpMatrix;                                                                                     \n"+
	    "                                                                                                            \n"+
	    "layout (binding = 0) uniform sampler1D grasspalette_texture;                                                \n"+
	    "layout (binding = 1) uniform sampler2D length_texture;                                                      \n"+
	    "layout (binding = 2) uniform sampler2D orientation_texture;                                                 \n"+
	    "layout (binding = 3) uniform sampler2D grasscolor_texture;                                                  \n"+
	    "layout (binding = 4) uniform sampler2D bend_texture;                                                        \n"+
	    "                                                                                                            \n"+
	    "int random(int seed, int iterations)                                                                        \n"+
	    "{                                                                                                           \n"+
	    "    int value = seed;                                                                                       \n"+
	    "    int n;                                                                                                  \n"+
	    "                                                                                                            \n"+
	    "    for (n = 0; n < iterations; n++) {                                                                      \n"+
	    "        value = ((value >> 7) ^ (value << 9)) * 15485863;                                                   \n"+
	    "    }                                                                                                       \n"+
	    "                                                                                                            \n"+
	    "    return value;                                                                                           \n"+
	    "}                                                                                                           \n"+
	    "                                                                                                            \n"+
	    "vec4 random_vector(int seed)                                                                                \n"+
	    "{                                                                                                           \n"+
	    "    int r = random(gl_InstanceID, 4);                                                                       \n"+
	    "    int g = random(r, 2);                                                                                   \n"+
	    "    int b = random(g, 2);                                                                                   \n"+
	    "    int a = random(b, 2);                                                                                   \n"+
	    "                                                                                                            \n"+
	    "    return vec4(float(r & 0x3FF) / 1024.0,                                                                  \n"+
	    "                float(g & 0x3FF) / 1024.0,                                                                  \n"+
	    "                float(b & 0x3FF) / 1024.0,                                                                  \n"+
	    "                float(a & 0x3FF) / 1024.0);                                                                 \n"+
	    "}                                                                                                           \n"+
	    "                                                                                                            \n"+
	    "mat4 construct_rotation_matrix(float angle)                                                                 \n"+
	    "{                                                                                                           \n"+
	    "    float st = sin(angle);                                                                                  \n"+
	    "    float ct = cos(angle);                                                                                  \n"+
	    "                                                                                                            \n"+
	    "    return mat4(vec4(ct, 0.0, st, 0.0),                                                                     \n"+
	    "                vec4(0.0, 1.0, 0.0, 0.0),                                                                   \n"+
	    "                vec4(-st, 0.0, ct, 0.0),                                                                    \n"+
	    "                vec4(0.0, 0.0, 0.0, 1.0));                                                                  \n"+
	    "}                                                                                                           \n"+
	    "                                                                                                            \n"+
	    "void main(void)                                                                                             \n"+
	    "{                                                                                                           \n"+
	    "    vec4 offset = vec4(float(gl_InstanceID >> 10) - 512.0,                                                  \n"+
	    "                       0.0f,                                                                                \n"+
	    "                       float(gl_InstanceID & 0x3FF) - 512.0,                                                \n"+
	    "                       0.0f);                                                                               \n"+
	    "    int number1 = random(gl_InstanceID, 3);                                                                 \n"+
	    "    int number2 = random(number1, 2);                                                                       \n"+
	    "    offset += vec4(float(number1 & 0xFF) / 256.0,                                                           \n"+
	    "                   0.0f,                                                                                    \n"+
	    "                   float(number2 & 0xFF) / 256.0,                                                           \n"+
	    "                   0.0f);                                                                                   \n"+
	    "    // float angle = float(random(number2, 2) & 0x3FF) / 1024.0;                                            \n"+
	    "                                                                                                            \n"+
	    "    vec2 texcoord = offset.xz / 1024.0 + vec2(0.5);                                                         \n"+
	    "                                                                                                            \n"+
	    "    // float bend_factor = float(random(number2, 7) & 0x3FF) / 1024.0;                                      \n"+
	    "    float bend_factor = texture(bend_texture, texcoord).r * 2.0;                                            \n"+
	    "    float bend_amount = cos(vVertex.y);                                                                     \n"+
	    "                                                                                                            \n"+
	    "    float angle = texture(orientation_texture, texcoord).r * 2.0 * 3.141592;                                \n"+
	    "    mat4 rot = construct_rotation_matrix(angle);                                                            \n"+
	    "    vec4 position = (rot * (vVertex + vec4(0.0, 0.0, bend_amount * bend_factor, 0.0))) + offset;            \n"+
	    "                                                                                                            \n"+
	    "    position *= vec4(1.0, texture(length_texture, texcoord).r * 0.9 + 0.3, 1.0, 1.0);                       \n"+
	    "                                                                                                            \n"+
	    "    gl_Position = mvpMatrix * position; // (rot * position);                                                \n"+
	    "    // color = vec4(random_vector(gl_InstanceID).xyz * vec3(0.1, 0.5, 0.1) + vec3(0.1, 0.4, 0.1), 1.0);     \n"+
	    "    // color = texture(orientation_texture, texcoord);                                                      \n"+
	    "    color = texture(grasspalette_texture, texture(grasscolor_texture, texcoord).r) +                        \n"+
	    "            vec4(random_vector(gl_InstanceID).xyz * vec3(0.1, 0.5, 0.1), 1.0);                              \n"+
	    "}                                                                                                           \n";

	static final String grass_fs_source =
	    "// Fragment Shader               \n"+
	    "// Graham Sellers                \n"+
	    "// OpenGL SuperBible             \n"+
	    "#version 420 core                \n"+
	    "                                 \n"+
	    "in vec4 color;                   \n"+
	    "                                 \n"+
	    "out vec4 output_color;           \n"+
	    "                                 \n"+
	    "void main(void)                  \n"+
	    "{                                \n"+
	    "    output_color = color;        \n"+
	    "}                                \n";

	public Grass() {
		super("OpenGL SuperBible - Grass");
	}

	@Override
	protected void startup() throws Throwable {
		
        final FloatBuffer grass_blade = BufferUtilsHelper.createFloatBuffer(new float[]
        {
            -0.3f, 0.0f,
             0.3f, 0.0f,
            -0.20f, 1.0f,
             0.1f, 1.3f,
            -0.05f, 2.3f,
             0.0f, 3.3f
        });


        grass_buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, grass_buffer);
        glBufferData(GL_ARRAY_BUFFER, grass_blade, GL_STATIC_DRAW);

        grass_vao = glGenVertexArrays();
        glBindVertexArray(grass_vao);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, MemoryUtil.NULL);
        glEnableVertexAttribArray(0);

        grass_program = glCreateProgram();
        int grass_vs = glCreateShader(GL_VERTEX_SHADER);
        int grass_fs = glCreateShader(GL_FRAGMENT_SHADER);

        glShaderSource(grass_vs, grass_vs_source);
        glShaderSource(grass_fs, grass_fs_source);

        glCompileShader(grass_vs);
        Shader.checkCompilerResult(grass_vs);
        glCompileShader(grass_fs);
        Shader.checkCompilerResult(grass_fs);

        glAttachShader(grass_program, grass_vs);
        glAttachShader(grass_program, grass_fs);

        glLinkProgram(grass_program);
        Shader.checkLinkerResult(grass_program);
        glDeleteShader(grass_fs);
        glDeleteShader(grass_vs);

        uniforms.mvpMatrix = glGetUniformLocation(grass_program, "mvpMatrix");

        glActiveTexture(GL_TEXTURE1);
        tex_grass_length = KTX.load(getMediaPath() + "textures/grass_length.ktx");
        glActiveTexture(GL_TEXTURE2);
        tex_grass_orientation = KTX.load(getMediaPath() + "textures/grass_orientation.ktx");
        glActiveTexture(GL_TEXTURE3);
        tex_grass_color = KTX.load(getMediaPath() + "textures/grass_color.ktx");
        glActiveTexture(GL_TEXTURE4);
        tex_grass_bend = KTX.load(getMediaPath() + "textures/grass_bend.ktx");

	}

	@Override
	protected void shutdown() {
		glDeleteProgram(grass_program);
	}
	
	@Override
	protected void render(double currentTime) {
		currentTime = 1.0;
        float t = (float)currentTime * 0.02f;
        float r = 550.0f;

        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 1.0f); // black
        glClearBuffer1f(GL_DEPTH, 0, 1.0f);

        Matrix4x4f mv_matrix = Matrix4x4f.lookat(new Vector3f(sinf(t) * r, 25.0f, cosf(t) * r),
        		new Vector3f(0.0f, -50.0f, 0.0f),
        		new Vector3f(0.0f, 1.0f, 0.0f));
        
        Matrix4x4f prj_matrix = Matrix4x4f.perspective(45.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);

        glUseProgram(grass_program);
        glUniformMatrix4(uniforms.mvpMatrix, false, (prj_matrix.mul(mv_matrix).toFloatBuffer()));

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glBindVertexArray(grass_vao);
        glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 6, 1024 * 1024);
	}


	public static void main (String[] args) {
		Application app = new Grass();
		app.run();
	}

}
