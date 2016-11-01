package deferredshading;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static sb6.vmath.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.*;



public class DeferredShading extends Application {

    private static final int MAX_DISPLAY_WIDTH       = 2048;
    private static final int MAX_DISPLAY_HEIGHT      = 2048;
    private static final int NUM_LIGHTS              = 64;
    private static final int NUM_INSTANCES           = (15 * 15);

    private int      gbuffer;
    private int[]    gbuffer_tex = new int[3];
    private int      fs_quad_vao;

    private SBMObject object = new SBMObject();

    private int      render_program = 0;
    private int      render_program_nm = 0;
    private int      render_transform_ubo = 0;

    private int      light_program = 0;
    private int      light_ubo;

    private int      vis_program;
    private int      loc_vis_mode;

    private int      tex_diffuse;
    private int      tex_nm;

    private boolean  use_nm = true;
    private boolean  paused = false;

    private double   last_time = 0.0;
    private double   total_time = 0.0;

    enum VisMode
    {
        VIS_OFF,
        VIS_NORMALS,
        VIS_WS_COORDS,
        VIS_DIFFUSE,
        VIS_META
    }
    VisMode vis_mode = VisMode.VIS_OFF;

    static class Light
    {
        Vector3f   position;
        int        pad0;       // pad0
        Vector3f   color;
        int        pad1;       // pad1
		public static int sizeof() {
			return Vector3f.sizeof() * 2 + BufferUtilsHelper.SIZEOF_INTEGER * 2;
		}
		public void write(ByteBuffer buf) {
			position.toByteBuffer(buf);
			buf.putInt(pad0);
			color.toByteBuffer(buf);
			buf.putInt(pad1);
		}
    }

	public DeferredShading() {
		super("OpenGL SuperBible - Deferred Shading");
	}
	
    protected void startup() throws IOException
    {
    	gbuffer = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, gbuffer);

        int i = 0;
        gbuffer_tex[i++] = glGenTextures();
        gbuffer_tex[i++] = glGenTextures();
        gbuffer_tex[i++] = glGenTextures();
        
