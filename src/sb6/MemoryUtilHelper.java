package sb6;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryUtil;

public class MemoryUtilHelper {

	public static void memcpy(FloatBuffer src, ByteBuffer trg,
			int buffer_size) {
		MemoryUtil.memCopy(MemoryUtil.memAddress(src), MemoryUtil.memAddress(trg), buffer_size);
	}

	public static long offsetof(FloatBuffer data, int i) {
		long target_addr = MemoryUtil.memAddress(data, i);
		long base_addr = MemoryUtil.memAddress(data);
		return target_addr - base_addr;
	}

}
