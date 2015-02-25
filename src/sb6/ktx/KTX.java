package sb6.ktx;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL42.*;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import sb6.BufferUtilsHelper;

public class KTX {

	public static class Header {
		byte[] identifier = new byte[12]; // 12 byte
		int endianness;
		int gltype;
		int gltypesize;
		int glformat;
		int glinternalformat;
		int glbaseinternalformat;
		int pixelwidth;
		int pixelheight;
		int pixeldepth;
		int arrayelements;
		int faces;
		int miplevels;
		int keypairbytes;

		public boolean read(FileInputStream fp) throws IOException {
			DataInputStream in = new DataInputStream(fp);
			in.read(identifier);
			endianness = in.readInt();
			gltype = in.readInt();
			gltypesize = in.readInt();
			glformat = in.readInt();
			glinternalformat = in.readInt();
			glbaseinternalformat = in.readInt();
			pixelwidth = in.readInt();
			pixelheight = in.readInt();
			pixeldepth = in.readInt();
			arrayelements = in.readInt();
			faces = in.readInt();
			miplevels = in.readInt();
			keypairbytes = in.readInt();
			return true;
		}
	}

	// union keyvaluepair
	// {
	// unsigned int size;
	// unsigned char rawbytes[4];
	// };

	static byte[] identifier = new byte[] { (byte) 0xAB, 0x4B, 0x54, 0x58,
			0x20, 0x31, 0x31, (byte) 0xBB, 0x0D, 0x0A, 0x1A, 0x0A };

	public static int swap32(int value) {
		int b1 = (value >> 0) & 0xff;
		int b2 = (value >> 8) & 0xff;
		int b3 = (value >> 16) & 0xff;
		int b4 = (value >> 24) & 0xff;

		return b1 << 24 | b2 << 16 | b3 << 8 | b4 << 0;
	}

	public static short swap(short value) {
		int b1 = value & 0xff;
		int b2 = (value >> 8) & 0xff;

		return (short) (b1 << 8 | b2 << 0);
	}

	public static int calculate_stride(Header h, int width) {
		int pad = 4;
		return calculate_stride(h, width, pad);
	}

	
	
	public static int calculate_stride(Header h, int width, int pad) {
		int channels = 0;

		switch (h.glbaseinternalformat) {
		case GL_RED:
			channels = 1;
			break;
		case GL_RG:
			channels = 2;
			break;
		case GL_BGR:
		case GL_RGB:
			channels = 3;
			break;
		case GL_BGRA:
		case GL_RGBA:
			channels = 4;
			break;
		}

		int stride = h.gltypesize * channels * width;

		stride = (stride + (pad - 1)) & ~(pad - 1);

		return stride;
	}

	public static int calculate_face_size(Header h) {
		int stride = calculate_stride(h, h.pixelwidth);

		return stride * h.pixelheight;
	}

	public static int load(String filename) throws IOException {
		return load(filename, 0);
	}

