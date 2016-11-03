package julia;


import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import static sb6.vmath.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;

public class Julia extends Application {
	private static final ByteBuffer palette = BufferUtilsHelper.createByteBuffer(new byte[]
		{
		    (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0xFF, (byte)0x0E, (byte)0x03, (byte)0xFF, (byte)0x1C,
		    (byte)0x07, (byte)0xFF, (byte)0x2A, (byte)0x0A, (byte)0xFF, (byte)0x38, (byte)0x0E, (byte)0xFF,
		    (byte)0x46, (byte)0x12, (byte)0xFF, (byte)0x54, (byte)0x15, (byte)0xFF, (byte)0x62, (byte)0x19,
		    (byte)0xFF, (byte)0x71, (byte)0x1D, (byte)0xFF, (byte)0x7F, (byte)0x20, (byte)0xFF, (byte)0x88,
		    (byte)0x22, (byte)0xFF, (byte)0x92, (byte)0x25, (byte)0xFF, (byte)0x9C, (byte)0x27, (byte)0xFF,
		    (byte)0xA6, (byte)0x2A, (byte)0xFF, (byte)0xB0, (byte)0x2C, (byte)0xFF, (byte)0xBA, (byte)0x2F,
		    (byte)0xFF, (byte)0xC4, (byte)0x31, (byte)0xFF, (byte)0xCE, (byte)0x34, (byte)0xFF, (byte)0xD8,
		    (byte)0x36, (byte)0xFF, (byte)0xE2, (byte)0x39, (byte)0xFF, (byte)0xEC, (byte)0x3B, (byte)0xFF,
		    (byte)0xF6, (byte)0x3E, (byte)0xFF, (byte)0xFF, (byte)0x40, (byte)0xF8, (byte)0xFE, (byte)0x40,
		    (byte)0xF1, (byte)0xFE, (byte)0x40, (byte)0xEA, (byte)0xFE, (byte)0x41, (byte)0xE3, (byte)0xFD,
		    (byte)0x41, (byte)0xDC, (byte)0xFD, (byte)0x41, (byte)0xD6, (byte)0xFD, (byte)0x42, (byte)0xCF,
		    (byte)0xFC, (byte)0x42, (byte)0xC8, (byte)0xFC, (byte)0x42, (byte)0xC1, (byte)0xFC, (byte)0x43,
		    (byte)0xBA, (byte)0xFB, (byte)0x43, (byte)0xB4, (byte)0xFB, (byte)0x43, (byte)0xAD, (byte)0xFB,
		    (byte)0x44, (byte)0xA6, (byte)0xFA, (byte)0x44, (byte)0x9F, (byte)0xFA, (byte)0x45, (byte)0x98,
		    (byte)0xFA, (byte)0x45, (byte)0x92, (byte)0xF9, (byte)0x45, (byte)0x8B, (byte)0xF9, (byte)0x46,
		    (byte)0x84, (byte)0xF9, (byte)0x46, (byte)0x7D, (byte)0xF8, (byte)0x46, (byte)0x76, (byte)0xF8,
		    (byte)0x46, (byte)0x6F, (byte)0xF8, (byte)0x47, (byte)0x68, (byte)0xF8, (byte)0x47, (byte)0x61,
		    (byte)0xF7, (byte)0x47, (byte)0x5A, (byte)0xF7, (byte)0x48, (byte)0x53, (byte)0xF7, (byte)0x48,
		    (byte)0x4C, (byte)0xF6, (byte)0x48, (byte)0x45, (byte)0xF6, (byte)0x49, (byte)0x3E, (byte)0xF6,
		    (byte)0x49, (byte)0x37, (byte)0xF6, (byte)0x4A, (byte)0x30, (byte)0xF5, (byte)0x4A, (byte)0x29,
		    (byte)0xF5, (byte)0x4A, (byte)0x22, (byte)0xF5, (byte)0x4B, (byte)0x1B, (byte)0xF5, (byte)0x4B,
		    (byte)0x14, (byte)0xF4, (byte)0x4B, (byte)0x0D, (byte)0xF4, (byte)0x4C, (byte)0x06, (byte)0xF4,
		    (byte)0x4D, (byte)0x04, (byte)0xF1, (byte)0x51, (byte)0x0D, (byte)0xE9, (byte)0x55, (byte)0x16,
		    (byte)0xE1, (byte)0x59, (byte)0x1F, (byte)0xD9, (byte)0x5D, (byte)0x28, (byte)0xD1, (byte)0x61,
		    (byte)0x31, (byte)0xC9, (byte)0x65, (byte)0x3A, (byte)0xC1, (byte)0x69, (byte)0x42, (byte)0xB9,
		    (byte)0x6D, (byte)0x4B, (byte)0xB1, (byte)0x71, (byte)0x54, (byte)0xA9, (byte)0x75, (byte)0x5D,
		    (byte)0xA1, (byte)0x79, (byte)0x66, (byte)0x99, (byte)0x7D, (byte)0x6F, (byte)0x91, (byte)0x81,
		    (byte)0x78, (byte)0x89, (byte)0x86, (byte)0x80, (byte)0x81, (byte)0x8A, (byte)0x88, (byte)0x7A,
		    (byte)0x8E, (byte)0x90, (byte)0x72, (byte)0x92, (byte)0x98, (byte)0x6A, (byte)0x96, (byte)0xA1,
		    (byte)0x62, (byte)0x9A, (byte)0xA9, (byte)0x5A, (byte)0x9E, (byte)0xB1, (byte)0x52, (byte)0xA2,
		    (byte)0xBA, (byte)0x4A, (byte)0xA6, (byte)0xC2, (byte)0x42, (byte)0xAA, (byte)0xCA, (byte)0x3A,
		    (byte)0xAE, (byte)0xD3, (byte)0x32, (byte)0xB2, (byte)0xDB, (byte)0x2A, (byte)0xB6, (byte)0xE3,
		    (byte)0x22, (byte)0xBA, (byte)0xEB, (byte)0x1A, (byte)0xBE, (byte)0xF4, (byte)0x12, (byte)0xC2,
		    (byte)0xFC, (byte)0x0A, (byte)0xC6, (byte)0xF5, (byte)0x02, (byte)0xCA, (byte)0xE6, (byte)0x09,
		    (byte)0xCE, (byte)0xD8, (byte)0x18, (byte)0xD1, (byte)0xCA, (byte)0x27, (byte)0xD5, (byte)0xBB,
		    (byte)0x36, (byte)0xD8, (byte)0xAD, (byte)0x45, (byte)0xDC, (byte)0x9E, (byte)0x54, (byte)0xE0,
		    (byte)0x90, (byte)0x62, (byte)0xE3, (byte)0x82, (byte)0x6F, (byte)0xE6, (byte)0x71, (byte)0x7C,
		    (byte)0xEA, (byte)0x61, (byte)0x89, (byte)0xEE, (byte)0x51, (byte)0x96, (byte)0xF2, (byte)0x40,
		    (byte)0xA3, (byte)0xF5, (byte)0x30, (byte)0xB0, (byte)0xF9, (byte)0x20, (byte)0xBD, (byte)0xFD,
		    (byte)0x0F, (byte)0xE3, (byte)0xFF, (byte)0x01, (byte)0xE9, (byte)0xFF, (byte)0x01, (byte)0xEE,
		    (byte)0xFF, (byte)0x01, (byte)0xF4, (byte)0xFF, (byte)0x00, (byte)0xFA, (byte)0xFF, (byte)0x00,
		    (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0xFF, (byte)0xFF, (byte)0x0A, (byte)0xFF, (byte)0xFF,
		    (byte)0x15, (byte)0xFF, (byte)0xFF, (byte)0x20, (byte)0xFF, (byte)0xFF, (byte)0x2B, (byte)0xFF,
		    (byte)0xFF, (byte)0x36, (byte)0xFF, (byte)0xFF, (byte)0x41, (byte)0xFF, (byte)0xFF, (byte)0x4C,
		    (byte)0xFF, (byte)0xFF, (byte)0x57, (byte)0xFF, (byte)0xFF, (byte)0x62, (byte)0xFF, (byte)0xFF,
		    (byte)0x6D, (byte)0xFF, (byte)0xFF, (byte)0x78, (byte)0xFF, (byte)0xFF, (byte)0x81, (byte)0xFF,
		    (byte)0xFF, (byte)0x8A, (byte)0xFF, (byte)0xFF, (byte)0x92, (byte)0xFF, (byte)0xFF, (byte)0x9A,
		    (byte)0xFF, (byte)0xFF, (byte)0xA3, (byte)0xFF, (byte)0xFF, (byte)0xAB, (byte)0xFF, (byte)0xFF,
		    (byte)0xB4, (byte)0xFF, (byte)0xFF, (byte)0xBC, (byte)0xFF, (byte)0xFF, (byte)0xC4, (byte)0xFF,
		    (byte)0xFF, (byte)0xCD, (byte)0xFF, (byte)0xFF, (byte)0xD5, (byte)0xFF, (byte)0xFF, (byte)0xDD,
		    (byte)0xFF, (byte)0xFF, (byte)0xE6, (byte)0xFF, (byte)0xFF, (byte)0xEE, (byte)0xFF, (byte)0xFF,
		    (byte)0xF7, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xF9,
		    (byte)0xFF, (byte)0xFF, (byte)0xF3, (byte)0xFF, (byte)0xFF, (byte)0xED, (byte)0xFF, (byte)0xFF,
		    (byte)0xE7, (byte)0xFF, (byte)0xFF, (byte)0xE1, (byte)0xFF, (byte)0xFF, (byte)0xDB, (byte)0xFF,
		    (byte)0xFF, (byte)0xD5, (byte)0xFF, (byte)0xFF, (byte)0xCF, (byte)0xFF, (byte)0xFF, (byte)0xCA,
		    (byte)0xFF, (byte)0xFF, (byte)0xC4, (byte)0xFF, (byte)0xFF, (byte)0xBE, (byte)0xFF, (byte)0xFF,
		    (byte)0xB8, (byte)0xFF, (byte)0xFF, (byte)0xB2, (byte)0xFF, (byte)0xFF, (byte)0xAC, (byte)0xFF,
		    (byte)0xFF, (byte)0xA6, (byte)0xFF, (byte)0xFF, (byte)0xA0, (byte)0xFF, (byte)0xFF, (byte)0x9B,
		    (byte)0xFF, (byte)0xFF, (byte)0x96, (byte)0xFF, (byte)0xFF, (byte)0x90, (byte)0xFF, (byte)0xFF,
		    (byte)0x8B, (byte)0xFF, (byte)0xFF, (byte)0x86, (byte)0xFF, (byte)0xFF, (byte)0x81, (byte)0xFF,
		    (byte)0xFF, (byte)0x7B, (byte)0xFF, (byte)0xFF, (byte)0x76, (byte)0xFF, (byte)0xFF, (byte)0x71,
		    (byte)0xFF, (byte)0xFF, (byte)0x6B, (byte)0xFF, (byte)0xFF, (byte)0x66, (byte)0xFF, (byte)0xFF,
		    (byte)0x61, (byte)0xFF, (byte)0xFF, (byte)0x5C, (byte)0xFF, (byte)0xFF, (byte)0x56, (byte)0xFF,
		    (byte)0xFF, (byte)0x51, (byte)0xFF, (byte)0xFF, (byte)0x4C, (byte)0xFF, (byte)0xFF, (byte)0x47,
		    (byte)0xFF, (byte)0xFF, (byte)0x41, (byte)0xF9, (byte)0xFF, (byte)0x40, (byte)0xF0, (byte)0xFF,
		    (byte)0x40, (byte)0xE8, (byte)0xFF, (byte)0x40, (byte)0xDF, (byte)0xFF, (byte)0x40, (byte)0xD7,
		    (byte)0xFF, (byte)0x40, (byte)0xCF, (byte)0xFF, (byte)0x40, (byte)0xC6, (byte)0xFF, (byte)0x40,
		    (byte)0xBE, (byte)0xFF, (byte)0x40, (byte)0xB5, (byte)0xFF, (byte)0x40, (byte)0xAD, (byte)0xFF,
		    (byte)0x40, (byte)0xA4, (byte)0xFF, (byte)0x40, (byte)0x9C, (byte)0xFF, (byte)0x40, (byte)0x95,
		    (byte)0xFF, (byte)0x40, (byte)0x8D, (byte)0xFF, (byte)0x40, (byte)0x86, (byte)0xFF, (byte)0x40,
		    (byte)0x7E, (byte)0xFF, (byte)0x40, (byte)0x77, (byte)0xFF, (byte)0x40, (byte)0x6F, (byte)0xFF,
		    (byte)0x40, (byte)0x68, (byte)0xFF, (byte)0x40, (byte)0x60, (byte)0xFF, (byte)0x40, (byte)0x59,
		    (byte)0xFF, (byte)0x40, (byte)0x51, (byte)0xFF, (byte)0x40, (byte)0x4A, (byte)0xFA, (byte)0x43,
		    (byte)0x42, (byte)0xF3, (byte)0x48, (byte)0x3E, (byte)0xED, (byte)0x4E, (byte)0x3D, (byte)0xE6,
		    (byte)0x53, (byte)0x3B, (byte)0xDF, (byte)0x58, (byte)0x39, (byte)0xD8, (byte)0x5E, (byte)0x37,
		    (byte)0xD2, (byte)0x63, (byte)0x35, (byte)0xCB, (byte)0x68, (byte)0x34, (byte)0xC4, (byte)0x6D,
		    (byte)0x32, (byte)0xBD, (byte)0x73, (byte)0x30, (byte)0xB7, (byte)0x78, (byte)0x2E, (byte)0xB0,
		    (byte)0x7D, (byte)0x2D, (byte)0xA9, (byte)0x83, (byte)0x2B, (byte)0xA2, (byte)0x88, (byte)0x29,
		    (byte)0x9C, (byte)0x8D, (byte)0x27, (byte)0x95, (byte)0x92, (byte)0x25, (byte)0x8E, (byte)0x98,
		    (byte)0x24, (byte)0x87, (byte)0x9D, (byte)0x22, (byte)0x81, (byte)0xA2, (byte)0x20, (byte)0x7A,
		    (byte)0xA6, (byte)0x1E, (byte)0x74, (byte)0xAB, (byte)0x1D, (byte)0x6E, (byte)0xB0, (byte)0x1B,
		    (byte)0x68, (byte)0xB5, (byte)0x1A, (byte)0x61, (byte)0xB9, (byte)0x18, (byte)0x5B, (byte)0xBE,
		    (byte)0x17, (byte)0x55, (byte)0xC3, (byte)0x15, (byte)0x4F, (byte)0xC8, (byte)0x13, (byte)0x48,
		    (byte)0xCD, (byte)0x12, (byte)0x42, (byte)0xD1, (byte)0x10, (byte)0x3C, (byte)0xD6, (byte)0x0F,
		    (byte)0x36, (byte)0xDB, (byte)0x0D, (byte)0x2F, (byte)0xE0, (byte)0x0C, (byte)0x29, (byte)0xE4,
		    (byte)0x0A, (byte)0x23, (byte)0xE9, (byte)0x08, (byte)0x1D, (byte)0xEE, (byte)0x07, (byte)0x16,
		    (byte)0xF3, (byte)0x05, (byte)0x10, (byte)0xF7, (byte)0x04, (byte)0x0A, (byte)0xFC, (byte)0x02,
		    (byte)0x04, (byte)0xFB, (byte)0x01, (byte)0x04, (byte)0xEF, (byte)0x00, (byte)0x12, (byte)0xE4,
		    (byte)0x00, (byte)0x1F, (byte)0xD9, (byte)0x00, (byte)0x2D, (byte)0xCE, (byte)0x00, (byte)0x3B,
		    (byte)0xC2, (byte)0x00, (byte)0x48, (byte)0xB7, (byte)0x00, (byte)0x56, (byte)0xAC, (byte)0x00,
		    (byte)0x64, (byte)0xA0, (byte)0x00, (byte)0x72, (byte)0x95, (byte)0x00, (byte)0x7F, (byte)0x8A,
		    (byte)0x00, (byte)0x88, (byte)0x7F, (byte)0x00, (byte)0x92, (byte)0x75, (byte)0x00, (byte)0x9C,
		    (byte)0x6B, (byte)0x00, (byte)0xA6, (byte)0x61, (byte)0x00, (byte)0xB0, (byte)0x57, (byte)0x00,
		    (byte)0xBA, (byte)0x4E, (byte)0x00, (byte)0xC4, (byte)0x44, (byte)0x00, (byte)0xCE, (byte)0x3A,
		    (byte)0x00, (byte)0xD8, (byte)0x30, (byte)0x00, (byte)0xE2, (byte)0x27, (byte)0x00, (byte)0xEC,
		    (byte)0x1D, (byte)0x00, (byte)0xF6, (byte)0x13, (byte)0x00, (byte)0xFF, (byte)0x09, (byte)0x00,
		});

    private int      program = 0;
    private int      vao = 0;
    private int      palette_texture = 0;

    class Uniforms
    {
        int   zoom;
        int   offset;
        int   C;
    } 
    Uniforms uniforms = new Uniforms();

    private boolean paused;
    private float time_offset = 0f;
    private float zoom = 1.0f;
    private float x_offset = 0f;
    private float y_offset = 0f;
    
    private float t = 0.0f;


	public Julia() {
		super("OpenGL SuperBible - Julia Fractal");
	}

	

    protected void startup()
    {
    	vao  = glGenVertexArrays();
        glBindVertexArray(vao);

        String vs_source =
            "// Julia set renderer - Vertex Shader                                                  \n" +
            "// Graham Sellers                                                                      \n" +
            "// OpenGL SuperBible                                                                   \n" +
            "#version 150 core                                                                      \n" +
            "                                                                                       \n" +
            "// Zoom factor                                                                         \n" +
            "uniform float zoom;                                                                    \n" +
            "                                                                                       \n" +
            "// Offset vector                                                                       \n" +
            "uniform vec2 offset;                                                                   \n" +
            "                                                                                       \n" +
            "out vec2 initial_z;                                                                    \n" +
            "                                                                                       \n" +
            "void main(void)                                                                        \n" +
            "{                                                                                      \n" +
            "    const vec4 vertices[4] = vec4[4](vec4(-1.0, -1.0, 0.5, 1.0),                       \n" +
            "                                     vec4( 1.0, -1.0, 0.5, 1.0),                       \n" +
            "                                     vec4( 1.0,  1.0, 0.5, 1.0),                       \n" +
            "                                     vec4(-1.0,  1.0, 0.5, 1.0));                      \n" +
            "    initial_z = (vertices[gl_VertexID].xy * zoom) + offset;                            \n" +
            "    gl_Position = vertices[gl_VertexID];                                               \n" +
            "}                                                                                      \n"
        ;

        String fs_source =
            "// Julia set renderer - Fragment Shader                                                \n" +
            "// Graham Sellers                                                                      \n" +
            "// OpenGL SuperBible                                                                   \n" +
            "#version 150 core                                                                      \n" +
            "                                                                                       \n" +
            "in vec2 initial_z;                                                                     \n" +
            "                                                                                       \n" +
            "out vec4 color;                                                                        \n" +
            "                                                                                       \n" +
            "uniform sampler1D tex_gradient;                                                        \n" +
            "uniform vec2 C;                                                                        \n" +
            "                                                                                       \n" +
            "void main(void)                                                                        \n" +
            "{                                                                                      \n" +
            "    vec2 Z = initial_z;                                                                \n" +
            "    int iterations = 0;                                                                \n" +
            "    const float threshold_squared = 16.0;                                              \n" +
            "    const int max_iterations = 256;                                                    \n" +
            "    while (iterations < max_iterations && dot(Z, Z) < threshold_squared) {             \n" +
            "        vec2 Z_squared;                                                                \n" +
            "        Z_squared.x = Z.x * Z.x - Z.y * Z.y;                                           \n" +
            "        Z_squared.y = 2.0 * Z.x * Z.y;                                                 \n" +
            "        Z = Z_squared + C;                                                             \n" +
            "        iterations++;                                                                  \n" +
            "    }                                                                                  \n" +
            "    if (iterations == max_iterations)                                                  \n" +
            "        color = vec4(0.0, 0.0, 0.0, 1.0);                                              \n" +
            "    else                                                                               \n" +
            "        color = texture(tex_gradient, float(iterations) / float(max_iterations));      \n" +
            "}                                                                                      \n"
        ;

        int vs = Shader.compile(GL_VERTEX_SHADER, vs_source);
        int fs = Shader.compile(GL_FRAGMENT_SHADER, fs_source);
        
        program = Program.link(true, vs, fs);

        uniforms.zoom   = glGetUniformLocation(program, "zoom");
        uniforms.offset = glGetUniformLocation(program, "offset");
        uniforms.C      = glGetUniformLocation(program, "C");

        glDeleteShader(vs);
        glDeleteShader(fs);

        palette_texture = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, palette_texture);
        glTexStorage1D(GL_TEXTURE_1D, 8, GL_RGB8, 256);
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, 256, GL_RGB, GL_UNSIGNED_BYTE, palette);
        glGenerateMipmap(GL_TEXTURE_1D);
    }

