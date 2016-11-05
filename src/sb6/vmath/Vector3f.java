package sb6.vmath;


import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import sb6.BufferUtilsHelper;

public class Vector3f extends VectorNf {
	
	public Vector3f() {
		this(new float[3]);
	}
	
	public Vector3f(Vector3f eye) {
		this(Arrays.copyOf(eye.data, eye.data.length));
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


	public Vector3f(float f) {
		super(f, 3);
	}


	public static Vector3f sub(Vector3f v1, Vector3f v2) {
		return new Vector3f(VectorNf._sub(v1.data,v2.data));
	}

	public Vector3f sub(Vector3f subtrahend) {
		this.data = VectorNf._sub(this.data, subtrahend.data);
		return this;
	}

	public Vector3f mul(float f) {
		super.mul(f);
		return this;
	}
	
	public static Vector3f add(Vector3f v1, Vector3f v2) {
		return new Vector3f(VectorNf._add(v1.data,v2.data));
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


	public static int sizeof() {
		return BufferUtilsHelper.SIZEOF_FLOAT * 3;
	}



	public void set(float x, float y, float z) {
		data[0] = x;
		data[1] = y;
		data[2] = z;
	}


	private static Random rng = new Random();
	
	public static Vector3f random() {
		return new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
	}


	public Vector3f dot(Vector3f v) {
		data = super._mul(data, v.data);
		return this;
	}


	public void print(PrintStream out) {
		out.print("(");
		for (int i = 0; i < data.length; i++) {
			out.printf("%5.2f", data[i]);
			if (i < data.length) out.print(", ");
		}
		out.println(")");
	}


	public static Vector3f multiply(Vector3f eye, float f) {
		return new Vector3f(eye).mul(f);
	}






}
