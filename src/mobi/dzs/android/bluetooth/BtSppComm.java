package mobi.dzs.android.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import mobi.dzs.android.util.RingByteBuffer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.os.SystemClock;

/**
 * 蓝牙串口通信类
 * 
 * @version 1.0 2013-03-21
 * @author JerryLi (lijian@dzs.mobi)
 * @see 抽象类，不要对其直接实例化。sendData()需要继承后再定义对外公开方法。<br />
 *      使用本类，需要有以下两个权限<br />
 *      &lt;uses-permission android:name="android.permission.BLUETOOTH"/&gt;<br />
 *      &lt;uses-permission
 *      android:name="android.permission.BLUETOOTH_ADMIN"/&gt;<br />
 *      Android 支持版本 LEVEL 4以上，并且LEVEL 17支持bluetooth 4的ble设备
 * */
public abstract class BtSppComm {
	/** 常量:SPP的Service UUID */
	public final static String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";

	/** 接收缓存池大小，8k */
	private RingByteBuffer mRecvBuf = new RingByteBuffer(10);
	/** 接收、发送计数 */
	private RxdTxdCount rxdTxdCount = new RxdTxdCount();
	/** 蓝牙地址码 */
	private String mMac;

	/* Get Default Adapter */
	private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	/** 蓝牙串口连接对象 */
	private BluetoothSocket mBtSocket = null;
	/** 输入流对象 */
	private InputStream mInStream = null;
	/** 输出流对象 */
	private OutputStream mOutStream = null;
	/** 用于同步 mBtSocket mInStream mOutStream 的赋值, 并对mOutStream和mInStream的读写进行锁定 */
	private ReadWriteLock mBtRdWrLock = new ReentrantReadWriteLock(false);
	
	/** 连接建立的时间 */
	private long mConnEstablishedTimestamp = 0;
	/** 连接关闭时间 */
	private long mConnCloseTimestamp = 0;

	/** 接收线程, 默认不启动接收线程，只有当调用接收函数后，才启动接收线程 */
	private RecvThread mRecvThread = null;

	/** 常量:未设限制的AsyncTask线程池(重要) */
	private static ExecutorService FULL_TASK_EXECUTOR;
	static {
		FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();
	};
	/** 操作开关，强制结束本次接收等待 */
	private Boolean mKillRecvData_StopFlg = false;

	/**
	 * 构造函数
	 * 
	 * @param String
	 *            sMAC 需要连接的蓝牙设备MAC地址码
	 * */
	public BtSppComm(String mac) {
		mMac = mac;
	}
	
	/**
	 * 发送、接收字节数计数 类
	 * 
	 * @author t
	 *
	 */
	private class RxdTxdCount {
		/** 接收到的字节数 */
		private Long mRxdCount = 0L;
		/** 发送的字节数 */
		private Long mTxdCount = 0L;
		
		RxdTxdCount() {
			reset();
		}
		
		/**
		 * 获取接收的字节数
		 * 
		 * @return 接收字节数总和
		 */
		public long getRxdCount() {
			synchronized (mRxdCount) {
				return mRxdCount;
			}
		}
		
		/**
		 * 设置接收字节数
		 * 
		 * @param rxdCount 接收字节数总和
		 */
		private void setRxdCount(long rxdCount) {
			synchronized (mRxdCount) {
				mRxdCount = rxdCount;
			}
		}
		
		/**
		 * 增加接收字节数
		 * 
		 * @param rxdn 增加的字节数
		 */
		public void addRxdCount(long rxdn) {
			synchronized (mRxdCount) {
				mRxdCount += rxdn;
			}
		}

		/**
		 * 获取发送的字节数
		 * 
		 * @return 发送字节数总和
		 */
		public long getTxdCount() {
			return mTxdCount;
		}
		
		/**
		 * 设置发送字节数
		 * 
		 * @param txdCount
		 */
		private void setTxdCount(long txdCount) {
			mTxdCount = txdCount;
		}
		
		/**
		 * 增加发送字节数
		 * 
		 * @param txdn 增加的字节数
		 */
		public void addTxdCount(long txdn) {
			synchronized (mRxdCount) {
				mTxdCount += txdn;
			}
		}
		
		/**
		 * 初始化计数
		 */
		public void reset() {
			setTxdCount(0);
			setRxdCount(0);
		}
	}

