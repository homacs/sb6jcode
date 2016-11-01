package toonshading;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;

import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static sb6.BufferUtilsHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL42.*;

public class ToonShading extends Application {
    private int          render_prog = 0;

    private int          tex_toon;

    class Uniforms
    {
        int       mv_matrix;
        int       proj_matrix;
    }
    private Uniforms uniforms = new Uniforms();

    private SBMObject object = new SBMObject();

	public ToonShading() {
		super("OpenGL SuperBible - Toon Shading");
	}

    protected void startup() throws IOException
    {
        ByteBuffer toon_tex_data = createByteBuffer(new byte[]
        {
            (byte)0x44, 0x00, 0x00, 0x00,
            (byte)0x88, 0x00, 0x00, 0x00,
            (byte)0xCC, 0x00, 0x00, 0x00,
            (byte)0xFF, 0x00, 0x00, 0x00
        });

        tex_toon = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, tex_toon);
        glTexStorage1D(GL_TEXTURE_1D, 1, GL_RGB8, sizeof(toon_tex_data) / 4);
        glTexSubImage1D(GL_TEXTURE_1D, 0,
                        0, sizeof(toon_tex_data) / 4,
                        GL_RGBA, GL_UNSIGNED_BYTE,
                        toon_tex_data);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);

        object.load(getMediaPath() + "/objects/torus_nrms_tc.sbm");

        load_shaders();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

        glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        glBindTexture(GL_TEXTURE_1D, tex_toon);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glUseProgram(render_prog);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -3.0f)
                                .mul(Matrix4x4f.rotate((float)currentTime * 13.75f, 0.0f, 1.0f, 0.0f))
                                .mul(Matrix4x4f.rotate((float)currentTime * 7.75f, 0.0f, 0.0f, 1.0f))
                                .mul(Matrix4x4f.rotate((float)currentTime * 15.3f, 1.0f, 0.0f, 0.0f));

        glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());

        object.render();
    }

    protected void shutdown()
    {
        glDeleteProgram(render_prog);
        glDeleteTextures(tex_toon);
    }

    void load_shaders() throws IOException
    {
        if (render_prog != 0)
            glDeleteProgram(render_prog);

        int vs, fs;

        vs = Shader.load(getMediaPath() + "/shaders/toonshading/toonshading.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/toonshading/toonshading.fs.glsl", GL_FRAGMENT_SHADER);

        render_prog = glCreateProgram();
        glAttachShader(render_prog, vs);
        glAttachShader(render_prog, fs);
        glLinkProgram(render_prog);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.mv_matrix = glGetUniformLocation(render_prog, "mv_matrix");
        uniforms.proj_matrix = glGetUniformLocation(render_prog, "proj_matrix");
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action == GLFW.GLFW_PRESS)
        {
            switch (key)
            {
                case 'R': load_shaders();
                    break;
            }
        }
    }
	public static void main(String[] args) {
		new ToonShading().run();
	}

}
