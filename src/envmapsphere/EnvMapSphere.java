package envmapsphere;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;

import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class EnvMapSphere extends Application {
    private int          render_prog;

    private int          tex_envmap;
    private int[]        envmaps = new int[3];
    private int          envmap_index = 0;

    class Uniforms
    {
        int       mv_matrix;
        int       proj_matrix;
    } 
    private Uniforms uniforms = new Uniforms();

    private SBMObject     object = new SBMObject();

	public EnvMapSphere() {
		super("OpenGL SuperBible - Spherical Environment Map");
	}

    protected void startup() throws IOException
    {
        envmaps[0] = KTX.load(getMediaPath() + "/textures/envmaps/spheremap1.ktx");
        envmaps[1] = KTX.load(getMediaPath() + "/textures/envmaps/spheremap2.ktx");
        envmaps[2] = KTX.load(getMediaPath() + "/textures/envmaps/spheremap3.ktx");
        tex_envmap = envmaps[envmap_index];

        object.load(getMediaPath() + "/objects/dragon.sbm");

        load_shaders();

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
    }

    protected void render(double currentTime)
    {

    	// double f = 1;
    	double f = currentTime * 10;
    	double g = currentTime * 10;
    	
        glClearBuffer4f(GL_COLOR, 0, 0.2f, 0.2f, 0.2f, 1.0f);
        glClearBuffer1f(GL_DEPTH, 0, 1f);
        glBindTexture(GL_TEXTURE_2D, tex_envmap);

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glUseProgram(render_prog);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);

        Matrix4x4f view_matrix;
        Matrix4x4f model_matrix;

        // This variable allows to either rotate the object in front
        // of a steady camera or move the camera around the object instead.
        boolean steadyCam = false;
        
        if (steadyCam) {
        	// This was the original code for the model view matrix.
        	
        	view_matrix = Matrix4x4f.translate(0.0f, 0.0f, -15.0f);
            model_matrix = 
                    Matrix4x4f.rotate((float)f, 1.0f, 0.0f, 0.0f)
                    .mul(Matrix4x4f.rotate((float)f * 1.1f, 0.0f, 1.0f, 0.0f))
                    .mul(Matrix4x4f.translate(0.0f, -4.0f, 0.0f));
        } else {
        	// This has been added to reveal a weakness of this approach
        	// to a moving camera: the environment map virtually moves 
        	// with the camera, always producing the same light effects.
        	// If the model would be a ball for example, the project image
        	// would not change at all, even though we are moving the camera 
        	// around it and light reflections should change.
        	
        	// I do not know whether this is a general issue or 
        	// just this implementation.
        	Vector3f eye = new Vector3f(0, 0, -15);
        	Vector3f center = new Vector3f(0.0f);
        	Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
        	
        	Matrix4x4f rotate = Matrix4x4f.rotate((float) g, 0f, 1f, 0f);
        	
        	eye = rotate.mul(eye);
        	
        	view_matrix = Matrix4x4f.lookat(eye, center, up);
        	
            model_matrix = Matrix4x4f.translate(0.0f, -4.0f, 0.0f);
        }

        Matrix4x4f mv_matrix = view_matrix.mul(model_matrix);
        
        
        glUniformMatrix4(uniforms.mv_matrix, false, mv_matrix.toFloatBuffer());
        glUniformMatrix4(uniforms.proj_matrix, false, proj_matrix.toFloatBuffer());

        object.render();
    }

    protected void shutdown()
    {
        glDeleteProgram(render_prog);
        glDeleteTextures(envmaps[0]);
        glDeleteTextures(envmaps[1]);
        glDeleteTextures(envmaps[2]);
    }

    void load_shaders() throws IOException
    {
        if (render_prog != 0)
            glDeleteProgram(render_prog);

        int vs, fs;

        vs = Shader.load(getMediaPath() + "/shaders/envmapsphere/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/envmapsphere/render.fs.glsl", GL_FRAGMENT_SHADER);

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
                case 'E':
                    envmap_index = (envmap_index + 1) % 3;
                    tex_envmap = envmaps[envmap_index];
                    break;
            }
        }
    }

	public static void main(String[] args) {
		new EnvMapSphere().run();
	}

}
