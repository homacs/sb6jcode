package singletriubo;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

import sb6.BufferUtilsHelper;
import sb6.GLAPIHelper;
import sb6.GLUniformBlock;
import sb6.MemoryUtilHelper;
import sb6.application.Application;
import sb6.shader.Program;
import sb6.shader.Shader;


/**
 * Java implementation of movingtri.cpp
 * 
 * @author homac
 *
 */

public class SingleTriUBO extends Application {

	static enum BufferInitMethod {
		BUFFER_INIT_DIRECT,
		BUFFER_INIT_SUBMIT,
		BUFFER_INIT_MAPPED
	}

	BufferInitMethod buffer_init_method = BufferInitMethod.BUFFER_INIT_SUBMIT;
	
	private int program;
	private int vao;
	private int vbo;
	private int ubo = 0;
	private int uniform_color;

	private GLUniformBlock uniformBlock = null;

	private static final boolean useInterfaceObject = true;
	

	public SingleTriUBO() {
		super("Triangle using a uniform buffer object");
	}

	@Override
	protected void startup() {
		/*
		 * Here we use a uniform block in standard layout (std140).
		 * Standard layout follows a set of alignment rules for members of a block.
		 * The following table lists the 
		 *     - base alignment: Basic alignment for the given data type
		 *     - given offset: Start offset given by the previous data in the block
		 *     - aligned offset: Actual offset determined from the alignment required 
		 *       for its type (i.e. next aligned address after offset according to base alignment)
		 * 
		 * layout(std140) uniform TransformBlock
		 * {
		 * 		// Member 				 base alignment   given offset 	aligned offset
		 * 		float scale; 			//  4 				 0 			  0
		 * 		vec3 translation; 		// 16 				 4 			 16
		 * 		float rotation[3]; 		// 16 				28 			 32 (rotation[0])
		 * 								// 								 48 (rotation[1])
		 * 								// 								 64 (rotation[2])
		 * 		mat4 projection_matrix; // 16 				80 			 80 (column 0)
		 * 								// 								 96 (column 1)
		 * 								// 								112 (column 2)
		 * 								// 								128 (column 3)
		 * } transform;

		 */		
		String vs_source = 
	            "#version 430 core                                                 \n"+
	            "// one input - its location is defined by opengl                  \n"+
	            "in vec3 position;                                                 \n"+
	            "                                                                  \n"+
                "// Declaring a uniform block with the std140 layout               \n"+
	            "layout(std140) uniform TransformBlock {                           \n"+
	            "    float scale;            // Global scale to apply to everything\n"+
	            "    vec3 translation;       // Translation in X, Y, and Z         \n"+
	            "    float rotation[3];      // Rotation around X, Y, and Z axes   \n"+
	            "    mat4 projection_matrix; // A generalized projection matrix to \n"+
	            "                            // apply after scale and rotate       \n"+
	            "} transform;                                                      \n"+
	            "                                                                  \n"+
	            "out vec3 vs_color;                                                \n"+
	            "uniform vec3 color;                                               \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    // set given position                                         \n"+
	            "    gl_Position = vec4(position * transform.scale + transform.translation, 1.0);          \n"+
	            "    vs_color = color; // for testing purposes ... \n"+
	            "}                                                                 \n";
		String fs_source = 
	            "#version 430 core                                                 \n"+
	            "                                                                  \n"+
	            "in vec3 vs_color; // received from vs                             \n"+
	            "out vec4 color;                                                   \n"+
	            "                                                                  \n"+
	            "void main(void)                                                   \n"+
	            "{                                                                 \n"+
	            "    color = vec4(vs_color, 1.0);  // convert in vec4              \n"+
	            "}                                                                 \n";
		
		program = glCreateProgram();
		int vs = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(vs, vs_source);
		glCompileShader(vs);
		Shader.checkCompilerResult(vs, "vs");
		
		int fs = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(fs, fs_source);
		glCompileShader(fs);
		Shader.checkCompilerResult(fs, "fs");

        glAttachShader(program, vs);
        glAttachShader(program, fs);

        glLinkProgram(program);
		glValidateProgram(program);
		Program.checkLinkerResult(program);
		glDeleteShader(vs);
		glDeleteShader(fs);
		
		
		
        vao = glGenVertexArrays();
        glBindVertexArray(vao);


        
        // define the triangle vertices and their color (ugly green)
		FloatBuffer data = BufferUtilsHelper.createFloatBuffer(new float[]{
				0.25f,  -0.25f, 0.0f,
				-0.25f, -0.25f, 0.0f,
				0.25f,   0.25f, 0.0f,
		});
        int position_components = 3; // the 3 first float components belong to the position
		int data_row_size = (int)MemoryUtilHelper.offsetof(data, 3);
		
		// Generate a name for the buffer
		vbo = glGenBuffers();
		// Now bind it to the context using the GL_ARRAY_BUFFER binding point
		glBindBuffer(GL_ARRAY_BUFFER, vbo);

		// Specify the storage size and type and submit immediately.
		glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
		
		// determine vertex attribute index of glsl variable 'position' for vertex positions
		int attr_position = glGetAttribLocation(program, "position");
		if (attr_position == -1) fatal("vertex attribute index for 'position' not found");
		
		// set format of buffer object content for 'position' data 
		glVertexAttribPointer(attr_position, // Attribute 'position'
							position_components, 			// just 3 components
							GL_FLOAT, 	// Floating-point data
							false, 		// Not normalized
										// (floating-point data never is)
							data_row_size, // size of one set of data for a vertex (including all attributes!)
							MemoryUtilHelper.offsetof(data, 0)); // Offset of the first vertex component (i.e. x value)
		glEnableVertexAttribArray(attr_position);


		// determine uniform attribute index of glsl variable 'color'
		uniform_color = glGetUniformLocation(program, "color");
		if (uniform_color == -1) fatal("uniform attribute 'color' not found");

		if (!useInterfaceObject ) {
			// setup uniform block binding and stuff
			provideDataToUniformBlock();
		} else {
			// alternative abstract interface
			setupUniformBlockFacade();
		}
		
	}

