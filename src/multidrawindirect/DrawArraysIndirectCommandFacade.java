package multidrawindirect;

import java.nio.ByteBuffer;

import sb6.BufferUtilsHelper;
import sb6.sbm.SBMObject.SBMSubObjectInfo;

/**
 * This class implements a facede for a data structure stored as raw data in some
 * region of memory outside of the virtual machine. The data structure has the 
 * following layout:
 * <pre>
 * struct DrawArraysIndirectCommand {
 *  int  count;        // int at pos=0
 *  int  primCount;    // int at pos=4
 *  int  first;        // int at pos=8
 *  int  baseInstance; // int at pos=12
 * };
 * </pre>
 * 
 * The facade just provides a view on that data structure through getter and setter 
 * methods which write or read the member variables in the referenced memory. The 
 * facade uses a ByteBuffer object to realise read/write native I/O access when 
 * getter and setter methods are called.
 * 
 * @author homac
 * @see ByteBuffer
 *
 */
public class DrawArraysIndirectCommandFacade implements SBMSubObjectInfo {
	/* Date is stored elsewhere and we have just a view on it.
	 * 
	 * Virtually this class provides access to the following members:
	 *  int  count;        // int at pos=0
	 *  int  primCount;    // int at pos=4
	 *  int  first;        // int at pos=8
	 *  int  baseInstance; // int at pos=12
     */

	/*
	 * Following is a list of indices in the byte array representation of the associated storage.
	 */
	static final int idx_count        = 0;
	static final int idx_primCount    = idx_count     + BufferUtilsHelper.SIZEOF_INTEGER;
	static final int idx_first        = idx_primCount + BufferUtilsHelper.SIZEOF_INTEGER;
	static final int idx_baseInstance = idx_first     + BufferUtilsHelper.SIZEOF_INTEGER;
	
	private ByteBuffer ptr;
	
	// unfortunately ByteBuffer supports just int, while we could use long instead
	private int offset;
    
    public static long sizeof() {
    	return 4 * BufferUtilsHelper.SIZEOF_INTEGER;
    }
    
    /**
     * Initialises this facade with the given ptr as reference on the memory region 
     * with the actual data. The offset specifies the position inside the memory 
     * region which contains the first member variable of the data structure represented 
     * by this facade. This ptr might be received through BufferUtils.createByteBuffer() or 
     * glMapBuffer().
     * 
     * Offset can be used to iterate through an array of DrawArraysIndirectCommand objects.
     * 
     * @param ptr
     * @param offset
     * @see #offset()
     * @see #offset(int)
     */
    public DrawArraysIndirectCommandFacade(ByteBuffer ptr, int offset) {
		this.ptr = ptr;
		this.offset = offset;
    }

    public void offset(int newOffset) {
    	offset = newOffset;
    }

    public int offset() {
    	return offset;
    }
    
	public int getCount() {
		return ptr.getInt(idx_count + offset);
	}


	public void setCount(int count) {
		ptr.putInt(idx_count + offset, count);
	}


	public int getPrimCount() {
		return ptr.getInt(idx_primCount + offset);
	}


	public void setPrimCount(int primCount) {
		ptr.putInt(idx_primCount + offset, primCount);
	}


	public int getFirst() {
		return ptr.getInt(idx_first + offset);
	}


	public void setFirst(int first) {
		ptr.putInt(idx_first + offset, first);
	}


	public int getBaseInstance() {
		return ptr.getInt(idx_baseInstance + offset);
	}


	public void setBaseInstance(int baseInstance) {
		ptr.putInt(idx_baseInstance + offset, baseInstance);
	}
    
    
}


