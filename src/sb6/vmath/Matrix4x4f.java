package sb6.vmath;

import static java.lang.Math.*;

import java.nio.ByteBuffer;

import sb6.BufferUtilsHelper;

public class Matrix4x4f extends MatrixNxMf {
	private float [] tmp;
	private float[] tmp2;

	public Matrix4x4f() {
		super(4, 4);
		tmp = new float [columns*this.rows];
		tmp2 = new float [columns*this.rows];
	}

	/** 
	 * data must be column-major!
	 */
	public Matrix4x4f(float[] data) {
		super(4, data);
		tmp = new float [columns*this.rows];
		tmp2 = new float [columns*this.rows];
	}

	public Matrix4x4f(Matrix4x4f m) {
		this();
		System.arraycopy(m.data, 0, data, 0, m.data.length);
	}

	/** Create a matrix providing the columns as 4 component vectors. 
	 * @param column1
	 * @param column2
	 * @param column3
	 * @param column4
	 */
	public Matrix4x4f(Vector4f column1, Vector4f column2,
			Vector4f column3, Vector4f column4) {
		this(column1.data, column2.data, column3.data, column4.data);
	}

	public Matrix4x4f(float[] column1, float[] column2, float[] column3, float[] column4) {
		this();
		int dest = 0;
		System.arraycopy(column1, 0, data, dest, rows);
		dest += rows;
		System.arraycopy(column2, 0, data, dest, rows);
		dest += rows;
		System.arraycopy(column3, 0, data, dest, rows);
		dest += rows;
		System.arraycopy(column4, 0, data, dest, rows);
		dest += rows;
	}

	public static float degrees(float angleInRadians)
	{
	    return angleInRadians * (float)(180.0/PI);
	}

	public static float radians(float angleInDegrees)
	{
	    return angleInDegrees * (float)(PI/180.0);
	}

	public static Matrix4x4f perspective(float fovy, float aspect, float n, float f)
	{
	    float q = 1.0f / (float)Math.tan(radians(0.5f * fovy));
	    float A = q / aspect;
	    float B = (n + f) / (n - f);
	    float C = (2.0f * n * f) / (n - f);

	    Matrix4x4f result = new Matrix4x4f();

	    result.setColumn(0, A, 0.0f, 0.0f, 0.0f);
	    result.setColumn(1, 0.0f, q, 0.0f, 0.0f);
	    result.setColumn(2, 0.0f, 0.0f, B, -1.0f);
	    result.setColumn(3, 0.0f, 0.0f, C, 0.0f);

	    return result;
	}


	public static Matrix4x4f frustum(float left, float right, float bottom, float top, float n, float f)
	{
	    Matrix4x4f result = Matrix4x4f.identity();
	
	    if ((right == left) ||
	        (top == bottom) ||
	        (n == f) ||
	        (n < 0.0) ||
	        (f < 0.0))
	       return result;
	
	    result.setCell(0, 0, (2.0f * n) / (right - left));
	    result.setCell(1, 1, (2.0f * n) / (top - bottom));
	
	    result.setColumn(2, (right + left) / (right - left),
	    		(top + bottom) / (top - bottom),
	    		-(f + n) / (f - n),
	    		-1.0f);
	
	    result.setCell(3, 2, -(2.0f * f * n) / (f - n));
	    result.setCell(3, 3, 0.0f);
	
	    return result;
	}

	
	
	private void setCell(int column, int row, float f) {
		data[row + column*rows] = f;
	}

	private void setColumn(int col, float v1, float v2, float v3, float v4) {
		set(0, col, v1);
		set(1, col, v2);
		set(2, col, v3);
		set(3, col, v4);
	}

	public void setRow(int row, float v1, float v2, float v3, float v4) {
		set(row, 0, v1);
		set(row, 1, v2);
		set(row, 2, v3);
		set(row, 3, v4);
	}

	public static Matrix4x4f translate(Vector3f v)
	{
	    return translate(v.get(0), v.get(1), v.get(2));
	}

	
	public static Matrix4x4f translate(float x, float y, float z) {
	    return new Matrix4x4f(new float[]{1.0f, 0.0f, 0.0f, 0.0f,
            	0.0f, 1.0f, 0.0f, 0.0f,
            	0.0f, 0.0f, 1.0f, 0.0f,
            	x, y, z, 1.0f});
	}
	public Matrix4x4f setTranslate(float x, float y, float z) {
		setIdentity();
		int c = columns-1;
		int r = rows-4;
		int i = r + c*rows;
		data[i++] = x;
		data[i++] = y;
		data[i++] = z;
		return this;
	}

