package hdrbloom;


import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.sbm.SBMObject;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.MathHelper;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL42.*;

/**
 * Please note: This algorithm has a bug .. the same one of the original algorithm ;)
 * @author homac
 *
 */
public class HDRBloom extends Application {
	
    private static final int MAX_SCENE_WIDTH     = 2048;
	private static final int MAX_SCENE_HEIGHT    = 2048;
	private static final int SPHERE_COUNT        = 32;

    private static FloatBuffer black = BufferUtilsHelper.createFloatBuffer(new float[]{ 0.0f, 0.0f, 0.0f, 1.0f });
    private static float one = 1.0f;


    private int      tex_src;
    private int      tex_lut;

    private int      render_fbo;
    private int[]      filter_fbo = new int[2];

    private int      tex_scene;
    private int      tex_brightpass;
    private int      tex_depth;
    private int[]      tex_filter = new int[2];

    private int      program_render = 0;
    private int      program_filter = 0;
    private int      program_resolve = 0;
    private int      vao;
    
    private volatile float       exposure = 1f;
    private volatile int         mode = 0;
    private volatile boolean        paused = false;
    private volatile float       bloom_factor = 1f;
    private volatile boolean        show_bloom = true;
    private volatile boolean        show_scene = true;
    private volatile boolean        show_prefilter = false;
    private volatile float       bloom_thresh_min = .8f;
    private volatile float       bloom_thresh_max = 1.2f;

    class Uniforms
    {
        class Scene
        {
            int bloom_thresh_min;
            int bloom_thresh_max;
        } 
        Scene scene = new Scene();
        class Resolve
        {
            int exposure;
            int bloom_factor;
            int scene_factor;
        } 
        Resolve resolve = new Resolve();
    } 
    
    private Uniforms uniforms = new Uniforms();

    private int      ubo_transform;
    private int      ubo_material;

    private SBMObject object = new SBMObject();

    static class Material
    {

    	
        Vector3f     diffuse_color = new Vector3f();
        int    		 padding_1;    // pad 32 bit
        Vector3f     specular_color = new Vector3f();
        float        specular_power;
        Vector3f     ambient_color = new Vector3f();
        int          padding_2;    // pad 32 bit

		public Material() {
		}

		public void read(ByteBuffer buf) {
			diffuse_color.fromByteBuffer(buf);
			padding_1 = buf.getInt();
			specular_color.fromByteBuffer(buf);
			specular_power = buf.getFloat();
			ambient_color.fromByteBuffer(buf);
			padding_2 = buf.getInt();
		}

		public void write(ByteBuffer buf) {
			diffuse_color.toByteBuffer(buf);
			buf.putInt(padding_1);
			specular_color.toByteBuffer(buf);
			buf.putFloat(specular_power);
			ambient_color.toByteBuffer(buf);
			buf.putInt(padding_2);
		}

		public static int sizeof() {
			return    Vector3f.sizeof() 
					+ BufferUtilsHelper.SIZEOF_INTEGER
					+ Vector3f.sizeof()
					+ BufferUtilsHelper.SIZEOF_FLOAT
					+ Vector3f.sizeof()
					+ BufferUtilsHelper.SIZEOF_INTEGER;
		}
    };

    static class Transforms
    {
        Matrix4x4f mat_proj;
        Matrix4x4f mat_view;
        Matrix4x4f[] mat_model = new Matrix4x4f[SPHERE_COUNT];
        

        public Transforms() {
        }

        public Transforms(boolean allocate) {
            mat_proj = new Matrix4x4f();
            mat_view = new Matrix4x4f();
        	for (int i = 0; i < SPHERE_COUNT; i++) {
        		mat_model[i] = new Matrix4x4f();
        	}
        }
        
		public static long sizeof() {
			return Matrix4x4f.sizeof() * (2 + SPHERE_COUNT);
		}
		public void write(ByteBuffer out) {
			mat_proj.toByteBuffer(out);
			mat_view.toByteBuffer(out);
			for (int i = 0; i < SPHERE_COUNT; i++) {
				mat_model[i].toByteBuffer(out);
			}
		}
        
    }

    
    private double last_time = 0.0;
    private double total_time = 0.0;

    
	public HDRBloom() {
		super("OpenGL SuperBible - HDR Bloom");
	}


    protected void startup() throws IOException
    {
        int i;
        IntBuffer buffers = BufferUtilsHelper.createIntBuffer(new int[]{ GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1 });

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        load_shaders();

        FloatBuffer exposureLUT   = BufferUtilsHelper.createFloatBuffer(new float[]{ 11.0f, 6.0f, 3.2f, 2.8f, 2.2f, 1.90f, 1.80f, 1.80f, 1.70f, 1.70f,  1.60f, 1.60f, 1.50f, 1.50f, 1.40f, 1.40f, 1.30f, 1.20f, 1.10f, 1.00f });

        render_fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, render_fbo);

