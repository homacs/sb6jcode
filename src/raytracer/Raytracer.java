package raytracer;

import org.lwjgl.glfw.GLFW;

import java.io.IOException;



import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import sb6.BufferUtilsHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import sb6.vmath.Vector3f;
import sb6.vmath.Vector4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;
import static sb6.vmath.MathHelper.*;

public class Raytracer extends Application {
	private static final IntBuffer draw_buffers = BufferUtilsHelper.createIntBuffer(new int[]
	    {
	        GL_COLOR_ATTACHMENT0,
	        GL_COLOR_ATTACHMENT1,
	        GL_COLOR_ATTACHMENT2,
	        GL_COLOR_ATTACHMENT3,
	        GL_COLOR_ATTACHMENT4,
	        GL_COLOR_ATTACHMENT5
	    });

	
	
    private int          prepare_program = 0;
    private int          trace_program = 0;
    private int          blit_program = 0;

    static class UniformsBlock
    {
        Matrix4x4f     mv_matrix;
        Matrix4x4f     view_matrix;
        Matrix4x4f     proj_matrix;
        
		public static long sizeof() {
			return Matrix4x4f.sizeof()*3;
		}
		public void write(ByteBuffer stream) {
			mv_matrix.toByteBuffer(stream);
			view_matrix.toByteBuffer(stream);
			proj_matrix.toByteBuffer(stream);
		}
    };

    private int          uniforms_buffer;
    private int          sphere_buffer;
    private int          plane_buffer;
    private int          light_buffer;

    class Uniforms
    {
        int           ray_origin;
        int           ray_lookat;
        int           aspect;
    } 
    Uniforms uniforms = new Uniforms();

    private int          vao;

    static class Sphere
    {
        Vector3f     center;
        float           radius;
        // unsigned int    : 32; // pad
        Vector4f     color;
        
		public static int sizeof() {
			return Vector3f.sizeof() + BufferUtilsHelper.SIZEOF_FLOAT + Vector4f.sizeof();
		}

		public void write(ByteBuffer stream) {
			center.toByteBuffer(stream);
			stream.putFloat(radius);
			color.toByteBuffer(stream);
		}
    }

    static class Plane
    {
        Vector3f     normal;
        float           d;
        
		public static int sizeof() {
			return Vector3f.sizeof() + BufferUtilsHelper.SIZEOF_FLOAT;
		}
		public void write(ByteBuffer stream) {
			normal.toByteBuffer(stream);
			stream.putFloat(d);
		}
    }

    static class Light
    {
        Vector3f     position;
        int    pad;       // pad
		public static int sizeof() {
			return Vector3f.sizeof() + BufferUtilsHelper.SIZEOF_INTEGER;
		}
		public void write(ByteBuffer stream) {
			position.toByteBuffer(stream);
			stream.putInt(pad);
		}
    }

    private static final int    MAX_RECURSION_DEPTH     = 5;
    private static final int    MAX_FB_WIDTH            = 2048;
    private static final int    MAX_FB_HEIGHT           = 1024;

    enum DEBUG_MODE
    {
        DEBUG_NONE,
        DEBUG_REFLECTED,
        DEBUG_REFRACTED,
        DEBUG_REFLECTED_COLOR,
        DEBUG_REFRACTED_COLOR
    };

    private int              tex_composite;
    private int[]            ray_fbo = new int[MAX_RECURSION_DEPTH];
    private int[]            tex_position = new int[MAX_RECURSION_DEPTH];
    private int[]            tex_reflected = new int[MAX_RECURSION_DEPTH];
    private int[]            tex_reflection_intensity = new int[MAX_RECURSION_DEPTH];
    private int[]            tex_refracted = new int[MAX_RECURSION_DEPTH];
    private int[]            tex_refraction_intensity = new int[MAX_RECURSION_DEPTH];

    private int                 max_depth = 1;
    private int                 debug_depth = 0;
    private DEBUG_MODE          debug_mode = DEBUG_MODE.DEBUG_NONE;
    private boolean             paused = false;

    
    private double last_time = 0.0;
    private double total_time = 0.0;

	public Raytracer() {
		super("OpenGL SuperBible - Ray Tracing");
	}
	
	protected void startup() throws IOException
	{
	    int i;
	
	    load_shaders();
	
	    uniforms_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, uniforms_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, UniformsBlock.sizeof(), GL_DYNAMIC_DRAW);
	
