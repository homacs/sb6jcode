package sb6.vmath;

import sb6.BufferUtilsHelper;


public class Vector4f extends VectorNf {

	public Vector4f(float[] fs) {
		super(fs);
	}

	public Vector4f(float x, float y, float z, float d) {
		super(new float[]{x,y,z,d});
	}


	public Vector4f() {
		super(4);
	}

	public static Vector4f normalize(Vector4f plane) {
		return new Vector4f(_normalize(plane));
	}

	public static int sizeof() {
		return 4*BufferUtilsHelper.SIZEOF_FLOAT;
	}




}