	public static Matrix4x4f lookat(final Vector3f eye, final Vector3f center, Vector3f up)
	{
		Vector3f f = Vector3f.normalize(Vector3f.sub(center, eye));
		Vector3f upN = Vector3f.normalize(up);
		Vector3f s = Vector3f.cross(f, upN);
		Vector3f u = Vector3f.cross(s, f);
	    Matrix4x4f M = new Matrix4x4f(new Vector4f(s.get(0), u.get(0), -f.get(0), 0.0f),
	                                new Vector4f(s.get(1), u.get(1), -f.get(1), 0.0f),
	                                new Vector4f(s.get(2), u.get(2), -f.get(2), 0.0f),
	                                new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));

	    return M .mul( translate((Vector3f)eye.mul(-1.0f)));
	}


	public static Matrix4x4f rotate(float angle, float x, float y, float z)
	{
		Matrix4x4f result = new Matrix4x4f();

	    return result.setRotate(angle,  x,  y,  z);
	}

	public Matrix4x4f setRotate(float angle, float x, float y, float z) {
		rotate(data, angle, x, y, z);
		return this;
	}

	public Matrix4x4f mulRotate(float angle, float x, float y, float z) {
		rotate(tmp, angle, x, y, z);
		multiply(tmp2, data, tmp, columns, rows);
		float[] swap = data;
		data = tmp2;
		tmp2 = swap;
		return this;
	}
	
	private static void rotate(float[] data, float angle, float x, float y, float z) {
		int i = 0;
		
	    float x2 = x * x;
	    float y2 = y * y;
	    float z2 = z * z;
	    float rads = angle * 0.0174532925f;
	    float c = (float) Math.cos(rads);
	    float s = (float) Math.sin(rads);
	    float omc = 1.0f - c;
	    data[i++] = (float)(x2 * omc + c);
	    data[i++] = (float)(y * x * omc + z * s);
	    data[i++] = (float)(x * z * omc - y * s);
	    data[i++] = (float)(0);
	    data[i++] = (float)(x * y * omc - z * s);
	    data[i++] = (float)(y2 * omc + c);
	    data[i++] = (float)(y * z * omc + x * s);
	    data[i++] = (float)(0);
	    data[i++] = (float)(x * z * omc + y * s);
	    data[i++] = (float)(y * z * omc - x * s);
	    data[i++] = (float)(z2 * omc + c);
	    data[i++] = (float)(0);
	    data[i++] = (float)(0);
	    data[i++] = (float)(0);
	    data[i++] = (float)(0);
	    data[i++] = (float)(1);
	}


	public static Matrix4x4f identity() {
		Matrix4x4f m = new Matrix4x4f();
		return (Matrix4x4f) m.setIdentity();
	}

	public static Matrix4x4f scale(float x, float y, float z) {
		Matrix4x4f result = new Matrix4x4f();
		result.setScale(x,y,z);
		return result;
	}

	public static Matrix4x4f scale(float f) {
		return scale(f,f,f);
	}


	public void setScale(float x, float y, float z) {
		int r = 0, c = 0;
		data[r + c*rows] = x; r++; c++;
		data[r + c*rows] = y; r++; c++;
		data[r + c*rows] = z; r++; c++;
		data[r + c*rows] = 1.0f;
	}

	public Matrix4x4f mul(Matrix4x4f m) {

		multiply(tmp, data, m.data, columns, rows);
		
		// swap (data, tmp)
		float[] swap = data;
		this.data = tmp;
		tmp = swap;
		
		this.columns = m.columns;
		return this;
	}

	private static void multiply(float[] dst, float[] src1, float[] src2, int src1_columns, int src1_rows) {
		if (dst == src1 || dst == src2) throw new IllegalArgumentException("dst must be different from src1 and src2");
		
		for(int c = 0; c < src1_columns; c++) {
			for (int r = 0; r < src1_rows; r++) {
                float sum = 0f;

                for (int n = 0; n < src1_columns; n++)
                {
                    sum += src1[r + n*src1_rows] * src2[n + c*src1_rows];
                }

                dst[r + c*src1_rows] = sum;
			}
		}
	}
	
	
	/** Returns the product of m1 * m2 as new Matrix4x4f */
	public static Matrix4x4f multiply(Matrix4x4f m1,
			Matrix4x4f m2) {
		return new Matrix4x4f(m1).mul(m2);
	}

	public static long sizeof() {
		return 16L * BufferUtilsHelper.SIZEOF_FLOAT;
	}

	public void toByteBuffer(ByteBuffer out) {
		toFloatBuffer(out.asFloatBuffer());
		out.position(out.position() + (int)sizeof());
	}

	public Vector3f mul(Vector3f v) {
		Vector3f r = new Vector3f();
		r.data[0] = v.data[0] * get(0,0) + v.data[1] * get(0,1) + v.data[2] * get(0,2);
		r.data[1] = v.data[0] * get(1,0) + v.data[1] * get(1,1) + v.data[2] * get(1,2);
		r.data[2] = v.data[0] * get(2,0) + v.data[1] * get(2,1) + v.data[2] * get(2,2);
		return r;
	}

	
}
