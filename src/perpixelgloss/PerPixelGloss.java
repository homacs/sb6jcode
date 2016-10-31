package perpixelgloss;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

public class PerPixelGloss extends Application {
	private int          render_prog = 0;

	private int          tex_envmap;
	private int          tex_glossmap;

    class Uniforms
    {
        int       mv_matrix;
        int       proj_matrix;
    } 
    Uniforms uniforms = new Uniforms();

    SBMObject     object = new SBMObject();

	public PerPixelGloss() {
		super("OpenGL SuperBible - Per-Pixel Gloss");
	}

    protected void startup() throws IOException
    {
        glActiveTexture(GL_TEXTURE0);
        tex_envmap = KTX.load(getMediaPath() + "/textures/envmaps/mountains3d.ktx");

        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glActiveTexture(GL_TEXTURE1);
        tex_glossmap = KTX.load(getMediaPath() + "/textures/pattern1.ktx");

        object.load(getMediaPath() + "/objects/torus_nrms_tc.sbm");

        load_shaders();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

        glClearBuffer4f(GL_COLOR, 0, 0.1f, 0.1f, 0.1f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_3D, tex_envmap);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, tex_glossmap);

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
        glDeleteTextures(tex_envmap);
    }

    void load_shaders() throws IOException
    {
        if (render_prog != 0)
            glDeleteProgram(render_prog);

        int vs, fs;

        vs = Shader.load(getMediaPath() + "/shaders/perpixelgloss/perpixelgloss.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/perpixelgloss/perpixelgloss.fs.glsl", GL_FRAGMENT_SHADER);

        render_prog = Program.link(true, vs, fs);

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
		new PerPixelGloss().run();
	}

}