	    sphere_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, sphere_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, 128 * Sphere.sizeof(), GL_DYNAMIC_DRAW);
	
	    plane_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, plane_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, 128 * Plane.sizeof(), GL_DYNAMIC_DRAW);
	
	    light_buffer = glGenBuffers();
	    glBindBuffer(GL_UNIFORM_BUFFER, light_buffer);
	    glBufferData(GL_UNIFORM_BUFFER, 128 * Light.sizeof(), GL_DYNAMIC_DRAW);
	
	    vao = glGenVertexArrays();
	    glBindVertexArray(vao);
	
	    tex_composite = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, tex_composite);
	    glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	
	    for (i = 0; i < MAX_RECURSION_DEPTH; i++)
	    {
	    	ray_fbo[i] = glGenFramebuffers();
	        glBindFramebuffer(GL_FRAMEBUFFER, ray_fbo[i]);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, tex_composite, 0);
	
	    	tex_position[i] = glGenTextures();
	        glBindTexture(GL_TEXTURE_2D, tex_position[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB32F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, tex_position[i], 0);
	
	    	tex_reflected[i] = glGenTextures();
	        glBindTexture(GL_TEXTURE_2D, tex_reflected[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT2, tex_reflected[i], 0);
	
	    	tex_refracted[i] = glGenTextures();
	        glBindTexture(GL_TEXTURE_2D, tex_refracted[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT3, tex_refracted[i], 0);
	
	    	tex_reflection_intensity[i] = glGenTextures();
	        glBindTexture(GL_TEXTURE_2D, tex_reflection_intensity[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT4, tex_reflection_intensity[i], 0);
	
	    	tex_refraction_intensity[i] = glGenTextures();
	        glBindTexture(GL_TEXTURE_2D, tex_refraction_intensity[i]);
	        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGB16F, MAX_FB_WIDTH, MAX_FB_HEIGHT);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	        glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT5, tex_refraction_intensity[i], 0);
	    }
	
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);
	    glBindTexture(GL_TEXTURE_2D, 0);
	}
	
	protected void render(double currentTime)
	{
	
	    if (!paused)
	        total_time += (currentTime - last_time);
	    last_time = currentTime;
	
	    float f = (float)total_time;
	    ByteBuffer stream;
	    Vector3f view_position = new Vector3f(sinf(f * 0.3234f) * 28.0f, cosf(f * 0.4234f) * 28.0f, cosf(f * 0.1234f) * 28.0f); // sinf(f * 0.2341f) * 20.0f - 8.0f);
	    Vector3f lookat_point = new Vector3f(sinf(f * 0.214f) * 8.0f, cosf(f * 0.153f) * 8.0f, sinf(f * 0.734f) * 8.0f);
	    Matrix4x4f view_matrix = Matrix4x4f.lookat(view_position,
	                                            lookat_point,
	                                            new Vector3f(0.0f, 1.0f, 0.0f));
	    /*
	
	    Matrix4x4f model_matrix = Matrix4x4f.scale(7.0f);
		
	    glBindBufferBase(GL_UNIFORM_BUFFER, 0, uniforms_buffer);
	    stream = glMapBufferRange(GL_UNIFORM_BUFFER,
	    		0,
	    		UniformsBlock.sizeof(),
	    		GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
	
	    // f = 0.0f;
	
	    UniformsBlock block = new UniformsBlock();
	    
	    block.mv_matrix = Matrix4x4f.multiply(view_matrix, model_matrix);
	    block.view_matrix = view_matrix;
	    block.proj_matrix = Matrix4x4f.perspective(60.0f,
	                                            (float)info.windowWidth / (float)info.windowHeight,
	                                            0.1f,
	                                            1000.0f);
	    block.write(stream);
	    glUnmapBuffer(GL_UNIFORM_BUFFER);
	     */	
	    
	    //
	    // Add the spheres (balls) to the scene
	    //
	    glBindBufferBase(GL_UNIFORM_BUFFER, 1, sphere_buffer);
	    stream = glMapBufferRange(GL_UNIFORM_BUFFER, 0, 128 * Sphere.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
	
	    Sphere s = new Sphere();
	    int i;
	    for (i = 0; i < 128; i++)
	    {
	        // float f = 0.0f;
	        float fi = (float)i / 128.0f;
	        s.center = new Vector3f(sinf(fi * 123.0f + f) * 15.75f, cosf(fi * 456.0f + f) * 15.75f, (sinf(fi * 300.0f + f) * cosf(fi * 200.0f + f)) * 20.0f);
	        s.radius = fi * 2.3f + 3.5f;
	        float r = fi * 61.0f;
	        float g = r + 0.25f;
	        float b = g + 0.25f;
	        r = (r - floorf(r)) * 0.8f + 0.2f;
	        g = (g - floorf(g)) * 0.8f + 0.2f;
	        b = (b - floorf(b)) * 0.8f + 0.2f;
	        s.color = new Vector4f(r, g, b, 1.0f);
	        s.write(stream);
	    }
	    glUnmapBuffer(GL_UNIFORM_BUFFER);

	    //
	    // Add six planes of infinite size to create a room for the scene (inside a cube).
	    //
	    glBindBufferBase(GL_UNIFORM_BUFFER, 2, plane_buffer);
	    stream = glMapBufferRange(GL_UNIFORM_BUFFER, 0, 128 * Plane.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
	    Plane p = new Plane();
	    
        p.normal = new Vector3f(0.0f, 0.0f, -1.0f);
        p.d = 30.0f;
        p.write(stream);
        
        p.normal = new Vector3f(0.0f, 0.0f, 1.0f);
        p.d = 30.0f;
        p.write(stream);
	
        p.normal = new Vector3f(-1.0f, 0.0f, 0.0f);
        p.d = 30.0f;
        p.write(stream);
	
        p.normal = new Vector3f(1.0f, 0.0f, 0.0f);
        p.d = 30.0f;
        p.write(stream);
	
        p.normal = new Vector3f(0.0f, -1.0f, 0.0f);
        p.d = 30.0f;
        p.write(stream);
	
        p.normal = new Vector3f(0.0f, 1.0f, 0.0f);
        p.d = 30.0f;
        p.write(stream);

	    glUnmapBuffer(GL_UNIFORM_BUFFER);

	    //
	    // Create three light sources
	    //
	    glBindBufferBase(GL_UNIFORM_BUFFER, 3, light_buffer);
	    stream = glMapBufferRange(GL_UNIFORM_BUFFER, 0, 128 * Light.sizeof(), GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

	    Light l = new Light();
	    f *= 1.0f;
	
	    for (i = 0; i < 128; i++)
	    {
	        float fi = 3.33f - (float)i; //  / 35.0f;
	        l.position = new Vector3f(sinf(fi * 2.0f - f) * 15.75f,
	        		cosf(fi * 5.0f - f) * 5.75f,
	        		(sinf(fi * 3.0f - f) * cosf(fi * 2.5f - f)) * 19.4f);
	        l.write(stream);
	    }
	
	    glUnmapBuffer(GL_UNIFORM_BUFFER);
	
	    glBindVertexArray(vao);
	    glViewport(0, 0, info.windowWidth, info.windowHeight);
	
	    glUseProgram(prepare_program);
	    glUniformMatrix4(uniforms.ray_lookat, false, view_matrix.toFloatBuffer());
	    glUniform3(uniforms.ray_origin, view_position.toFloatBuffer());
	    glUniform1f(uniforms.aspect, ((float)info.windowHeight / (float)info.windowWidth));
	    glBindFramebuffer(GL_FRAMEBUFFER, ray_fbo[0]);

	    glDrawBuffers(draw_buffers);
	
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	
	    glUseProgram(trace_program);
	    recurse(0);
	
	    glUseProgram(blit_program);
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);
	    glDrawBuffer(GL_BACK);
	
	    glActiveTexture(GL_TEXTURE0);
	    switch (debug_mode)
	    {
	        case DEBUG_NONE:
	            glBindTexture(GL_TEXTURE_2D, tex_composite);
	            break;
	        case DEBUG_REFLECTED:
	            glBindTexture(GL_TEXTURE_2D, tex_reflected[debug_depth]);
	            break;
	        case DEBUG_REFRACTED:
	            glBindTexture(GL_TEXTURE_2D, tex_refracted[debug_depth]);
	            break;
	        case DEBUG_REFLECTED_COLOR:
	            glBindTexture(GL_TEXTURE_2D, tex_reflection_intensity[debug_depth]);
	            break;
	        case DEBUG_REFRACTED_COLOR:
	            glBindTexture(GL_TEXTURE_2D, tex_refraction_intensity[debug_depth]);
	            break;
	    }
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	
	    /*
	    glClearBufferfv(GL_COLOR, 0, gray);
	    glClearBufferfv(GL_DEPTH, 0, ones);
	
	
	    glBindVertexArray(vao);
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	    */
	}
	
	void recurse(int depth)
	{
	    glBindFramebuffer(GL_FRAMEBUFFER, ray_fbo[depth + 1]);
	
	    glDrawBuffers(draw_buffers);
	
	    glEnablei(GL_BLEND, 0);
	    glBlendFunci(0, GL_ONE, GL_ONE);
	
	    // static const float zeros[] = { 0.0f, 0.0f, 0.0f, 0.0f };
	    // glClearBufferfv(GL_COLOR, 0, zeros);
	
	    glActiveTexture(GL_TEXTURE0);
	    glBindTexture(GL_TEXTURE_2D, tex_position[depth]);
	
	    glActiveTexture(GL_TEXTURE1);
	    glBindTexture(GL_TEXTURE_2D, tex_reflected[depth]);
	
	    glActiveTexture(GL_TEXTURE2);
	    glBindTexture(GL_TEXTURE_2D, tex_reflection_intensity[depth]);
	
	    // Render
	    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	
	    if (depth != (max_depth - 1))
	    {
	        recurse(depth + 1);
	    }
	    //*/
	
	    /*
	    if (depth != 0)
	    {
	        glBindTexture(GL_TEXTURE_2D, tex_refracted[depth]);
	        glActiveTexture(GL_TEXTURE2);
	        glBindTexture(GL_TEXTURE_2D, tex_refraction_intensity[depth]);
	
	        // Render
	        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
	
	        if (depth != (max_depth - 1))
	        {
	            recurse(depth + 1);
	        }
	    }
	    //**/
	
	    glDisablei(GL_BLEND, 0);
	}
	
	protected void onKey(int key, int action) throws IOException
	{
	    if (action == GLFW.GLFW_PRESS)
	    {
	        switch (key)
	        {
	            case GLFW.GLFW_KEY_KP_ADD:
	                if (max_depth < MAX_RECURSION_DEPTH-1) max_depth++;
	                break;
	            case GLFW.GLFW_KEY_KP_SUBTRACT:
	            	if (max_depth > 1) max_depth--;
	                break;
	            case 'P':
	                paused = !paused;
	                break;
	            case 'R':
	                load_shaders();
	                break;
	            case 'Q':
	                debug_mode = DEBUG_MODE.DEBUG_NONE;
	                break;
	            case 'W':
	                debug_mode = DEBUG_MODE.DEBUG_REFLECTED;
	                break;
	            case 'E':
	                debug_mode = DEBUG_MODE.DEBUG_REFRACTED;
	                break;
	            case 'S':
	                debug_mode = DEBUG_MODE.DEBUG_REFLECTED_COLOR;
	                break;
	            case 'D':
	                debug_mode = DEBUG_MODE.DEBUG_REFRACTED_COLOR;
	                break;
	            case 'A':
	                debug_depth++;
	                if (debug_depth < MAX_RECURSION_DEPTH-1) debug_depth++;
	                break;
	            case 'Z':
	                if (debug_depth > 0) debug_depth--;
	                break;
	        }
	    }
	}
	
	void load_shaders() throws IOException
	{
	    int vs, fs;
	
	    vs = Shader.load(getMediaPath() + "/shaders/raytracer/trace-prepare.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/raytracer/trace-prepare.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (prepare_program != 0)
	        glDeleteProgram(prepare_program);
	
	    prepare_program = Program.link(true, vs, fs);
	
	    uniforms.ray_origin = glGetUniformLocation(prepare_program, "ray_origin");
	    uniforms.ray_lookat = glGetUniformLocation(prepare_program, "ray_lookat");
	    uniforms.aspect = glGetUniformLocation(prepare_program, "aspect");
	
	    vs = Shader.load(getMediaPath() + "/shaders/raytracer/raytracer.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/raytracer/raytracer.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (trace_program != 0)
	        glDeleteProgram(trace_program);
	
	    trace_program = Program.link(true, vs, fs);
	
	    vs = Shader.load(getMediaPath() + "/shaders/raytracer/blit.vs.glsl", GL_VERTEX_SHADER);
	    fs = Shader.load(getMediaPath() + "/shaders/raytracer/blit.fs.glsl", GL_FRAGMENT_SHADER);
	
	    if (blit_program != 0)
	        glDeleteProgram(blit_program);
	
	    blit_program = Program.link(true, vs, fs);
	}

	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
	}
	
	public static void main(String[] args) {
		new Raytracer().run();
	}


}
