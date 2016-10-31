package equirectangular;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class Equirectangular extends Application {
    private int          render_prog;
    private int          tex_envmap;

    class Uniforms
    {
        int       mv_matrix;
        int       proj_matrix;
    } 
    Uniforms uniforms = new Uniforms();

    SBMObject     object = new SBMObject();

	public Equirectangular() {
		super("OpenGL SuperBible - Equirectangular Environment Map");
	}
	
    protected void startup() throws IOException
    {
    	tex_envmap = KTX.load(getMediaPath() + "/textures/envmaps/equirectangularmap1.ktx");
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        object.load(getMediaPath() + "/objects/dragon.sbm");

        load_shaders();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

        glClearBuffer4f(GL_COLOR, 0, 0.2f, 0.2f, 0.2f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);
        glBindTexture(GL_TEXTURE_2D, tex_envmap);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glUseProgram(render_prog);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -15.0f)
                                .mul(Matrix4x4f.rotate((float)currentTime, 1.0f, 0.0f, 0.0f))
                                .mul(Matrix4x4f.rotate((float)currentTime * 1.1f, 0.0f, 1.0f, 0.0f))
                                .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));

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

        vs = Shader.load(getMediaPath() + "/shaders/equirectangular/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/equirectangular/render.fs.glsl", GL_FRAGMENT_SHADER);

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
		new Equirectangular().run();
	}

}
