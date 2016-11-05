package sb6.vmath;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

import sb6.BufferUtilsHelper;

public class VectorNf {

	protected float[] data;
	private FloatBuffer apiBuffer;

	
	public VectorNf(float[] fs) {
		apiBuffer = BufferUtils.createFloatBuffer(fs.length);
		data = fs;
	}

	public VectorNf(float f, int len) {
		this(len);
        for (int n = 0; n < data.length; n++)
        {
            data[n] = f;
        }
	}

	public VectorNf(int len) {
		apiBuffer = BufferUtils.createFloatBuffer(len);
		data = new float[len];
	}

	public float[] getData() {
		return data;
	}

	public static float[] _add(float[] v1, float[] v2) {
		float[] result = new float[v1.length];
        for (int n = 0; n < v1.length; n++)
            result[n] = v1[n] + v2[n];
        return result;
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
	
	protected static float[] _mul(float[] v1, float[] v2) {
		float[] result = new float[v1.length];
        int n;
        for (n = 0; n < v1.length; n++)
            result[n] = v1[n] * v2[n];
        return result;

	}

	static float[] _mul_inplace(float[] v1, float f) {
        int n;
        for (n = 0; n < v1.length; n++)
            v1[n] = v1[n] * f;
        return v1;
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

	public void set(int i, float value) {
		data [i] = value;
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


	public void toFloatBuffer(FloatBuffer out) {
		out.put(data);
	}
	public void fromFloatBuffer(FloatBuffer in) {
		in.get(data);
	}

	public FloatBuffer toFloatBuffer() {
		toFloatBuffer(apiBuffer);
		apiBuffer.rewind();
		return apiBuffer;
	}

	public void fromByteBuffer(ByteBuffer in) {
		fromFloatBuffer(in.asFloatBuffer());
		in.position(in.position() + data.length * BufferUtilsHelper.SIZEOF_FLOAT);
	}


	public void toByteBuffer(ByteBuffer buf) {
		toFloatBuffer(buf.asFloatBuffer());
		buf.position(buf.position() + data.length * BufferUtilsHelper.SIZEOF_FLOAT);
	}


	/**
	 * Length (magnitude/norm) of the vector.
	 */
	public float length() {
	    float result = 0.0f;

	    for (int i = 0; i < size(); ++i)
	    {
	        result += get(i) * get(i);
	    }

	    return (float)Math.sqrt(result);
	}

	/**
	 * Number of coordinates (components) used by the vector.
	 * 
	 */
	public int size() {
		return data.length;
	}

	public void normalize()
	{
	    mul(1.0f/length());
	}


}
