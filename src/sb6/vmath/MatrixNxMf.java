package sb6.vmath;

import java.nio.FloatBuffer;
import java.util.Arrays;

import org.lwjgl.BufferUtils;

/**
 * N x M Matrix stored in column major format.
 * 
 * @author homac
 *
 */

public class MatrixNxMf
{
	
	protected float[] data;
	protected int columns;
	protected int rows;
	private FloatBuffer apiBuffer = BufferUtils.createFloatBuffer(4*4);
	

	public MatrixNxMf(int columns, int rows) {
		this.columns = columns;
		this.rows = rows;
		data = new float[16];
	}
	
	public MatrixNxMf(int columns, float[] data) {
		this.columns = columns;
		this.rows = data.length/columns;
		this.data = data;
	}
	
	public MatrixNxMf setIdentity() {
		setZero();
		for (int c = 0; c < columns && c < rows; c++) {
			int r = c;
			data[r + c*rows] = 1.0f;
		}
		return this;
	}

	public MatrixNxMf setZero() {
		Arrays.fill(data, 0.0f);
		return this;
	}


	public MatrixNxMf mul(MatrixNxMf m) {
		assert(columns == m.rows && m.rows == rows);
		float [] tmp = new float [m.columns*this.rows];
		
		for(int c = 0; c < columns; c++) {
			for (int r = 0; r < rows; r++) {
                float sum = 0f;

                for (int n = 0; n < columns; n++)
                {
                    sum += data[r + n*rows] * m.data[n + c*rows];
                }

                tmp[r + c*rows] = sum;
			}
		}
		this.data = tmp;
		this.columns = m.columns;
		return this;
	}

	public static float get(int row, int column, int rows, float[] that) {
		assert(that.length%rows == 0);
		return that[row + column*rows];
	}

	public float get(int row, int column) {
		return data[row + column*rows];
	}
	public void set(int row, int column, float val) {
		data[row + column*rows] = val;
	}
	public void setColumn(int column, float[] values) {
		assert(values.length == rows);
		System.arraycopy(values, 0, data, column*rows, rows);
	}
	public void setRow(int row, float[] values) {
		assert(values.length == columns);
		for (int c = 0; c < columns; c++) {
			set(row, c, values[c]);
		}
	}

	public void getColumn(int column, float[] values) {
		assert(values.length == rows);
		System.arraycopy(data, column*rows, values, 0, rows);
	}
	
	public void getRow(int row, float[] values) {
		assert(values.length == columns);
		for (int c = 0; c < columns; c++) {
			values[c] = get(row, c);
		}
	}

	public float[] getData() {
		return data;
	}

	public void setData(float[] data) {
		assert(data.length == rows*columns);
		this.data = data;
	}

	public void setData(float[] data, int rows, int columns) {
		this.rows = rows;
		this.columns =columns;
		this.data = data;
	}

	public int getColumns() {
		return columns;
	}

	public int getRows() {
		return rows;
	}
	
	@Override
	public String toString() {
		StringBuffer s = new StringBuffer();
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < columns; c++) {
				s.append(get(r,c)).append(", ");
			}
			s.append("\n");
		}
		return s.toString();
	}

	public FloatBuffer toFloatBuffer() {
		toFloatBuffer(apiBuffer);
		apiBuffer.rewind();
		return apiBuffer;
	}

	public void toFloatBuffer(FloatBuffer out) {
		out.put(data);
	}
	
	
	public void fromFloatBuffer(FloatBuffer in) {
		in.get(data);
	}
	
}
