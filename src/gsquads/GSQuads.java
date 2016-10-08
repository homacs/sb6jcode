package gsquads;


import static org.lwjgl.opengl.GL11.GL_COLOR;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_FAN;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;
import static org.lwjgl.opengl.GL32.GL_LINES_ADJACENCY;

import java.io.IOException;

import org.lwjgl.glfw.GLFW;

import sb6.application.Application;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;

public class GSQuads extends Application {
    private int      program_fans = 0;
    private int      program_linesadjacency = 0;
    private int      vao;
    private volatile int         mode;
    private int         mvp_loc_fans;
    private int         mvp_loc_linesadj;
    private int         vid_offset_loc_fans;
    private int         vid_offset_loc_linesadj;
    private int         vid_offset;
    private volatile boolean       paused;

    private double last_time = 0.0;
    private double total_time = 0.0;

    
	public GSQuads() {
		super("OpenGL SuperBible - Quad Rendering");
	}


    protected void startup() throws IOException
    {
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        load_shaders();
    }

    protected void shutdown()
    {
        glDeleteProgram(program_linesadjacency);
        glDeleteProgram(program_fans);
        glDeleteVertexArrays(vao);
    }

    
    protected void render(double currentTime)
    {
        glViewport(0, 0, info.windowWidth, info.windowHeight);


        if (!paused)
            total_time += (currentTime - last_time);
        last_time = currentTime;

        float t = (float)total_time;

        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.25f, 0.0f, 1.0f);

        Matrix4x4f mv_matrix = Matrix4x4f.translate(0.0f, 0.0f, -2.0f)
        		.mul(Matrix4x4f.rotate((float)t * 5.0f, 0.0f, 0.0f, 1.0f))
        		.mul(Matrix4x4f.rotate((float)t * 30.0f, 1.0f, 0.0f, 0.0f));
        Matrix4x4f proj_matrix = Matrix4x4f.perspective(50.0f, (float)info.windowWidth / (float)info.windowHeight, 0.1f, 1000.0f);
        Matrix4x4f mvp = proj_matrix.mul(mv_matrix);

        switch (mode)
        {
            case 0:
                glUseProgram(program_fans);
                glUniformMatrix4(mvp_loc_fans, false, mvp.toFloatBuffer());
                glUniform1i(vid_offset_loc_fans, vid_offset);
                glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
                break;
            case 1:
                glUseProgram(program_linesadjacency);
                glUniformMatrix4(mvp_loc_linesadj, false, mvp.toFloatBuffer());
                glUniform1i(vid_offset_loc_linesadj, vid_offset);
                glDrawArrays(GL_LINES_ADJACENCY, 0, 4);
                break;
        }
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action != GLFW.GLFW_PRESS)
            return;

        switch (key)
        {
            case '1':
            case '2':
                    mode = key - '1';
                break;
            case GLFW.GLFW_KEY_KP_ADD:
                vid_offset++;
                break;
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                vid_offset--;
                break;
            case 'P': paused = !paused;
                break;
            case 'R':
                    load_shaders();
                break;
            case 'M':
                    mode = (mode + 1) % 2;
                break;
        }
    }

    void load_shaders() throws IOException
    {
        int vs;
        int gs;
        int fs;

        if (program_fans != 0)
            glDeleteProgram(program_fans);

        program_fans = glCreateProgram();

        vs = Shader.load(getMediaPath() + "/shaders/gsquads/quadsasfans.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/gsquads/quadsasfans.fs.glsl", GL_FRAGMENT_SHADER);

        glAttachShader(program_fans, vs);
        glAttachShader(program_fans, fs);

        glLinkProgram(program_fans);

        mvp_loc_fans = glGetUniformLocation(program_fans, "mvp");
        vid_offset_loc_fans = glGetUniformLocation(program_fans, "vid_offset");

        glDeleteShader(vs);
        glDeleteShader(fs);

        vs = Shader.load(getMediaPath() + "/shaders/gsquads/quadsaslinesadj.vs.glsl", GL_VERTEX_SHADER);
        gs = Shader.load(getMediaPath() + "/shaders/gsquads/quadsaslinesadj.gs.glsl", GL_GEOMETRY_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/gsquads/quadsaslinesadj.fs.glsl", GL_FRAGMENT_SHADER);

        if (program_linesadjacency != 0)
            glDeleteProgram(program_linesadjacency);

        program_linesadjacency = glCreateProgram();

        glAttachShader(program_linesadjacency, vs);
        glAttachShader(program_linesadjacency, gs);
        glAttachShader(program_linesadjacency, fs);

        glLinkProgram(program_linesadjacency);

        mvp_loc_linesadj = glGetUniformLocation(program_linesadjacency, "mvp");
        vid_offset_loc_linesadj = glGetUniformLocation(program_fans, "vid_offset");

        glDeleteShader(vs);
        glDeleteShader(gs);
        glDeleteShader(fs);
    }

	public static void main(String[] args) {
		new GSQuads().run();
	}

}
