package cubemapenv;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public class CubeMapEnv extends Application {
    private int          render_prog = 0;
    private int          skybox_prog = 0;

    private int          tex_envmap = 0;

    class Uniforms
    {
        class Render
        {
            int       mv_matrix;
            int       proj_matrix;
        } 
        Render render = new Render();
        class Skybox
        {
            int       view_matrix;
        } 
        Skybox skybox = new Skybox();
    } 
    private Uniforms uniforms = new Uniforms();

    SBMObject     object = new SBMObject();

    private int          skybox_vao;

	public CubeMapEnv() {
		super("OpenGL SuperBible - Cubic Environment Map");
	}
	
    protected void startup() throws IOException
    {
    	tex_envmap = KTX.load(getMediaPath() + "/textures/envmaps/mountaincube.ktx");

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

        object.load(getMediaPath() + "/objects/dragon.sbm");

        load_shaders();

        skybox_vao = glGenVertexArrays();
        glBindVertexArray(skybox_vao);

        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
        float t = (float)currentTime * 0.1f;

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        Matrix4x4f view_matrix = Matrix4x4f.lookat(new Vector3f(15.0f * MathHelper.sinf(t), 0.0f, 15.0f * MathHelper.cosf(t)),
                                                new Vector3f(0.0f, 0.0f, 0.0f),
                                                new Vector3f(0.0f, 1.0f, 0.0f));
        Matrix4x4f mv_matrix = new Matrix4x4f(view_matrix)
        		.mul(Matrix4x4f.rotate(t, 1.0f, 0.0f, 0.0f))
        		.mul(Matrix4x4f.rotate(t * 130.1f, 0.0f, 1.0f, 0.0f))
        		.mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));

        glClearBuffer4f(GL_COLOR, 0, 0.2f, 0.2f, 0.2f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);
        glBindTexture(GL_TEXTURE_CUBE_MAP, tex_envmap);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glUseProgram(skybox_prog);
        glBindVertexArray(skybox_vao);

        glUniformMatrix4(uniforms.skybox.view_matrix, false, view_matrix.toFloatBuffer());

        glDisable(GL_DEPTH_TEST);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glUseProgram(render_prog);

        glUniformMatrix4(uniforms.render.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.render.proj_matrix, false, proj_matrix.toFloatBuffer());

        glEnable(GL_DEPTH_TEST);

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

        vs = Shader.load(getMediaPath() + "/shaders/cubemapenv/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/cubemapenv/render.fs.glsl", GL_FRAGMENT_SHADER);

        render_prog = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.render.mv_matrix = glGetUniformLocation(render_prog, "mv_matrix");
        uniforms.render.proj_matrix = glGetUniformLocation(render_prog, "proj_matrix");

        vs = Shader.load(getMediaPath() + "/shaders/cubemapenv/skybox.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/cubemapenv/skybox.fs.glsl", GL_FRAGMENT_SHADER);

        skybox_prog = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.skybox.view_matrix = glGetUniformLocation(skybox_prog, "view_matrix");
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
		new CubeMapEnv().run();
	}

}