	private void setupUniformBlockFacade() {
		uniformBlock = new GLUniformBlock(program, 0, "TransformBlock", new String[]{
					"scale",
					"translation",
					"rotation",
					"projection_matrix"
				});
				
		uniformBlock.set(0, 3.0f);
		uniformBlock.setVector(1, new float[]{-0.3f, 0.3f, 0.0f});
		uniformBlock.setArray(2, new float[]{ 30.0f, 40.0f, 60.0f });
		uniformBlock.setMatrix4x4(3, new float[]
					{
						1.0f, 2.0f, 3.0f, 4.0f,
						9.0f, 8.0f, 7.0f, 6.0f,
						2.0f, 4.0f, 6.0f, 8.0f,
						1.0f, 3.0f, 5.0f, 7.0f
					});
		uniformBlock.submit();	
	}

	private void provideDataToUniformBlock() {
		
		//////////////////////////////////////////////////////////////////
		//
		//  PROVIDE DATA TO THE UNIFORM BLOCK
		//
		//
		// NOTE: This example is held close to the code snippets in the book.
		//       Optimised code will be very different.
		//       TODO: add class GLUniformBlock to handle it
		
		
		//
		// retrieve indices of the uniform block members
		//
		int num_members = 4;
		IntBuffer uniformIndices = BufferUtils.createIntBuffer(num_members);
		String[] uniformNames = {
				"TransformBlock.scale",
				"TransformBlock.translation",
				"TransformBlock.rotation",
				"TransformBlock.projection_matrix"
		};
		glGetUniformIndices(program, uniformNames, uniformIndices);
		
		//
		// Get information about the uniform block members
		//
		IntBuffer uniformOffsets = BufferUtils.createIntBuffer(num_members);
		IntBuffer arrayStrides = BufferUtils.createIntBuffer(num_members);
		IntBuffer matrixStrides = BufferUtils.createIntBuffer(num_members);
		glGetActiveUniforms(program, uniformIndices, GL_UNIFORM_OFFSET, uniformOffsets);
		glGetActiveUniforms(program, uniformIndices, GL_UNIFORM_ARRAY_STRIDE, arrayStrides);
		glGetActiveUniforms(program, uniformIndices, GL_UNIFORM_MATRIX_STRIDE, matrixStrides);
		
		// Allocate some memory for our buffer (don’t forget to free it later)
		// TODO: determine the size of the buffer (not done in the book)
		ByteBuffer buffer = BufferUtils.createByteBuffer(4096); // 4k (max 64k)
		
		// We know that TransformBlock.scale is at uniformOffsets[0] bytes
		// into the block, so we can offset our buffer pointer by that value and
		// store the scale there.
		buffer.putFloat(0, 3.0f);
		

		// Next, we can initialize data for TransformBlock.translation. This is a
		// vec3, which means it consists of three floating-point values packed tightly
		// together in memory. To update this, all we need to do is find the location
		// of the first element of the vector and store three consecutive floats in
		// memory starting there.
		
		// Put three consecutive float values in memory to update a vec3
		buffer.putFloat(uniformOffsets.get(1)+0*BufferUtilsHelper.SIZEOF_FLOAT, -0.3f);
		buffer.putFloat(uniformOffsets.get(1)+1*BufferUtilsHelper.SIZEOF_FLOAT, 0.3f);
		buffer.putFloat(uniformOffsets.get(1)+2*BufferUtilsHelper.SIZEOF_FLOAT, 0.0f);

		// Now, we tackle the ARRAY rotation. We could have also used a vec3 here,
		// but for the purposes of this example, we use a three-element array to
		// demonstrate the use of the GL_UNIFORM_ARRAY_STRIDE parameter. When
		// the shared layout is used, arrays are defined as a sequence of elements
		// separated by an implementation-defined stride in bytes. This means that
		// we have to place the data at locations in the buffer defined both by
		// GL_UNIFORM_OFFSET and GL_UNIFORM_ARRAY_STRIDE. This is demonstrated here:
		
		// TransformBlock.rotations[0] is at uniformOffsets[2] bytes into
		// the buffer. Each element of the array is at a multiple of
		// arrayStrides[2] bytes past that
		float rotations[] = { 30.0f, 40.0f, 60.0f };
		int offset = uniformOffsets.get(2);
		for (int n = 0; n < 3; n++)
		{
			buffer.putFloat(offset, rotations[n]);
			offset += arrayStrides.get(2);
		}
		
		// The first column of TransformBlock.projection_matrix is at
		// uniformOffsets[3] bytes into the buffer. The columns are
		// spaced matrixStride[3] bytes apart and are essentially vec4s.
		// This is the source matrix - remember, it’s column major so
		float matrix[] =
		{
			1.0f, 2.0f, 3.0f, 4.0f,
			9.0f, 8.0f, 7.0f, 6.0f,
			2.0f, 4.0f, 6.0f, 8.0f,
			1.0f, 3.0f, 5.0f, 7.0f
		};
		for (int i = 0; i < 4; i++)
		{
			offset = uniformOffsets.get(3) + matrixStrides.get(3) * i;
			for (int j = 0; j < 4; j++)
			{
				buffer.putFloat(offset, matrix[i * 4 + j]);
				// calc offset for next float
				offset += BufferUtilsHelper.SIZEOF_FLOAT;
			}
		}
		
		//
		// Flush data to the uniform buffer object
		//
		
		// create a buffer object and submit the data we have already prepared in 'buffer'
		ubo = glGenBuffers();
		glBindBuffer(GL_UNIFORM_BUFFER, ubo);
		glBufferData(GL_UNIFORM_BUFFER, buffer, GL_STATIC_DRAW);
		// use glBufferMap or glBufferSubData later to change data in the buffer object
		
		// first retrieve the uniform block index
		int uniformBlockIndex = glGetUniformBlockIndex(program, "TransformBlock");
		
		// then assign a binding point to our uniform block
		int uniformBlockBindingIndex = 0;
		glUniformBlockBinding(program, uniformBlockIndex, uniformBlockBindingIndex);
		
		// bind buffer to the binding point to make the data in the buffer
		// appear in the uniform block
		glBindBufferBase(GL_UNIFORM_BUFFER, uniformBlockBindingIndex, ubo);
	}


	@Override
	protected void shutdown() {
		if (uniformBlock != null){
			uniformBlock.delete();
			uniformBlock = null;
		} else {
			glDeleteBuffers(ubo);
		}
		glDeleteBuffers(vbo);
		glDeleteVertexArrays(vao);
		glDeleteProgram(program);
	}

	@Override
	protected void render(double currentTime) {
		GLAPIHelper.glClearBuffer4f(GL_COLOR, 0, (float)Math.sin(currentTime) * 0.5f + 0.5f,(float)Math.cos(currentTime) * 0.5f + 0.5f, 0f, 1f);
		// Use the program object we created earlier for rendering
		glUseProgram(program);
		// set uniform
		glUniform3f(uniform_color, 1.0f, 0.0f, 0.0f);
		
		// Draw one triangle
		glDrawArrays(GL_TRIANGLES, 0, 3);
	}

	
	public static void main (String[] args) {
		Application app = new SingleTriUBO();
		app.run();
	}

}