	/**
	 * 获取连接保持的时间
	 * 
	 * @return 单位 秒
	 * */
	public long getConnectHoldTime() {
		if (0 == mConnEstablishedTimestamp) {
			return 0;
		} else if (0 == mConnCloseTimestamp) {
			return (System.currentTimeMillis() - mConnEstablishedTimestamp) / 1000;
		} else {
			return (mConnCloseTimestamp - mConnEstablishedTimestamp) / 1000;
		}
	}

	/**
	 * 断开蓝牙设备的连接
	 * 
	 * @return void
	 * */
	public void closeConn() {
		if (!isConnect())
			return;
		synchronized (mBtRdWrLock) {
			try {
				if (null != mInStream)
					mInStream.close();
				if (null != mOutStream)
					mOutStream.close();

				if (null != mBtSocket) {
					mBtSocket.close();
					mBtSocket = null;
				}

			} catch (IOException e) {
				// 任何一部分报错，都将强制关闭socket连接
				mInStream = null;
				mOutStream = null;
				mBtSocket = null;
			} finally { // 保存连接中断时间
				mConnCloseTimestamp = System.currentTimeMillis();
			}
		}
	}

	/**
	 * 建立蓝牙设备串口通信连接<br />
	 * <strong>备注</strong>：这个函数最好放到线程中去调用，因为调用时会阻塞系统
	 * 
	 * @return boolean false:连接创建失败 / true:连接创建成功
	 * */
	final public boolean createConn() {
		if (!mBtAdapter.isEnabled())
			return false;

		// 如果连接已经存在，则断开连接
		if (isConnect())
			closeConn();

		/* 开始连接蓝牙设备 */
		final BluetoothDevice BtDev = mBtAdapter.getRemoteDevice(mMac);
		final UUID uuidSPP = UUID.fromString(BtSppClient.SPP_UUID);

		synchronized (mBtRdWrLock) {
			try {
				// 得到设备连接后，立即创建SPP连接
				if (Build.VERSION.SDK_INT >= 10) {
					// 2.3.3以上的设备需要用这个方式创建通信连接
					mBtSocket = BtDev.createInsecureRfcommSocketToServiceRecord(uuidSPP);
				} else {
					// 创建SPP连接 API level 5
					mBtSocket = BtDev.createRfcommSocketToServiceRecord(uuidSPP);
				}


				mBtSocket.connect();
				mOutStream = mBtSocket.getOutputStream();// 获取全局输出流对象
				mInStream = mBtSocket.getInputStream(); // 获取流输入对象
				mConnEstablishedTimestamp = System.currentTimeMillis(); // 保存连接建立时间
			} catch (IOException e) {
				closeConn();// 断开连接
				return false;
			} finally {
				mConnCloseTimestamp = 0; // 连接终止时间初始化
			}
		}

		return true;
	}

	/**
	 * 设备的通信是否已建立
	 * 
	 * @return boolean true:通信已建立 / false:通信丢失
	 * */
	public boolean isConnect() {
		synchronized (mBtRdWrLock) {
			return mBtSocket == null ? false : true;
		}
	}

	/**
	 * 接收到的字节数
	 * 
	 * @return long
	 * */
	public long getRxdCount() {
		return rxdTxdCount.getRxdCount();
	}

	/**
	 * 发送的字节数
	 * 
	 * @return long
	 * */
	public long getTxdCount() {
		return rxdTxdCount.getTxdCount();
	}

	/**
	 * 接收缓冲池的数据量
	 * 
	 * @return int
	 * */
	public int getRecvBufLen() {
		return mRecvBuf.remain();
	}

	/**
	 * 发送数据
	 * 
	 * @param byte bD[] 需要发送的数据位
	 * @return int >=0 发送正常, -2:连接未建立; -3:连接丢失
	 * */
	protected int sendData(byte[] data) {
		if (!isConnect())
			return -2;

		try {
			// 发送字符串值
			mOutStream.write(data);
			
			rxdTxdCount.addTxdCount(data.length);

			return data.length;
		} catch (IOException e) {
			// 到这儿表示蓝牙连接已经丢失，关闭socket
			this.closeConn();
			return -3;
		}
	}