        glBindTexture(GL_TEXTURE_2D, gbuffer_tex[0]);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32UI, MAX_DISPLAY_WIDTH, MAX_DISPLAY_HEIGHT); 
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, gbuffer_tex[1]);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, MAX_DISPLAY_WIDTH, MAX_DISPLAY_HEIGHT); 
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glBindTexture(GL_TEXTURE_2D, gbuffer_tex[2]);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT32F, MAX_DISPLAY_WIDTH, MAX_DISPLAY_HEIGHT); 

        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, gbuffer_tex[0], 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, gbuffer_tex[1], 0);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, gbuffer_tex[2], 0);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        fs_quad_vao = glGenVertexArrays();
        glBindVertexArray(fs_quad_vao);

        object.load(getMediaPath() + "/objects/ladybug.sbm");
        tex_nm = KTX.load(getMediaPath() + "/textures/ladybug_nm.ktx");
        tex_diffuse = KTX.load(getMediaPath() + "/textures/ladybug_co.ktx");

        load_shaders();

        light_ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, light_ubo);
        glBufferData(GL_UNIFORM_BUFFER, NUM_LIGHTS * Light.sizeof(), GL_DYNAMIC_DRAW);

        render_transform_ubo = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, render_transform_ubo);
        glBufferData(GL_UNIFORM_BUFFER, (2 + NUM_INSTANCES) * Matrix4x4f.sizeof(), GL_DYNAMIC_DRAW);
    }

    protected void render(double currentTime) throws InterruptedException
    {
        final IntBuffer draw_buffers = BufferUtilsHelper.createIntBuffer(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });
        int i, j;

        if (!paused)
        {
            total_time += (currentTime - last_time);
        }
        else
        {
        	// TODO: Windows: Do we really need 10ms sleep here?
            Thread.sleep(10);
        }
        last_time = currentTime;

        float t = (float)total_time;

        glBindFramebuffer(GL_FRAMEBUFFER, gbuffer);
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glDrawBuffers(draw_buffers);
        glClearBuffer4i(GL_COLOR, 0, 0, 0, 0, 0);
        glClearBuffer4i(GL_COLOR, 1, 0, 0, 0, 0);
        glClearBuffer4f(GL_DEPTH, 0, 1.0f, 1.0f, 1.0f, 1.0f);

        glBindBufferBase(GL_UNIFORM_BUFFER, 0, render_transform_ubo);
        ByteBuffer matrices_buf = glMapBufferRange(GL_UNIFORM_BUFFER,
        		0,
        		(2 + NUM_INSTANCES) * Matrix4x4f.sizeof(),
        		GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        Matrix4x4f matrix = Matrix4x4f.perspective(50.0f,
                                         (float)info.windowWidth / (float)info.windowHeight,
                                         0.1f,
                                         1000.0f);
        matrix.toByteBuffer(matrices_buf);
        float d = (sinf(t * 0.131f) + 2.0f) * 0.15f;
        Vector3f eye_pos = new Vector3f(d * 120.0f * sinf(t * 0.11f),
                                          5.5f,
                                          d * 120.0f * cosf(t * 0.01f));
        matrix = Matrix4x4f.lookat(eye_pos,
                                    new Vector3f(0.0f, -20.0f, 0.0f),
                                    new Vector3f(0.0f, 1.0f, 0.0f));
        matrix.toByteBuffer(matrices_buf);

        for (j = 0; j < 15; j++)
        {
            for (i = 0; i < 15; i++)
            {
                matrix = Matrix4x4f.translate(((float)i - 7.5f) * 7.0f, 0.0f, ((float)j - 7.5f) * 11.0f);
                matrix.toByteBuffer(matrices_buf);
            }
        }

        glUnmapBuffer(GL_UNIFORM_BUFFER);

        glUseProgram(use_nm ? render_program_nm : render_program);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tex_diffuse);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, tex_nm);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);

        object.render(NUM_INSTANCES);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glDrawBuffer(GL_BACK);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, gbuffer_tex[0]);

        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, gbuffer_tex[1]);

        if (vis_mode == VisMode.VIS_OFF)
        {
            glUseProgram(light_program);
        }
        else
        {
            glUseProgram(vis_program);
            glUniform1i(loc_vis_mode, vis_mode.ordinal());
        }

        glDisable(GL_DEPTH_TEST);

        glBindBufferBase(GL_UNIFORM_BUFFER, 0, light_ubo);
        ByteBuffer lights_buf = glMapBufferRange(GL_UNIFORM_BUFFER,
        		0,
        		NUM_LIGHTS * Light.sizeof(),
        		GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        
        Light l = new Light();
        for (i = 0; i < NUM_LIGHTS; i++)
        {
            float i_f = ((float)i - 7.5f) * 0.1f + 0.3f;
            // t = 0.0f;
            l.position = new Vector3f(100.0f * sinf(t * 1.1f + (5.0f * i_f)) * cosf(t * 2.3f + (9.0f * i_f)),
                                             15.0f,
                                             100.0f * sinf(t * 1.5f + (6.0f * i_f)) * cosf(t * 1.9f + (11.0f * i_f))); // 300.0f * sinf(t * i_f * 0.7f) * cosf(t * i_f * 0.9f) - 600.0f);
            l.color = new Vector3f(cosf(i_f * 14.0f) * 0.5f + 0.8f,
                                          sinf(i_f * 17.0f) * 0.5f + 0.8f,
                                          sinf(i_f * 13.0f) * cosf(i_f * 19.0f) * 0.5f + 0.8f);
            l.write(lights_buf);
        }

        glUnmapBuffer(GL_UNIFORM_BUFFER);

        glBindVertexArray(fs_quad_vao);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindTexture(GL_TEXTURE_2D, 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    protected void shutdown()
    {
    	int i = 0;
        glDeleteTextures(gbuffer_tex[i++]);
        glDeleteTextures(gbuffer_tex[i++]);
        glDeleteTextures(gbuffer_tex[i++]);
        glDeleteFramebuffers(gbuffer);
        glDeleteProgram(render_program);
        glDeleteProgram(light_program);
    }

    void load_shaders() throws IOException
    {
        if (render_program != 0)
            glDeleteProgram(render_program);
        if (light_program != 0)
            glDeleteProgram(light_program);
        int vs, fs;

        vs = Shader.load(getMediaPath() + "/shaders/deferredshading/render.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/deferredshading/render.fs.glsl", GL_FRAGMENT_SHADER);

        render_program = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        vs = Shader.load(getMediaPath() + "/shaders/deferredshading/render-nm.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/deferredshading/render-nm.fs.glsl", GL_FRAGMENT_SHADER);

        render_program_nm = Program.link(true, vs, fs);

        glDeleteShader(vs);
        glDeleteShader(fs);

        vs = Shader.load(getMediaPath() + "/shaders/deferredshading/light.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/deferredshading/light.fs.glsl", GL_FRAGMENT_SHADER);

        light_program = Program.link(true, vs, fs);

        glDeleteShader(fs);

        fs = Shader.load(getMediaPath() + "/shaders/deferredshading/render-vis.fs.glsl", GL_FRAGMENT_SHADER);

        vis_program = Program.link(true, vs, fs);

        loc_vis_mode = glGetUniformLocation(vis_program, "vis_mode");

        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action == GLFW.GLFW_PRESS)
        {
            switch (key)
            {
                case 'R': load_shaders();
                    break;
                case 'P': paused = !paused;
                    break;
                case 'N': use_nm = !use_nm;
                    break;
                case '1': vis_mode = VisMode.VIS_OFF;
                    break;
                case '2': vis_mode = VisMode.VIS_NORMALS;
                    break;
                case '3': vis_mode = VisMode.VIS_WS_COORDS;
                    break;
                case '4': vis_mode = VisMode.VIS_DIFFUSE;
                    break;
                case '5': vis_mode = VisMode.VIS_META;
                    break;
            }
        }
    }

	public static void main(String[] args) {
		new DeferredShading().run();
	}

}
