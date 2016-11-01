package sb6;

import static org.lwjgl.opengl.GL30.glClearBuffer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;

/**
 * This class mainly provides a cache for native I/O buffers.
 */
public class GLAPIHelper {
	static class APICache {
		FloatBuffer scalarf = BufferUtils.createFloatBuffer(1);
		IntBuffer scalari = BufferUtils.createIntBuffer(1);
		FloatBuffer vec4f = BufferUtils.createFloatBuffer(4);
		IntBuffer vec4i = BufferUtils.createIntBuffer(4);

		static ThreadLocal<APICache> tls = new ThreadLocal<APICache>();
		public static FloatBuffer getVec4f() {
			return getCache().vec4f;
		}
		private static APICache getCache() {
			APICache cache = tls.get();
			if (cache== null) {
				cache = new APICache();
				tls.set(cache);
			}
			return cache;
		}
		public static FloatBuffer getScalarF() {
			return getCache().scalarf;
		}
		public static IntBuffer getScalarI() {
			return getCache().scalari;
		}
		public static IntBuffer getVec4i() {
			return getCache().vec4i;
		}
	}

	
	
	public static void glClearBuffer4f(int bits, int drawbuffer, float r, float g, float b, float a) {
		FloatBuffer vec4f = APICache.getVec4f();
		vec4f.put(0, r);
		vec4f.put(1, g);
		vec4f.put(2, b);
		vec4f.put(3, a);
		glClearBuffer(bits, drawbuffer, vec4f);
	}

	public static void glClearBuffer4i(int bits, int drawbuffer, int r, int g, int b, int a) {
		IntBuffer vec4i = APICache.getVec4i();
		vec4i.put(0, r);
		vec4i.put(1, g);
		vec4i.put(2, b);
		vec4i.put(3, a);
		glClearBuffer(bits, drawbuffer, vec4i);
	}

	public static void glClearBuffer1f(int bits, int drawbuffer, float val) {
		final FloatBuffer scalarf = APICache.getScalarF();
		scalarf.put(0, val);
		scalarf.rewind();
		glClearBuffer(bits, drawbuffer, scalarf);
	}

	public static void glClearBuffer1i(int bits, int drawbuffer, int val) {
		final IntBuffer scalari = APICache.getScalarI();
		scalari.put(0, val);
		scalari.rewind();
		glClearBuffer(bits, drawbuffer, scalari);
	}

}
