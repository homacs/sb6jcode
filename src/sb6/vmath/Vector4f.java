package sb6.vmath;


public class Vector4f extends VectorNf {

	public Vector4f(float[] fs) {
		super(fs);
	}

	public Vector4f(float x, float y, float z, float d) {
		super(new float[]{x,y,z,d});
	}

	public static Vector4f normalize(Vector4f plane) {
		return new Vector4f(_normalize(plane));
	}




}
