package mobi.dzs.android.util;

/**
 * 环形字节缓冲
 * 
 * <p>
 * 
 * <pre>
 *            T     H
 *            |     |
 *            v     v
 * +---+---+-+-+---+-+---+-+
 * |n+1|...|S| |   |1|...|n|
 * +---+---+-+-+---+-+---+-+
 * 
 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail, <b><i>S</i></b> is data size.
 * </per>
 * 
 * @author t
 * 
 */
public class RingByteBuffer {
	private byte[] mBuf = null;
	/** 数组使用情况指示器 */
	private int mHead, mTail;
	/** 用于readline方法中，保存上次搜索分割符时已经搜索的位置 {@link #readline(boolean)} */
	private int mSearchIdx;
	/** 用于readline作为各行的分割符 */
	private byte[] mSeparator;
	private int mSeparatorIdx;
	private int[] mSepKmpTab;

	/**
	 * 构造方法
	 * 
	 * @param size 缓冲大小，单位:Byte
	 */
	public RingByteBuffer(int size) {
		mBuf = new byte[size + 1];
		mHead = mTail = 0;
		setSeparator(null);
	}

	/**
	 * 获取缓冲大小
	 * 
	 * @return 缓冲大小
	 * 
	 * @see #remain()
	 * @see #isEmpty()
	 * @see #isFull()
	 */
	public int size() {
		return mBuf.length - 1;
	}
	
	/**
	 * 设置readline方法使用的换行符
	 * 
	 * @param separator 换行分隔符
	 * 
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #getSeparator()
	 */
	synchronized public void setSeparator(byte[] separator) {
		if (separator == mSeparator)
			return;
		
		if (separator == null || separator.length == 0)
			separator = new byte[]{'\n'};
		
		mSeparator = separator;
		mSepKmpTab = getKmpTab(mSeparator);
		mSearchIdx = mHead;
		mSeparatorIdx = 0;
	}
	
	/**
	 * 获取readline当前使用的换行符
	 * 
	 * @return 存储于byte[]中的换行分隔符
	 * 
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #setSeparator(byte[])
	 */
	synchronized public byte[] getSeparator() {
		return mSeparator;
	}

	/**
	 * 判断缓冲是否为空
	 * 
	 * <p>
	 * 
	 * <pre>
	 *      T
	 *      |
	 *      v
	 * +---+-+---+
	 * |   | |   |
	 * +---+-+---+
	 *      ^
	 *      |
	 *      H
	 *    T == H
	 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail.
	 * </per>
	 * 
	 * @return 为空返回true，否则返回false
	 * 
	 * @see #isFull()
	 * @see #remain()
	 * @see #size()
	 */
	synchronized public boolean isEmpty() {
		return mHead == mTail;
	}

	/**
	 * 判断缓冲是否以满
	 * 
	 * <p>
	 * 
	 * <pre>
	 *  H       T              T H
	 *  |       |              | |
	 *  v       v              v v
	 * +-+---+-+-+    +-+---+-+-+-+---+
	 * |1|...|S| |    |n|...|S| |1|...|
	 * +-+---+-+-+    +-+---+-+-+-+---+
	 * T - H == S         H - T == 1
	 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail, <b><i>S</i></b> is data size.
	 * </per>
	 * 
	 * @return 缓冲满返回true，否则返回false
	 * 
	 * @see #isEmpty()
	 * @see #remain()
	 * @see #size()
	 */
	synchronized public boolean isFull() {
		return (mHead - mTail + mBuf.length) % mBuf.length == 1;
	}

	/**
	 * 获取缓冲剩余空间
	 * 
	 * <p>
	 * 
	 * <pre>
	 *      H       T                   T     H
	 *      |       |                   |     |
	 *      v       v                   v     v
	 * +---+-+---+-+-+---+     +-+---+-+-+---+-+---+
	 * |   |1|...|S| |   |     |n|...|s| |   |1|...|
	 * +---+-+---+-+-+---+     +-+---+-+-+---+-+---+
	 *       R = T-H              R = S+1 - H + T
	 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail, <b><i>S</i></b> is data size, <b><i>R</i></b> is remain.
	 * </per>
	 * 
	 * @return 缓冲剩余空间
	 * 
	 * @see #size()
	 * @see #isEmpty()
	 * @see #isFull()
	 */
	synchronized public int remain() {
		return (mTail - mHead + mBuf.length) % mBuf.length;
	}

	/**
	 * 从缓冲中读出数据
	 * 
	 * @param n
	 *            欲读取的字节数
	 * @return 读取到的bytes数据
	 * 
	 * @see #read()
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #write(byte[], int, int)
	 * @see #write(byte[])
	 * @see #clear()
	 */
	synchronized public byte[] read(int n) {
		byte[] ret = null;
		int idx;
		int remain = remain();
		
		if (n < 1 || remain < 1)
			return null;

		ret = new byte[n > remain ? remain : n];
		for (idx = 0; idx < ret.length; ++idx) {
			ret[idx] = mBuf[(mHead + idx) % mBuf.length];
		}
		mHead = (mHead + idx) % mBuf.length;
		
		/* 重置readline()会用到的指示器 @see {@link #readline()} */
		if (mSearchIdx < mHead || mSearchIdx > mTail) {
			mSearchIdx = mHead;
			mSeparatorIdx = 0;
		}

		return ret;
	}
	
	/**
	 * 读取缓存中的所有数据
	 * 
	 * @return 读取到的数据
	 * 
	 * @see #read(int)
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #write(byte[], int, int)
	 * @see #write(byte[])
	 * @see #clear()
	 */
	synchronized public byte[] read() {
		return read(remain());
	}
	