	public static int load(String filename, int tex) throws IOException {
		FileInputStream fp;
		int retval = 0;
		Header header = new Header();
		long data_start, data_end;
		byte[] data;
		int target = GL_NONE;

		fp = new FileInputStream(filename);

		if (!header.read(fp)) {
			fp.close();
			throw new IOException("Failed reading header");
		}

		if (!Arrays.equals(header.identifier, identifier)) {
			fp.close();
			throw new IOException("Magic number test failed. File corrupted.");
		}

		if (header.endianness == 0x04030201) {
			// No swap needed
		} else if (header.endianness == 0x01020304) {
			// Swap needed
			header.endianness = swap32(header.endianness);
			header.gltype = swap32(header.gltype);
			header.gltypesize = swap32(header.gltypesize);
			header.glformat = swap32(header.glformat);
			header.glinternalformat = swap32(header.glinternalformat);
			header.glbaseinternalformat = swap32(header.glbaseinternalformat);
			header.pixelwidth = swap32(header.pixelwidth);
			header.pixelheight = swap32(header.pixelheight);
			header.pixeldepth = swap32(header.pixeldepth);
			header.arrayelements = swap32(header.arrayelements);
			header.faces = swap32(header.faces);
			header.miplevels = swap32(header.miplevels);
			header.keypairbytes = swap32(header.keypairbytes);
		} else {
			fp.close();
			throw new IOException(
					"Couldn't identify endianess. File corrupted.");
		}

		// Guess target (texture type)
		if (header.pixelheight == 0) {
			if (header.arrayelements == 0) {
				target = GL_TEXTURE_1D;
			} else {
				target = GL_TEXTURE_1D_ARRAY;
			}
		} else if (header.pixeldepth == 0) {
			if (header.arrayelements == 0) {
				if (header.faces == 0) {
					target = GL_TEXTURE_2D;
				} else {
					target = GL_TEXTURE_CUBE_MAP;
				}
			} else {
				if (header.faces == 0) {
					target = GL_TEXTURE_2D_ARRAY;
				} else {
					target = GL_TEXTURE_CUBE_MAP_ARRAY;
				}
			}
		} else {
			target = GL_TEXTURE_3D;
		}

		// Check for insanity...
		if (target == GL_NONE || // Couldn't figure out target
				(header.pixelwidth == 0) || // Texture has no width???
				(header.pixelheight == 0 && header.pixeldepth != 0)) // Texture
																		// has
																		// depth
																		// but
																		// no
																		// height???
		{
			fp.close();
			throw new IOException("Failed reading header");
		}

		if (tex == 0) {
			tex = glGenTextures();
		}

		glBindTexture(target, tex);

		// determine data start and size
		data_start = fp.getChannel().position() + header.keypairbytes;
		data_end = fp.getChannel().size();

		fp.getChannel().position(data_start);

		data = new byte[(int) (data_end - data_start)];
		Arrays.fill(data, (byte) 0);

		fp.read(data);
		fp.close();

		if (header.miplevels == 0) {
			header.miplevels = 1;
		}

		switch (target) {
		case GL_TEXTURE_1D:
			glTexStorage1D(GL_TEXTURE_1D, header.miplevels,
					header.glinternalformat, header.pixelwidth);
			glTexSubImage1D(GL_TEXTURE_1D, 0, 0, header.pixelwidth,
					header.glformat, header.glinternalformat,
					BufferUtilsHelper.createByteBuffer(data));
			break;
		case GL_TEXTURE_2D:
			glTexStorage2D(GL_TEXTURE_2D, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.pixelheight);
			{
				int ptr = 0;
				int height = header.pixelheight;
				int width = header.pixelwidth;
				glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
				for (int i = 0; i < header.miplevels; i++) {
					int stride_len = calculate_stride(header, width, 1);
					glTexSubImage2D(GL_TEXTURE_2D, i, 0, 0, width, height,
							header.glformat, header.gltype,
							BufferUtilsHelper.createByteBuffer(data, ptr,
									height*stride_len));
					ptr += height * stride_len;
					height >>= 1;
					width >>= 1;
					if (height == 0)
						height = 1;
					if (width == 0)
						width = 1;
				}
			}
			break;
		case GL_TEXTURE_3D:
			glTexStorage3D(GL_TEXTURE_3D, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.pixelheight, header.pixeldepth);
			glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0, header.pixelwidth,
					header.pixelheight, header.pixeldepth, header.glformat,
					header.gltype, BufferUtilsHelper.createByteBuffer(data));
			break;
		case GL_TEXTURE_1D_ARRAY:
			glTexStorage2D(GL_TEXTURE_1D_ARRAY, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.arrayelements);
			glTexSubImage2D(GL_TEXTURE_1D_ARRAY, 0, 0, 0, header.pixelwidth,
					header.arrayelements, header.glformat, header.gltype,
					BufferUtilsHelper.createByteBuffer(data));
			break;
		case GL_TEXTURE_2D_ARRAY:
			glTexStorage3D(GL_TEXTURE_2D_ARRAY, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.pixelheight, header.arrayelements);
			glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, header.pixelwidth,
					header.pixelheight, header.arrayelements, header.glformat,
					header.gltype, BufferUtilsHelper.createByteBuffer(data));
			break;
		case GL_TEXTURE_CUBE_MAP:
			glTexStorage2D(GL_TEXTURE_CUBE_MAP, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.pixelheight);
			// glTexSubImage3D(GL_TEXTURE_CUBE_MAP, 0, 0, 0, 0, h.pixelwidth,
			// h.pixelheight, h.faces, h.glformat, h.gltype, data);
			{
				int face_size = calculate_face_size(header);
				for (int i = 0; i < header.faces; i++) {
					glTexSubImage2D(
							GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
							0,
							0,
							0,
							header.pixelwidth,
							header.pixelheight,
							header.glformat,
							header.gltype,
							BufferUtilsHelper.createByteBuffer(data, face_size
									* i, face_size));
				}
			}
			break;
		case GL_TEXTURE_CUBE_MAP_ARRAY:
			glTexStorage3D(GL_TEXTURE_CUBE_MAP_ARRAY, header.miplevels,
					header.glinternalformat, header.pixelwidth,
					header.pixelheight, header.arrayelements);
			glTexSubImage3D(GL_TEXTURE_CUBE_MAP_ARRAY, 0, 0, 0, 0,
					header.pixelwidth, header.pixelheight, header.faces
							* header.arrayelements, header.glformat,
					header.gltype, BufferUtilsHelper.createByteBuffer(data));
			break;
		default: // Should never happen
			throw new Error("Unknown texture target type");
		}

		if (header.miplevels == 1) {
			glGenerateMipmap(target);
		}

		retval = tex;

		return retval;
	}

	
	// TOTO: remove?
	// boolean save(String filename, int target, int tex)
	// {
	// header h;
	//
	// memset(&h, 0, sizeof(h));
	// memcpy(h.identifier, identifier, sizeof(identifier));
	// h.endianness = 0x04030201;
	//
	// glBindTexture(target, tex);
	//
	// glGetTexLevelParameteriv(target, 0, GL_TEXTURE_WIDTH, (GLint
	// *)&h.pixelwidth);
	// glGetTexLevelParameteriv(target, 0, GL_TEXTURE_HEIGHT, (GLint
	// *)&h.pixelheight);
	// glGetTexLevelParameteriv(target, 0, GL_TEXTURE_DEPTH, (GLint
	// *)&h.pixeldepth);
	//
	// return true;
	// }

}
