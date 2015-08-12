package sb6.vmath;

public class VectorNi {

	protected int[] data;

	public VectorNi(int[] fs) {
		data = fs;
	}

	public VectorNi(int i) {
        int n;
        for (n = 0; n < data.length; n++)
        {
            data[n] = i;
        }
	}

	protected static int[] _sub(int[] v1, int[] v2) {
		int[] result = new int[v1.length];
        for (int n = 0; n < v1.length; n++)
            result[n] = v1[n] - v2[n];
        return result;
	}

	protected static int[] _div(int[] v, float divisor) {
		int[] result = new int[v.length];
		for (int i = 0; i < v.length; i++) {
			result[i] = (int) (v[i] / divisor);
		}
		return result;
	}

	protected static int[] _normalize(final VectorNi v) {
		return _div(v.data, length(v));
	}
	
	public VectorNi mul(float f) {
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
	public static float length(VectorNi v)
	{
	    double result = 0.0f;

	    for (int i = 0; i < v.data.length; ++i)
	    {
	        result += v.data[i] * v.data[i];
	    }

	    return (float) Math.sqrt(result);
	}


	public int get(int i) {
		return data[i];
	}
	
	public void set(int i, int val) {
		data[i] = val;
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