        tex_scene = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex_scene);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA16F, MAX_SCENE_WIDTH, MAX_SCENE_HEIGHT);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, tex_scene, 0);
        tex_brightpass = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex_brightpass);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA16F, MAX_SCENE_WIDTH, MAX_SCENE_HEIGHT);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, tex_brightpass, 0);
        tex_depth = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, tex_depth);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH_COMPONENT32F, MAX_SCENE_WIDTH, MAX_SCENE_HEIGHT);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, tex_depth, 0);
        glDrawBuffers(buffers);

        filter_fbo[0] = glGenFramebuffers();
        filter_fbo[1] = glGenFramebuffers();
        
        tex_filter[0] = glGenTextures();
        tex_filter[1] = glGenTextures();
        
        for (i = 0; i < 2; i++)
        {
            glBindFramebuffer(GL_FRAMEBUFFER, filter_fbo[i]);
            glBindTexture(GL_TEXTURE_2D, tex_filter[i]);
            glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA16F, i != 0 ? MAX_SCENE_WIDTH : MAX_SCENE_HEIGHT, i != 0 ? MAX_SCENE_HEIGHT : MAX_SCENE_WIDTH);
            glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, tex_filter[i], 0);
            glDrawBuffers(buffers.get(0));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        tex_lut = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, tex_lut);
        glTexStorage1D(GL_TEXTURE_1D, 1, GL_R32F, 20);
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, 20, GL_RED, GL_FLOAT, exposureLUT);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);

        object.load(getMediaPath() + "/objects/sphere.sbm");

        ubo_transform = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, ubo_transform);
        glBufferData(GL_UNIFORM_BUFFER, (2 + SPHERE_COUNT) * Matrix4x4f.sizeof(), GL_DYNAMIC_DRAW);


        
        ubo_material = glGenBuffers();
        glBindBuffer(GL_UNIFORM_BUFFER, ubo_material);
        glBufferData(GL_UNIFORM_BUFFER, SPHERE_COUNT * Material.sizeof(), GL_STATIC_DRAW);

        ByteBuffer stream = glMapBufferRange(GL_UNIFORM_BUFFER, 0, SPHERE_COUNT * Material.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        
        Material m = new Material();
        
        float ambient = 0.002f;
        for (i = 0; i < SPHERE_COUNT; i++)
        {
            float fi = 3.14159267f * (float)i / 8.0f;
            m.diffuse_color.set(MathHelper.sinf(fi) * 0.5f + 0.5f, MathHelper.sinf(fi + 1.345f) * 0.5f + 0.5f, MathHelper.sinf(fi + 2.567f) * 0.5f + 0.5f);
            m.specular_color.set(2.8f, 2.8f, 2.9f);
            m.specular_power = 30.0f;
            float this_ambient = ambient * 0.025f;
            m.ambient_color.set(this_ambient, this_ambient, this_ambient);
            ambient *= 1.5f;
            
            // write material data to mapped buffer region
            m.write(stream);
        }
        
        glUnmapBuffer(GL_UNIFORM_BUFFER);
    }

    protected void shutdown()
    {
        glDeleteProgram(program_render);
        glDeleteProgram(program_filter);
        glDeleteProgram(program_resolve);
        glDeleteVertexArrays(vao);
        glDeleteTextures(tex_src);
        glDeleteTextures(tex_lut);
    }

    protected void render(double currentTime)
    {
        int i;

        if (!paused)
            total_time += (currentTime - last_time);
        last_time = currentTime;
        float t = (float)total_time;

        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glBindFramebuffer(GL_FRAMEBUFFER, render_fbo);
        glClearBuffer(GL_COLOR, 0, black);
        glClearBuffer(GL_COLOR, 1, black);
        glClearBuffer1f(GL_DEPTH, 0, one);

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);

        glUseProgram(program_render);

        glBindBufferBase(GL_UNIFORM_BUFFER, 0, ubo_transform);
        
        ByteBuffer stream = glMapBufferRange(GL_UNIFORM_BUFFER, 0, Transforms.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
        
        Transforms transforms = new Transforms(true);
        
        transforms.mat_proj = Matrix4x4f.perspective(50.0f, (float)info.windowWidth / (float)info.windowHeight, 1.0f, 1000.0f);
        transforms.mat_view.setTranslate(0.0f, 0.0f, -20.0f);
        for (i = 0; i < SPHERE_COUNT; i++)
        {
            float fi = 3.141592f * (float)i / 16.0f;
            // float r = cosf(fi * 0.25f) * 0.4f + 1.0f;
            float r = (i & 2) != 0 ? 0.6f : 1.5f;
            transforms.mat_model[i].setTranslate(MathHelper.cosf(t + fi) * 5.0f * r, MathHelper.sinf(t + fi * 4.0f) * 4.0f, MathHelper.sinf(t + fi) * 5.0f * r);
        }
        
        transforms.write(stream);
        
        glUnmapBuffer(GL_UNIFORM_BUFFER);
        glBindBufferBase(GL_UNIFORM_BUFFER, 1, ubo_material);

        glUniform1f(uniforms.scene.bloom_thresh_min, bloom_thresh_min);
        glUniform1f(uniforms.scene.bloom_thresh_max, bloom_thresh_max);

        object.render(SPHERE_COUNT);

        glDisable(GL_DEPTH_TEST);

        glUseProgram(program_filter);

        glBindVertexArray(vao);

        glBindFramebuffer(GL_FRAMEBUFFER, filter_fbo[0]);
        glBindTexture(GL_TEXTURE_2D, tex_brightpass);
        glViewport(0, 0, info.windowHeight, info.windowWidth);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glBindFramebuffer(GL_FRAMEBUFFER, filter_fbo[1]);
        glBindTexture(GL_TEXTURE_2D, tex_filter[0]);
        glViewport(0, 0, info.windowWidth, info.windowHeight);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

        glUseProgram(program_resolve);

        glUniform1f(uniforms.resolve.exposure, exposure);
        if (show_prefilter)
        {
            glUniform1f(uniforms.resolve.bloom_factor, 0.0f);
            glUniform1f(uniforms.resolve.scene_factor, 1.0f);
        }
        else
        {
            glUniform1f(uniforms.resolve.bloom_factor, show_bloom ? bloom_factor : 0.0f);
            glUniform1f(uniforms.resolve.scene_factor, show_scene ? 1.0f : 0.0f);
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, tex_filter[1]);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, show_prefilter ? tex_brightpass : tex_scene);

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    }

    protected void onKey(int key, int action) throws IOException
    {
        if (action != GLFW.GLFW_PRESS)
            return;

        switch (key)
        {
            case '1':
            case '2':
            case '3':
                    mode = key - '1';
                break;
            case 'B':
                    show_bloom = !show_bloom;
                break;
            case 'V':
                    show_scene = !show_scene;
                break;
            case 'A':
                    bloom_factor += 0.1f;
                break;
            case 'Z':
                    bloom_factor -= 0.1f;
                break;
            case 'S':
                    bloom_thresh_min += 0.1f;
                break;
            case 'X':
                    bloom_thresh_min -= 0.1f;
                break;
            case 'D':
                    bloom_thresh_max += 0.1f;
                break;
            case 'C':
                    bloom_thresh_max -= 0.1f;
                break;
            case 'R':
                    load_shaders();
                break;
            case 'N':
                    show_prefilter = !show_prefilter;
                    break;
            case 'M':
                    mode = (mode + 1) % 3;
                break;
            case 'P':
                    paused = !paused;
                break;
            case GLFW.GLFW_KEY_KP_ADD:
                    exposure *= 1.1f;
                break;
            case GLFW.GLFW_KEY_KP_SUBTRACT:
                    exposure /= 1.1f;
                break;
        }
    }

    void load_shaders() throws IOException
    {
        int vs;
        int fs;

        if (program_render != 0)
            glDeleteProgram(program_render);

        vs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-scene.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-scene.fs.glsl", GL_FRAGMENT_SHADER);
        program_render = Program.link(true, vs, fs);

        uniforms.scene.bloom_thresh_min = glGetUniformLocation(program_render, "bloom_thresh_min");
        uniforms.scene.bloom_thresh_max = glGetUniformLocation(program_render, "bloom_thresh_max");

        if (program_filter != 0)
            glDeleteProgram(program_filter);

        vs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-filter.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-filter.fs.glsl", GL_FRAGMENT_SHADER);
        program_filter = Program.link(true, vs, fs);

        if (program_resolve != 0)
            glDeleteProgram(program_resolve);

        vs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-resolve.vs.glsl", GL_VERTEX_SHADER);
        fs = Shader.load(getMediaPath() + "/shaders/hdrbloom/hdrbloom-resolve.fs.glsl", GL_FRAGMENT_SHADER);
        program_resolve = Program.link(true, vs, fs);

        uniforms.resolve.exposure = glGetUniformLocation(program_resolve, "exposure");
        uniforms.resolve.bloom_factor = glGetUniformLocation(program_resolve, "bloom_factor");
        uniforms.resolve.scene_factor = glGetUniformLocation(program_resolve, "scene_factor");
    }

	
	public static void main(String[] args) {
		new HDRBloom().run();
	}

}
