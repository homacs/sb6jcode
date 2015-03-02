package sb6.vmath;

public class VectorNf {

	protected float[] data;

	public VectorNf(float[] fs) {
		data = fs;
	}

	protected static float[] _sub(float[] v1, float[] v2) {
		float[] result = new float[v1.length];
        for (int n = 0; n < v1.length; n++)
            result[n] = v1[n] - v2[n];
        return result;
	}

	protected static float[] _div(float[] v, float divisor) {
		float[] result = new float[v.length];
		for (int i = 0; i < v.length; i++) {
			result[i] = v[i] / divisor;
		}
		return result;
	}

	protected static float[] _normalize(final VectorNf v) {
		return _div(v.data, length(v));
	}
	
	public VectorNf mul(float f) {
		for (int i = 0 ; i < data.length; i++) {
			data[i] *= f;
		}
		return this;
	}

	/** Returns the length of the vector, not the size of the underlying array.
	 * 
	 * @param v
	 * @return
	 */
	public static float length(VectorNf v)
	{
	    double result = 0.0f;

	    for (int i = 0; i < v.data.length; ++i)
	    {
	        result += v.data[i] * v.data[i];
	    }

	    return (float) Math.sqrt(result);
	}


	public float get(int i) {
		return data[i];
	}

	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		int i;
		for (i = 0; i < data.length -1; i++) {
			s.append(data[i]);
			s.append(", ");
		}
		s.append(data[i]);
		return s.toString();
	}



}
