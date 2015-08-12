package sb6;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;

public class BufferUtilsHelper {
	public static final int SIZEOF_BYTE = Byte.SIZE/8;
	public static final int SIZEOF_CHAR = Character.SIZE/8;
	public static final int SIZEOF_SHORT = Short.SIZE/8;
	public static final int SIZEOF_INTEGER = Integer.SIZE/8;
	public static final int SIZEOF_LONG = Long.SIZE/8;
	public static final int SIZEOF_FLOAT = Float.SIZE/8;
	public static final int SIZEOF_DOUBLE = Double.SIZE/8;
	
	/** Null pointer in case there is no function signature which supports null. */
	public static final long NULL = 0L;

	
	public static int sizeof(FloatBuffer buffer) {
		return buffer.capacity() * SIZEOF_FLOAT;
	}


	public static String getFixedLenString(ByteBuffer data, int length) {
		byte buf[] = new byte[length];
		for (int i = 0; i < length; i++) {
			buf[i] = data.get();
		}
		return new String(buf);
	}


	
	/**
	 * Returns a byte buffer of length len initialised with the elements of data
	 * in the range [start, start+len].
	 * Iterator of the created buffer refers to the first element.
	 * @param data 
	 * @return
	 */
	public static ByteBuffer createByteBuffer(byte[] data, int start,
			int len) {
		ByteBuffer buf = BufferUtils.createByteBuffer(len);
		buf.put(data, start, len);
		buf.rewind();
		return buf;
	}


	public static ByteBuffer createByteBuffer(int capacity) {
		return BufferUtils.createByteBuffer(capacity);
	}

	/**
	 * Returns a byte buffer of length data.length initialised with data.
	 * Iterator of the created buffer refers to the first element.
	 * @param data 
	 * @return
	 */
	public static ByteBuffer createByteBuffer(byte[] data) {
		ByteBuffer buf = BufferUtils.createByteBuffer(data.length);
		buf.put(data);
		buf.rewind();
		return buf;
	}


	/**
	 * Returns a float buffer of length data.length initialised with data.
	 * Iterator of the created buffer refers to the first element.
	 * @param data 
	 * @return
	 */
	public static FloatBuffer createFloatBuffer(float[] data) {
		FloatBuffer fb = BufferUtils.createFloatBuffer(data.length);
		fb.put(data);
		fb.rewind();
		return fb;
	}


	/**
	 * Returns a short buffer of length data.length initialised with data.
	 * Iterator of the created buffer refers to the first element.
	 * @param data 
	 * @return
	 */
	public static ShortBuffer createShortBuffer(short[] data) {
		ShortBuffer sb = BufferUtils.createShortBuffer(data.length);
		sb.put(data);
		sb.rewind();
		return sb;
	}

	/**
	 * Returns a int buffer of length data.length initialised with data.
	 * Iterator of the created buffer refers to the first element.
	 * @param data 
	 * @return
	 */
	public static IntBuffer createIntBuffer(int[] data) {
		IntBuffer sb = BufferUtils.createIntBuffer(data.length);
		sb.put(data);
		sb.rewind();
		return sb;
	}



}
