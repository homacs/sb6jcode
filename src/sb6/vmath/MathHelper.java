package sb6.vmath;

public class MathHelper {

	public static float fmodf(float x, float y) {
		int n = (int) (x / y);
		return x - n * y;
	}
	
	public static float sinf(float f) {
		return (float)Math.sin(f);
	}
	public static float cosf(float f) {
		return (float)Math.cos(f);
	}

	public static float powf(float f, float g) {
		return (float) Math.pow(f, g);
	}
	
	public static float floorf(float f) {
		return (float)Math.floor(f);
	}
}
