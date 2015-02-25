package sb6;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public class BufferUtilsHelper {
	public static final int SIZEOF_BYTE = Byte.SIZE/8;
	public static final int SIZEOF_CHAR = Character.SIZE/8;
	public static final int SIZEOF_SHORT = Short.SIZE/8;
	public static final int SIZEOF_INTEGER = Integer.SIZE/8;
	public static final int SIZEOF_LONG = Long.SIZE/8;
	public static final int SIZEOF_FLOAT = Float.SIZE/8;
	public static final int SIZEOF_DOUBLE = Double.SIZE/8;

	public static FloatBuffer createBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length*SIZEOF_FLOAT); // space for 3 vec4
		buffer.put(data);
		return buffer;
	}

	
	public static int sizeof(FloatBuffer buffer) {
		return buffer.capacity() * SIZEOF_FLOAT;
	}


	public static ByteBuffer createByteBuffer(int capacity) {
		return BufferUtils.createByteBuffer(capacity);
	}


	public static String getFixedLenString(ByteBuffer data, int length) {
		byte buf[] = new byte[length];
		for (int i = 0; i < length; i++) {
			buf[i] = data.get();
		}
		return new String(buf);
	}


	public static ByteBuffer createByteBuffer(byte[] data, int start,
			int len) {
		ByteBuffer buf = BufferUtils.createByteBuffer(len);
		buf.put(data, start, len);
		buf.rewind();
		return buf;
	}


	public static ByteBuffer createByteBuffer(byte[] data) {
		ByteBuffer buf = BufferUtils.createByteBuffer(data.length);
		buf.put(data);
		buf.rewind();
		return buf;
	}


	public static FloatBuffer createFloatBuffer(float[] data) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(data.length);
		fb.put(data);
		fb.rewind();
		return fb;
	}



}
