package sb6.vmath;

public class Vector4f extends VectorNf {

	public Vector4f(float[] fs) {
		super(fs);
	}

	public Vector4f(float x, float y, float z, float d) {
		super(new float[]{x,y,z,d});
	}

}
