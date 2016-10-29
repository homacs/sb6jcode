package sb6.vmath;

import java.io.PrintStream;
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
	private FloatBuffer apiBuffer;
	

	public MatrixNxMf(int columns, int rows) {
		this.columns = columns;
		this.rows = rows;
		data = new float[columns*rows];
		apiBuffer = BufferUtils.createFloatBuffer(columns*rows);
	}
	
	public MatrixNxMf(int columns, float[] data) {
		this.columns = columns;
		this.rows = data.length/columns;
		this.data = data;
		apiBuffer = BufferUtils.createFloatBuffer(this.columns*this.rows);
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

	/** 
	 * Add a given row onto the row r in the matrix
	 * @param r
	 * @param row
	 */
	public void addRow(int row, float[] values) {
		assert(values.length == columns);
		for (int c = 0; c < columns; c++) {
			set(row, c, get(row, c) + values[c]);
		}
		
	}

	/**
	 * Multiply row r with given factor f
	 * @param r
	 * @param f
	 */
	private void mulRow(int row, float f) {
		for (int c = 0; c < columns; c++) {
			set(row, c, get(row, c) * f);
		}
	}


	private void setRow(int row, float[] values, int src_off, int src_len) {
		int limit = columns > src_len ? src_len : columns;
		for (int c = 0; c < limit; c++) {
			set(row, c, values[src_off+c]);
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

	/**
	 * Append appendix to given matrix.
	 * @param given
	 * @param appendix
	 * @return
	 */
	public static MatrixNxMf append(MatrixNxMf given, MatrixNxMf appendix) {
		if (given.rows != appendix.rows) throw new IllegalArgumentException("appended matrix must have the same number of rows as the given matrix");
		int columns = given.columns + appendix.columns;

		MatrixNxMf result = new MatrixNxMf(columns, given.rows);
		for (int r = 0; r < given.rows; r++) {
			int c;
			for (c = 0; c < given.columns; c++) {
				result.set(r, c, given.get(r, c));
			}
			for (int k = 0;c < columns; c++, k++) {
				result.set(r, c, appendix.get(r, k));
			}

		}
		return result;
	}

	public static MatrixNxMf inverse(MatrixNxMf in) {
		MatrixNxMf m = MatrixNxMf.append(in, new MatrixNxMf(in.columns, in.rows).setIdentity());
		float[] row_buf = new float[m.columns];
		
		//
		// create upper triangular matrix to the left
		//
		for (int r = 0; r < m.rows; r++) {
			int c = r;// column we are currently working on
			int s;    // another row index
			float w;  // value of get(s,c)
			
			float v = m.get(r, c);
			if (v == 0f) {
				// find row with v(c) != 0
				w = 0f;
				for (s = r+1; s < m.rows; s++) {
					w = m.get(s, c);
					if (w != 0) break;
				}
				if (w == 0f) throw new IllegalArgumentException("no inverse matrix possible");
				m.getRow(s, row_buf);
				m.addRow(r, row_buf);
			}
			
			// for each row s below r, subtract the given row, so that get(s,c) == 0
			v = m.get(r, c);
			m.getRow(r, row_buf);
			for (s = r+1; s < m.rows; s++) {
				w = m.get(s, c);
				if (w != 0f) {
					VectorNf._mul_inplace(row_buf, -w/v);
					m.addRow(s, row_buf);
				}
			}
		}
		
		//
		// create lower triangular matrix to the left -> resulting in diagonal matrix
		//
		for (int r = m.rows-1; r >= 0; r--) {
			int c = r;// column we are currently working on
			int s;    // another row index
			float w;  // value of get(s,c)
			
			// for each row s below r, subtract the given row, so that get(s,c) == 0
			float v = m.get(r, c);
			for (s = r-1; s >= 0; s--) {
				w = m.get(s, c);
				if (w != 0f) {
					m.getRow(r, row_buf);
					VectorNf._mul_inplace(row_buf, -w/v);
					m.addRow(s, row_buf);
				}
			}
		}
		
		//
		// now transform into unit matrix (front half)
		//
		for (int r = 0; r < in.rows; r++) {
			float v = m.get(r, r);
			if (v != 1f) {
				m.mulRow(r, 1/v);
			}
		}

		MatrixNxMf result = new MatrixNxMf(in.columns, in.rows);
		for (int r = 0; r < m.rows; r++) {
			m.getRow(r, row_buf);
			result.setRow(r, row_buf, in.columns, in.columns);
		}

		return result;
	}

	public static MatrixNxMf setIdentity(MatrixNxMf m) {
		for (int r = 0; r < m.rows; r++) {
			for (int c = 0; c < m.columns; c++) {
				m.set(r, c, c==r ? 1f : 0f);
			}
		}
		return m;
	}
	public static void print (PrintStream out, MatrixNxMf m) {
		for (int r = 0; r < m.rows; r++) {
			for (int c = 0; c < m.columns; c++) {
				out.printf("%5.2f", m.get(r, c));
				if (c < m.columns) out.print(" ");
			}
			out.println();
		}
	}
	
	
	public static void main(String[] args) {
		MatrixNxMf m = new MatrixNxMf(2, 2);
		m.setData(new float[]{1,2,2,3});
		print(System.out, m);
		MatrixNxMf I = inverse(m);
		print(System.out, I);
	}

	
}