    protected void render(double currentTime)
    {
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);

        float r = 0.0f;

        if (!paused)
            t = (float)currentTime;

        r = t + time_offset;

        float[] C = new float[]{ (sinf(r * 0.1f) + cosf(r * 0.23f)) * 0.5f, (cosf(r * 0.13f) + sinf(r * 0.21f)) * 0.5f };
        float[] offset = new float[]{ x_offset, y_offset };

        glUseProgram(program);

        glUniform2f(uniforms.C, C[0], C[1]);
        glUniform2f(uniforms.offset, offset[0], offset[1]);
        glUniform1f(uniforms.zoom, zoom);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    protected void onKey(int key, int action)
    {
        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT)
        {
            switch (key)
            {
                case 'P': paused = !paused;
                    break;
                case GLFW.GLFW_KEY_KP_ADD: time_offset -= 0.00001f;
                    break;
                case GLFW.GLFW_KEY_KP_SUBTRACT: time_offset += 0.00001f;
                    break;
                case GLFW.GLFW_KEY_KP_9: time_offset += 0.0001f;
                    break;
                case GLFW.GLFW_KEY_KP_3: time_offset -= 0.0001f;
                    break;
                case GLFW.GLFW_KEY_KP_8: time_offset += 0.01f;
                    break;
                case GLFW.GLFW_KEY_KP_2: time_offset -= 0.01f;
                    break;
                case GLFW.GLFW_KEY_KP_7: time_offset += 1.0f;
                    break;
                case GLFW.GLFW_KEY_KP_1: time_offset -= 1.0f;
                    break;
                case GLFW.GLFW_KEY_KP_MULTIPLY: zoom *= 1.02f;
                    break;
                case GLFW.GLFW_KEY_KP_DIVIDE: zoom /= 1.02f;
                    break;
                case 'S': y_offset -= zoom * 0.02f;
                    break;
                case 'A': x_offset -= zoom * 0.02f;
                    break;
                case 'W': y_offset += zoom * 0.02f;
                    break;
                case 'D': x_offset += zoom * 0.02f;
                    break;
                default:
                    break;
            };
        }
    }

    protected void onResize(int w, int h)
    {
    	glViewport(0, 0, w, h);
    }

    protected void shutdown()
    {
        glDeleteTextures(palette_texture);
        glDeleteVertexArrays(vao);
        glDeleteProgram(program);
    }

	public static void main(String[] args) {
		new Julia().run();
	}

}