	/**
	 * 从缓冲中读取一行数据<br>
	 * 通过搜索指定换行分隔符区分各行。换行符不仅限于\n或\r,可以自定义换行符号。<br>
	 * 使用setSeparator(byte[])方法制定换行分隔符。
	 * 
	 * <p>
	 * 
	 * <pre>
	 *      H     P       T                   T     H     P                 P     T     H
	 *      |     |       |                   |     |     |                 |     |     |
	 *      v     v       v                   v     v     v                 v     v     v
	 * +---+-+---+-+---+-+-+---+     +-+---+-+-+---+-+---+-+---+     +-+---+-+---+-+---+-+---+
	 * |   |1|...|m|...|S| |   |     |n|...|S| |   |1|...|m|...|     |m|...|n|...|S|   |1|...|
	 * +---+-+---+-+---+-+-+---+     +-+---+-+-+---+-+---+-+---+     +-+---+-+---+-+---+-+---+
	 *    
	 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail, <b><i>S</i></b> is buffer size, <b><i>P</i></b> is search position.
	 * </per>
	 * 
	 * @param reSearch 是否需要从头开始搜索换行符，为false时，会提高效率
	 * @return 一行数据
	 * 
	 * @see #setSeparator(byte[])
	 * @see #read(int)
	 * @see #read()
	 * @see #readline()
	 * @see #write(byte[], int, int)
	 * @see #write(byte[])
	 * @see #clear()
	 */
	public byte[] readline(boolean reSearch) {
		
		if (reSearch) {
			mSearchIdx = mHead;
			mSeparatorIdx = 0;
		}
	
		while (mSearchIdx != mTail) {
			while (mSeparatorIdx >= 0 &&
					mBuf[mSearchIdx] != mSeparator[mSeparatorIdx]) {
				mSeparatorIdx = mSepKmpTab[mSeparatorIdx];
			}
			mSearchIdx = (mSearchIdx + 1) % mBuf.length;
			++mSeparatorIdx;

			if (mSeparatorIdx == mSeparator.length) {
				mSeparatorIdx = mSepKmpTab[mSeparatorIdx];
				return read((mSearchIdx - mHead + mBuf.length) % mBuf.length);
			}
		}
		
		if (isFull()) {
			/* 缓冲是满的，但没有搜索到换行符，直接把所有数据返回 */
			return read();
		}
		
		return null;
	}
	
	/**
	 * 从缓冲中读取一行数据<br>
	 * 通过搜索指定换行分隔符区分各行。换行符不仅限于\n或\r,可以自定义换行符号。<br>
	 * 使用setSeparator(byte[])方法制定换行分隔符。
	 * 
	 * <p>
	 * 
	 * <pre>
	 *      H     P       T                   T     H     P                 P     T     H
	 *      |     |       |                   |     |     |                 |     |     |
	 *      v     v       v                   v     v     v                 v     v     v
	 * +---+-+---+-+---+-+-+---+     +-+---+-+-+---+-+---+-+---+     +-+---+-+---+-+---+-+---+
	 * |   |1|...|m|...|S| |   |     |n|...|S| |   |1|...|m|...|     |m|...|n|...|S|   |1|...|
	 * +---+-+---+-+---+-+-+---+     +-+---+-+-+---+-+---+-+---+     +-+---+-+---+-+---+-+---+
	 *    
	 * <b><i>H</i></b> is head, <b><i>T</i></b> is tail, <b><i>S</i></b> is buffer size, <b><i>P</i></b> is search position.
	 * </per>
	 * 
	 * @return 一行数据
	 * 
	 * @see #setSeparator(byte[])
	 * @see #read(int)
	 * @see #read()
	 * @see #readline(boolean)
	 * @see #write(byte[], int, int)
	 * @see #write(byte[])
	 * @see #clear()
	 */
	public byte[] readline() {
		return readline(false);
	}
	
	/**
	 * 向缓冲写入数据<br>
	 * 
	 * @param buffer 数据数组
	 * @param offset 数据数组开始位置
	 * @param count  数据数量
	 * @return 成功写入数据数量
	 * 
	 * @see #read(int)
	 * @see #read()
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #write(byte[])
	 * @see #clear()
	 */
	synchronized public int write(byte[] buffer, int offset, int count) {
		int idx;
		int nWrite;
		
		if ((nWrite = mBuf.length - 1 - remain()) > count)
			nWrite = count;

		for (idx = 0; idx < nWrite; ++idx) {
			mBuf[(mTail + idx) % mBuf.length] = buffer[offset + idx];
		}
		mTail = (mTail + idx) % mBuf.length;

		return idx;
	}

	/**
	 * 向缓冲写入数据<br>
	 * 相当于 {@link #write(buffer, 0, buffer.length)}
	 * 
	 * @param buffer
	 *            数据
	 * @return 成功写入数据的字节数
	 * 
	 * @see #read(int)
	 * @see #read()
	 * @see #readline(boolean)
	 * @see #readline()
	 * @see #write(byte[], int, int)
	 * @see #clear()
	 */
	synchronized public int write(byte[] buffer) {
		 return write(buffer, 0, buffer.length);
	}

	/**
	 * 清空缓冲区
	 * 
	 * @see #read()
	 * @see #read(int)
	 * @see #readline(byte[])
	 * @see #write(byte[])
	 */
	synchronized public void clear() {
		mHead = mTail = 0;
		mSearchIdx = mHead;
		mSeparatorIdx = 0;
	}
	
	private int[] getKmpTab(byte[] ptrn) {
		int idx = 0, n = -1;
		int[] kmpTab = new int[ptrn.length + 1];

		kmpTab[idx] = n;
		while (idx < ptrn.length) {
			while (n >= 0 && ptrn[idx] != ptrn[n]) {
				n = kmpTab[n];
			}
			idx++;
			n++;
			kmpTab[idx] = n;
		}

		return kmpTab;
	}
}