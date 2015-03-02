package sb6.sbm;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*; // buffer management
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL42.*;
import static sb6.BufferUtilsHelper.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.lwjgl.LWJGLUtil;

import sb6.BufferUtilsHelper;
import sb6.application.Application;

public class SBMObject {
	private static final int MAX_SUB_OBJECTS = 256;
	private static final int SB6M_MAGIC = SB6M_FOURCC('S', 'B', '6', 'M');
	private static final int SB6M_CHUNK_TYPE_INDEX_DATA = SB6M_FOURCC('I', 'N',
			'D', 'X');
	private static final int SB6M_CHUNK_TYPE_VERTEX_DATA = SB6M_FOURCC('V',
			'R', 'T', 'X');
	private static final int SB6M_CHUNK_TYPE_VERTEX_ATTRIBS = SB6M_FOURCC('A',
			'T', 'R', 'B');
	private static final int SB6M_CHUNK_TYPE_SUB_OBJECT_LIST = SB6M_FOURCC('O',
			'L', 'S', 'T');
	private static final int SB6M_CHUNK_TYPE_COMMENT = SB6M_FOURCC('C', 'M',
			'N', 'T');
	private static final int SB6M_VERTEX_ATTRIB_FLAG_NORMALIZED = 0x00000001;
	private static final int SB6M_VERTEX_ATTRIB_FLAG_INTEGER = 0x00000002;

	int vertex_buffer;
	int index_buffer;
	int vao;
	int num_indices;
	int index_type;

	int num_sub_objects;
	SBMSubObjectDecl sub_object[] = new SBMSubObjectDecl[MAX_SUB_OBJECTS];

	public void load(String filename) throws IOException {

		free();

		//
		// read raw data
		//
		FileInputStream infile = new FileInputStream(new File(filename));
		long filesize;
		ByteBuffer data;
		filesize = infile.getChannel().size();
		if (filesize > Integer.MAX_VALUE) {
			infile.close();
			throw new Error("File too big, can't allocate buffers of that size");
		}

		data = BufferUtilsHelper.createByteBuffer((int) filesize);
		byte[] buffer = new byte[4096];
		for (int nread = infile.read(buffer); nread > 0; nread = infile
				.read(buffer)) {
			data.put(buffer, 0, nread);
		}
		// done reading
		infile.close();
		// reset buffer pointer to beginning
		data.rewind();

		//
		// read headers
		//

		// read the file header
		SBMHeader header = new SBMHeader(data);

		// temp objects
		SBMVertexAttribChunk vertex_attrib_chunk = null;
		SBMChunkVertexData vertex_data_chunk = null;
		SBMChunkIndexData index_data_chunk = null;
		SBMChunkSubObjectList sub_object_chunk = null;

		SBMChunkHeader chunk = new SBMChunkHeader();

		// read all chunk headers
		for (int i = 0; i < header.num_chunks; i++) {
			chunk.init(data);
			if (chunk.chunk_type == SB6M_CHUNK_TYPE_VERTEX_ATTRIBS) {
				vertex_attrib_chunk = new SBMVertexAttribChunk(chunk, data);
			} else if (chunk.chunk_type == SB6M_CHUNK_TYPE_VERTEX_DATA) {
				vertex_data_chunk = new SBMChunkVertexData(chunk, data);
			} else if (chunk.chunk_type == SB6M_CHUNK_TYPE_INDEX_DATA) {
				index_data_chunk = new SBMChunkIndexData(chunk, data);
			} else if (chunk.chunk_type == SB6M_CHUNK_TYPE_SUB_OBJECT_LIST) {
				sub_object_chunk = new SBMChunkSubObjectList(chunk, data);
			} else {
				// goto failed;
			}
		}

		// failed:

		if (sub_object_chunk != null) {
			if (sub_object_chunk.count > MAX_SUB_OBJECTS) {
				sub_object_chunk.count = MAX_SUB_OBJECTS;
			}

			for (int i = 0; i < sub_object_chunk.count; i++) {
				sub_object[i] = sub_object_chunk.sub_object[i];
			}

			num_sub_objects = sub_object_chunk.count;
		} else {
			sub_object[0] = new SBMSubObjectDecl();
			sub_object[0].first = 0;
			sub_object[0].count = vertex_data_chunk.total_vertices;
			num_sub_objects = 1;
		}

		// done interpreting headers
		data.rewind();

		vertex_buffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertex_buffer);
		glBufferData(GL_ARRAY_BUFFER, vertex_data_chunk.data_size,
				(ByteBuffer) data.position(vertex_data_chunk.data_offset),
				GL_STATIC_DRAW);

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		for (int i = 0; i < vertex_attrib_chunk.attrib_count; i++) {
			SBMVertexAttribDecl attrib_decl = vertex_attrib_chunk.attrib_data[i];
			glVertexAttribPointer(
					i,
					attrib_decl.size,
					attrib_decl.type,
					(0 != (attrib_decl.flags & SB6M_VERTEX_ATTRIB_FLAG_NORMALIZED)),
					attrib_decl.stride, (long) attrib_decl.data_offset);
			glEnableVertexAttribArray(i);
		}

