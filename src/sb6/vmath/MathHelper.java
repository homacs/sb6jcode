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
}
