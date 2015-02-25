package sb6;

public class MathHelper {

	public static float fmodf(float x, float y) {
		int n = (int) (x / y);
		return x - n * y;
	}
}