	/**
	 * 接收数据<br />
	 * <strong>备注:</strong>getRecvBufLen()>0时，本函数能够取出数据。一般在线程中使用这个函数
	 * 
	 * @return null:未连接或连接中断/byte[]:取到的新数据
	 * */
	final protected byte[] recvData() {
		if (!isConnect())
			return null;

		runRecvThreadIfNot();
		
		return mRecvBuf.read();
	}

	/**
	 * 接收数据（带结束标识符的接收方式）<br />
	 * <strong>注意:</strong>本函数以阻塞模式工作，如果未收到结束符，将一直等待。<br />
	 * <strong>备注:</strong>只有遇到结束标示符时才会终止等待，并送出结果。适合于命令行模式。<br />
	 * 如果想要终止阻塞等待可调用killRecvData_StopFlg()
	 * 
	 * @param btStopFlg
	 *            结束符 (例如: '\n')
	 * @return null:未连接或连接中断/byte[]:取到数据
	 * */
	final protected byte[] recvData_StopFlg(byte[] btStopFlg) {
		byte[] ret = null; // 临时输出缓存

		if (!isConnect())
			return null;

		runRecvThreadIfNot();

		synchronized (mKillRecvData_StopFlg) {
			mKillRecvData_StopFlg = false; // 可用killRecvData_StopFlg()来终止阻塞状态
		}

		mRecvBuf.setSeparator(btStopFlg);
		while (isConnect()) {
			synchronized (mKillRecvData_StopFlg) {
				if (mKillRecvData_StopFlg)
					break;
			}

			if ((ret = mRecvBuf.readline()) != null)
				return ret;

			SystemClock.sleep(50);// 死循环，等待数据回复
		}

		return ret;
	}

	/**
	 * 强制终止ReceiveData_StopFlg()的阻塞等待状态
	 * 
	 * @return void
	 * @see 必须在ReceiveData_StopFlg()执行后，才有使用价值
	 * */
	public void killReceiveData_StopFlg() {
		synchronized (mKillRecvData_StopFlg) {
			mKillRecvData_StopFlg = true;
		}
	}
	
	/**
	 * 如果数据接收线程未启动，则启动接收线程
	 */
	private void runRecvThreadIfNot() {
		if (mRecvThread == null) {
			mRecvThread = new RecvThread();
			
			if (Build.VERSION.SDK_INT >= 11) {
				// LEVEL 11 时的特殊处理
				mRecvThread.executeOnExecutor(FULL_TASK_EXECUTOR);
			} else {
				// 启动接收线程
				mRecvThread.execute("");
			}
		}
	}

	// ----------------
	/** 多线程处理<br>
	 * 接收数据的线程
	 */
	private class RecvThread extends AsyncTask<String, String, Integer> {
		/** 常量:缓冲区最大空间 */
		static private final int BUFF_MAX_CONUT = 1024;
		/** 常量:连接丢失 */
		static private final int CONNECT_LOST = 0x01;
		/** 常量：接收线程正常结束 */
		static private final int THREAD_END = 0x02;

		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute() {
			mRecvThread = this;
			/* 清空缓冲数据 */
			mRecvBuf.clear();
		}

		@Override
		protected Integer doInBackground(String... arg0) {
			int iReadCnt = 0; // 本次读取的字节数
			byte[] btButTmp = new byte[BUFF_MAX_CONUT]; // 临时存储区

			/* 只要连接建立完成就开始进入读取等待处理 */
			while (isConnect()) {
					try {
						iReadCnt = mInStream.read(btButTmp); // 没有数据，将一直锁死在这个位置等待
					} catch (IOException e) {
						return CONNECT_LOST;
					}

				// 开始处理接收到的数据
				rxdTxdCount.addRxdCount(iReadCnt);// 记录接收的字节总数
				int nwrite = 0;
				while (nwrite < iReadCnt) {
					nwrite += mRecvBuf.write(btButTmp, nwrite, iReadCnt - nwrite);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			return THREAD_END;
		}

		/**
		 * 阻塞任务执行完后的清理工作
		 */
		@Override
		public void onPostExecute(Integer result) {
			mRecvThread = null;

			if (CONNECT_LOST == result) {
				// 判断是否为串口连接失败
				closeConn();
			} else { // 正常结束，关闭接收流
				synchronized (mBtRdWrLock) {
					try {
						mInStream.close();
						mInStream = null;
					} catch (IOException e) {
						mInStream = null;
					}
				}
			}
		}
	}
}
