package sb6.vmath;

public class Vector3f extends VectorNf {
	
	public Vector3f() {
		this(new float[3]);
	}
	
	
	public Vector3f(float[] data) {
		super(data);
	}


	public Vector3f(float x, float y, float z) {
		this();
		data[0] = x;
		data[1] = y;
		data[2] = z;
	}


	public static Vector3f sub(Vector3f v1, Vector3f v2) {
		return new Vector3f(VectorNf._sub(v1.data,v2.data));
	}

	public static Vector3f normalize(Vector3f v) {
		return new Vector3f(_normalize(v));
	}

	
	
	public static Vector3f cross(final Vector3f a, final Vector3f b)
	{
	    return new Vector3f(new float[]{ a.data[1] * b.data[2] - b.data[1] * a.data[2],
	                    a.data[2] * b.data[0] - b.data[2] * a.data[0],
	                    a.data[0] * b.data[1] - b.data[0] * a.data[1]});
	}



}
