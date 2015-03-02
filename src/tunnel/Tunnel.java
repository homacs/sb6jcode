package tunnel;





import sb6.application.Application;
import sb6.ktx.KTX;
import sb6.shader.Shader;
import sb6.vmath.Matrix4x4f;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;


public class Tunnel extends Application {
 
	private int render_prog;
	private int          render_vao;
    class Uniforms
    {
        int       mvp;
        int       offset;
    } 
    Uniforms uniforms = new Uniforms();

    private int          tex_wall;
    private int          tex_ceiling;
    private int          tex_floor;
	private int filtering;


	public Tunnel() {
		super("OpenGL SuperBible - Tunnel");
	}

	
	
	@Override
	protected void startup() throws Throwable {
        int  vs, fs;

        final String vs_source =
            "#version 420 core                                                      \n"+
            "                                                                       \n"+
            "out VS_OUT                                                             \n"+
            "{                                                                      \n"+
            "    vec2 tc;                                                           \n"+
            "} vs_out;                                                              \n"+
            "                                                                       \n"+
            "uniform mat4 mvp;                                                      \n"+
            "uniform float offset;                                                  \n"+
            "                                                                       \n"+
            "void main(void)                                                        \n"+
            "{                                                                      \n"+
            "    const vec2[4] position = vec2[4](vec2(-0.5, -0.5),                 \n"+
            "                                     vec2( 0.5, -0.5),                 \n"+
            "                                     vec2(-0.5,  0.5),                 \n"+
            "                                     vec2( 0.5,  0.5));                \n"+
            "    vs_out.tc = (position[gl_VertexID].xy + vec2(offset, 0.5)) * vec2(30.0, 1.0);                  \n"+
            "    gl_Position = mvp * vec4(position[gl_VertexID], 0.0, 1.0);       \n"+
            "}                                                                      \n";

        final String fs_source =
            "#version 420 core                                                      \n"+
            "                                                                       \n"+
            "layout (location = 0) out vec4 color;                                  \n"+
            "                                                                       \n"+
            "in VS_OUT                                                              \n"+
            "{                                                                      \n"+
            "    vec2 tc;                                                           \n"+
            "} fs_in;                                                               \n"+
            "                                                                       \n"+
            "layout (binding = 0) uniform sampler2D tex;                            \n"+
            "                                                                       \n"+
            "void main(void)                                                        \n"+
            "{                                                                      \n"+
            "    color = texture(tex, fs_in.tc);                                    \n"+
            "}                                                                      \n";
        vs = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vs, vs_source);
        glCompileShader(vs);
        Shader.checkCompilerResult(vs,"vs");
        
        fs = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fs, fs_source);
        glCompileShader(fs);
        Shader.checkCompilerResult(fs,"fs");

        render_prog = glCreateProgram();
        glAttachShader(render_prog, vs);
        glAttachShader(render_prog, fs);
        glLinkProgram(render_prog);

        glDeleteShader(vs);
        glDeleteShader(fs);

        Shader.checkLinkerResult(render_prog);
        
        uniforms.mvp = glGetUniformLocation(render_prog, "mvp");
        uniforms.offset = glGetUniformLocation(render_prog, "offset");

        render_vao = glGenVertexArrays();
        glBindVertexArray(render_vao);

        tex_wall = KTX.load(getMediaPath() + "textures/brick.ktx");
        tex_ceiling = KTX.load(getMediaPath() + "textures/ceiling.ktx");
        tex_floor = KTX.load(getMediaPath() + "textures/floor.ktx");

        int i;
        int textures[] = { tex_floor, tex_wall, tex_ceiling };

        for (i = 0; i < 3; i++)
        {
            glBindTexture(GL_TEXTURE_2D, textures[i]);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }

        glBindVertexArray(render_vao);

	}


	@Override
	protected void render(double currentTime) throws Throwable {

        float t = (float)currentTime;

        glViewport(0, 0, info.windowWidth, info.windowHeight);
        glClearBuffer4f(GL_COLOR, 0, 0.0f, 0.0f, 0.0f, 0.0f);

        glUseProgram(render_prog);

        Matrix4x4f proj_matrix = Matrix4x4f.perspective(60.0f,
                                                     (float)info.windowWidth / (float)info.windowHeight,
                                                     0.1f, 100.0f);

        glUniform1f(uniforms.offset, t * 0.003f);

        int i;
        int textures[] = { tex_wall, tex_floor, tex_wall, tex_ceiling };
        for (i = 0; i < 4; i++)
        {
        	Matrix4x4f mv_matrix = Matrix4x4f.rotate(90.0f * (float)i, 0.0f, 0.0f, 1.0f) 
        			.mul (Matrix4x4f.translate(-0.5f, 0.0f, -10.0f))
        			.mul (Matrix4x4f.rotate(90.0f, 0.0f, 1.0f, 0.0f))
        			.mul (Matrix4x4f.scale(30.0f, 1.0f, 1.0f));
        	Matrix4x4f mvp = Matrix4x4f.multiply(proj_matrix, mv_matrix);

            glUniformMatrix4(uniforms.mvp, false, mvp.toFloatBuffer());

            glBindTexture(GL_TEXTURE_2D, textures[i]);
            
            // The tunnel is modelled as a 2D triangle strip.
            // The vertices and texcoords are generated by the 
            // vertex shader.
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
	}

	public static void main(String[] args) {
		Tunnel app = new Tunnel();
		app.run();
	}



	@Override
	protected void shutdown() throws Throwable {
		// TODO Auto-generated method stub
		
	}



	@Override
	protected void onKey(int key, int action) {
        if (action != 0)
        {
            switch (key)
            {
            case 'T':
                filtering = (filtering+1)%6;

				int minFilter;
				int magFilter;
				switch(filtering) {
                    case 0:
                        minFilter = GL_LINEAR_MIPMAP_LINEAR;
                        magFilter = GL_LINEAR;
                        break;
                    case 1:
                        minFilter = GL_NEAREST;
                        magFilter = GL_NEAREST;
                        break;
                    case 2:
                        minFilter = GL_NEAREST_MIPMAP_NEAREST;
                        magFilter = GL_NEAREST;
                        break;
                    case 3:
                        minFilter = GL_NEAREST_MIPMAP_LINEAR;
                        magFilter = GL_NEAREST;
                        break;
                    case 4:
                        minFilter = GL_LINEAR;
                        magFilter = GL_LINEAR;
                        break;
                    case 5:
                        minFilter = GL_LINEAR_MIPMAP_NEAREST;
                        magFilter = GL_LINEAR;
                        break;
                   	default: 
                        minFilter = GL_LINEAR_MIPMAP_LINEAR;
                        magFilter = GL_LINEAR;
                    }
				
                    int textures[] = { tex_floor, tex_wall, tex_ceiling };
                    for (int i = 0; i < 3; i++)
                    {
                        glBindTexture(GL_TEXTURE_2D, textures[i]);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter);
                        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter);
                    }
                    break;
            }
        }

	}

}
