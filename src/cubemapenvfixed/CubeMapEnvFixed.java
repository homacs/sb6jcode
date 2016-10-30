package cubemapenvfixed;


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

/**
 * The example cubemapenv has a bug which makes it basically
 * looking like the environmap used for reflections is rotating
 * with the camera. You can see this, if you remove 5 of the six 
 * sides of the cube map, leaving all other sides black.
 * 
 * This class and accompanying shaders try to fix this. You 
 * can find more information in the shaders for skybox and reflection.
 * 
 * Changes get more apparent when using objects like a cube or a sphere.
 * 
 * @author homac
 *
 */
public class CubeMapEnvFixed extends Application {
    private int          render_prog = 0;
    private int          skybox_prog = 0;

    private int          tex_envmap = 0;

    class Uniforms
    {
        class Render
        {
            int       mv_matrix;
            int       proj_matrix;
			int 	  reverse_matrix;
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
	private boolean pause = false;
	private double last_time = 0f;
	private float t = 0f;

	public CubeMapEnvFixed() {
		super("OpenGL SuperBible (fixed) - Cubic Environment Map");
	}
	
    protected void startup() throws IOException
    {
    	tex_envmap = KTX.load(getMediaPath() + "/textures/envmaps/mountaincube.ktx");

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

//        object.load(getMediaPath() + "/objects/dragon.sbm");
        object.load(getMediaPath() + "/objects/sphere.sbm");
//        object.load(getMediaPath() + "/objects/cube.sbm");

        load_shaders();

        skybox_vao = glGenVertexArrays();
        glBindVertexArray(skybox_vao);

        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {
    	if (!pause){
    		t += (float)(currentTime - last_time) * 1.1f;
    	}
        last_time = currentTime;

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        
        // camera is moving counter-clockwise around the object in a 7 meter radius
        // (remember, positive z-coordinate points towards world observer (not camera))
        float distance = 7f;
        Vector3f eye;
        if (false) {
        	eye = new Vector3f(0f, 0.0f, -distance);
        } else {
        	eye = new Vector3f(distance * MathHelper.sinf(t), 4*MathHelper.sinf(t), distance * MathHelper.cosf(t));
        }
        
        Matrix4x4f view_matrix = Matrix4x4f.lookat(eye,
                                                new Vector3f(0.0f, 0.0f, 0.0f),
                                                new Vector3f(0.0f, 1.0f, 0.0f));
        
        Matrix4x4f reverse_matrix = view_matrix;

        reverse_matrix = Matrix4x4f.inverse(reverse_matrix);
        
        
        // object is steady
        Matrix4x4f mv_matrix = new Matrix4x4f(view_matrix)
//        		.mul(Matrix4x4f.rotate(t*57, 0.0f, 1.0f, 0.0f))
//        		.mul(Matrix4x4f.rotate(t * 130.1f, 0.0f, 1.0f, 0.0f))
        		.mul(Matrix4x4f.translate(0.0f, -0.0f, 0.0f));

        glClearBuffer4f(GL_COLOR, 0, 0.2f, 0.2f, 0.2f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);
        glBindTexture(GL_TEXTURE_CUBE_MAP, tex_envmap);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glUseProgram(skybox_prog);
        glBindVertexArray(skybox_vao);

        glUniformMatrix4(uniforms.skybox.view_matrix, false, reverse_matrix.toFloatBuffer());

        glDisable(GL_DEPTH_TEST);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glUseProgram(render_prog);

        glUniformMatrix4(uniforms.render.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.render.reverse_matrix, false, reverse_matrix.toFloatBuffer());
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

        vs = Shader.load(getMediaPath() + "/shaders/cubemapenvfixed/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/cubemapenvfixed/render.fs.glsl", GL_FRAGMENT_SHADER);

        render_prog = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        uniforms.render.mv_matrix = glGetUniformLocation(render_prog, "mv_matrix");
        uniforms.render.reverse_matrix = glGetUniformLocation(render_prog, "reverse_matrix");
        uniforms.render.proj_matrix = glGetUniformLocation(render_prog, "proj_matrix");

        vs = Shader.load(getMediaPath() + "/shaders/cubemapenvfixed/skybox.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/cubemapenvfixed/skybox.fs.glsl", GL_FRAGMENT_SHADER);

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
            case 'P': pause = !pause ;
            break;
            }
        }
    }

	public static void main(String[] args) {
		new CubeMapEnvFixed().run();
	}

}