		if (index_data_chunk != null) {
			index_buffer = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, index_buffer);
			glBufferData(
					GL_ELEMENT_ARRAY_BUFFER,
					index_data_chunk.index_count
							* (index_data_chunk.index_type == GL_UNSIGNED_SHORT ? SIZEOF_SHORT
									: SIZEOF_BYTE),
					(ByteBuffer) data
							.position(index_data_chunk.index_data_offset),
					GL_STATIC_DRAW);
			num_indices = index_data_chunk.index_count;
			index_type = index_data_chunk.index_type;
		} else {
			num_indices = vertex_data_chunk.total_vertices;
		}

		glBindVertexArray(0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	public void free() {
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vertex_buffer);
		glDeleteBuffers(index_buffer);

		vao = 0;
		vertex_buffer = 0;
		index_buffer = 0;
		num_indices = 0;
	}

	public void render(int instance_count, int base_instance) {
		render_sub_object(0, instance_count, base_instance);
	}

	public void render(int instance_count) {
		int base_instance = 0;
		render(instance_count, base_instance);
	}

	public void render() {
		int instance_count = 1, base_instance = 0;
		render(instance_count, base_instance);
	}

	public void render_sub_object(int object_index, int instance_count,
			int base_instance) {
		glBindVertexArray(vao);

		if (LWJGLUtil.getPlatform() == LWJGLUtil.Platform.MACOSX) {

			if (index_buffer != 0) {
				glDrawElementsInstanced(GL_TRIANGLES, num_indices, index_type,
						0, instance_count);
			} else {
				glDrawArraysInstanced(GL_TRIANGLES,
						sub_object[object_index].first,
						sub_object[object_index].count, instance_count);
			}
		} else {
			if (index_buffer != 0) {
				glDrawElementsInstancedBaseInstance(GL_TRIANGLES, num_indices,
						index_type, 0, instance_count, base_instance);
			} else {
				glDrawArraysInstancedBaseInstance(GL_TRIANGLES,
						sub_object[object_index].first,
						sub_object[object_index].count, instance_count,
						base_instance);
			}
		}
	}

	static void err(String errmsg) {
		System.err.println(errmsg);
		System.exit(-1);
	}

	public static void main(String[] args) throws IOException {
		// small test
		if (SB6M_MAGIC != 0x4d364253) {
			err("SB6M_FOURCC not working correct");
		}
		Application testApp = new Application("object loader test") {

			private SBMObject myobject;

			@Override
			protected void startup() throws Throwable {
				myobject = new SBMObject();
				myobject.load("../sb6code/bin/media/objects/sphere.sbm");
			}

			@Override
			protected void render(double currentTime) {
				myobject.render();
			}

			@Override
			protected void shutdown() {
			}
		};
		testApp.run();
	}

	private String readFixedLenString(ByteBuffer data, int length)
			throws IOException {
		byte[] buffer = new byte[length];
		data.get(buffer);
		return new String(buffer);
	}

	public class SBMSubObjectDecl {
		int first;
		int count;

		public SBMSubObjectDecl(ByteBuffer data) throws IOException {
			first = data.getInt();
			count = data.getInt();
		}

		public SBMSubObjectDecl() {
		}

	}

	public class SBMVertexAttribDecl {
		String name; // 64 bytes
		int size;
		int type;
		int stride;
		int flags;
		int data_offset;

		public SBMVertexAttribDecl(ByteBuffer data) throws IOException {
			name = readFixedLenString(data, 64);
			size = data.getInt();
			type = data.getInt();
			stride = data.getInt();
			flags = data.getInt();
			data_offset = data.getInt();
		}
	}

	static int SB6M_FOURCC(char a, char b, char c, char d) {
		return (((int) (a) << 0) | ((int) (b) << 8) | ((int) (c) << 16) | ((int) (d) << 24));
	}

	public class SBMChunkHeader {
		int chunk_type;
		int size;

		public void init(ByteBuffer data) throws IOException {
			chunk_type = data.getInt();
			size = data.getInt();
		}

	}

	public class SBMChunkSubObjectList {
		SBMChunkHeader header;
		int count;
		SBMSubObjectDecl[] sub_object;

		public SBMChunkSubObjectList(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			init(header, data);
		}

		public void init(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			this.header = header;
			count = data.getInt();
			sub_object = new SBMSubObjectDecl[count];
			for (int i = 0; i < count; i++) {
				sub_object[i] = new SBMSubObjectDecl(data);
			}
		}

	}

	public class SBMChunkIndexData {
		SBMChunkHeader header;
		int index_type;
		int index_count;
		int index_data_offset;

		public SBMChunkIndexData(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			init(header, data);
		}

		public void init(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			this.header = header;
			index_type = data.getInt();
			index_count = data.getInt();
			index_data_offset = data.getInt();
		}

	}

	public class SBMChunkVertexData {
		SBMChunkHeader header;
		int data_size;
		int data_offset;
		int total_vertices;

		public SBMChunkVertexData(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			init(header, data);
		}

		public void init(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			this.header = header;
			data_size = data.getInt();
			data_offset = data.getInt();
			total_vertices = data.getInt();
		}

	}

	public class SBMVertexAttribChunk {
		SBMChunkHeader header;
		int attrib_count;
		SBMVertexAttribDecl attrib_data[];

		public SBMVertexAttribChunk(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			init(header, data);
		}

		public void init(SBMChunkHeader header, ByteBuffer data)
				throws IOException {
			this.header = header;
			attrib_count = data.getInt();
			attrib_data = new SBMVertexAttribDecl[attrib_count];
			for (int i = 0; i < attrib_count; i++) {
				attrib_data[i] = new SBMVertexAttribDecl(data);
			}
		}

	}

	public class SBMHeader {
		int magic;
		int size;
		int num_chunks;
		int flags;

		public SBMHeader(ByteBuffer data) throws IOException {
			init(data);
		}

		public void init(ByteBuffer data) throws IOException {
			magic = data.getInt();
			size = data.getInt();
			num_chunks = data.getInt();
			flags = data.getInt();
		}

	}

}
