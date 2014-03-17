package mobi.dzs.android.bluetooth;

import java.io.UnsupportedEncodingException;

import mobi.dzs.android.util.CHexConver;

/**
 * 蓝牙通信的SPP客户端
 * 
 * @version 1.0 2013-03-17
 * @author JerryLi (lijian@dzs.mobi)
 * */
public final class BtSppClient extends BtSppComm {
	/** 当前发送时的编码模式 */
	private BtIOMode mBtTxdMode = BtIOMode.STR;
	/** 当前接收时的编码模式 */
	private BtIOMode mBtRxdMode = BtIOMode.STR;
	/** 接收终止符 */
	private byte[] mBtEndFlg = null;
	/** 指定:输入输出字符集 默认不指定(UTF-8:一个全角占3字节/GBK:一个全角占2字节) */
	protected String mCharsetName = null;
	
	
		 
	/**
	 * 输入输出模式
	 * 
	 * @author t
	 */
	public enum BtIOMode {
		/**
		 * 二进制
		 * @deprecated 未实现
		 */
		BIN(2),
		
		/**
		 * 十进制
		 * @deprecated 未实现
		 */
		DEC(10),
		
		/**
		 * 十六进制
		 */
		HEX(16),
		
		/**
		 * 字符串
		 */
		STR(0);
		
		private int mMode;
		
		private BtIOMode(int mode) {
			mMode = mode;
		}
		
		static public BtIOMode valueOf(int value) {
			switch (value) {
			case 0:
				return STR;
			case 2:
				return BIN;
			case 10:
				return DEC;
			case 16:
				return HEX;
			default:
				return null;
			}
		}
		
		public int value() {
			return mMode;
		}
	}
	
	/**
	 * 创建蓝牙SPP客户端类
	 * 
	 * @param String mac 蓝牙MAC地址
	 * @return void
	 * */
	public BtSppClient(String mac) {
		super(mac); // 执行父类的构造函数
	}

	/**
	 * 设置发送时的字符串模式
	 * 
	 * @param mode 发送io模式
	 * @return void
	 * */
	public void setTxdMode(BtIOMode mode) {
		mBtTxdMode = mode;
	}
	
	/**
	 * 获取发送时的字符串模式
	 * 
	 * @return BtIOMode 发送io模式
	 * */
	public BtIOMode getTxdMode() {
		return mBtTxdMode;
	}
	
	/**
	 * 设置接收时的字符串输出模式
	 * 
	 * @param mode 接收io模式
	 * */
	public void setRxdMode(BtIOMode mode) {
		mBtRxdMode = mode;
	}

	/**
	 * 发送数据给设备
	 * 
	 * @param byte btData[] 需要发送的数据位
	 * @return int >0 发送正常, 0未发送数据, -2:连接未建立; -3:连接丢失
	 * */
	public int send(String data) {
		switch (mBtTxdMode) {
		case STR:
			if (null != mCharsetName) {
				try { // 尝试做字符集转换
					return sendData(data.getBytes(this.mCharsetName));
				} catch (UnsupportedEncodingException e) { // 字符集转换失败时使用默认字符集
					return sendData(data.getBytes());
				}
			} else {
				return sendData(data.getBytes());
			}
		case HEX:
			if (CHexConver.isHexStr(data)) {
				return sendData(CHexConver.hexToBytes(data));
			} else {
				return 0; // 无效的HEX值
			}
		default:
			return 0;
		}
	}
	
	/**
	 * 接收设备数据
	 * 
	 * @return String null:未连接或连接中断 / String:数据
	 * */
	public String recv() {
		byte[] data = recvData();

		if (null == data)
			return null;

		if (BtIOMode.HEX == mBtRxdMode) {
			// 16进制字符串转换成byte值
			return (CHexConver.bytesToHex(data)).concat(" ");
		} else {
			return new String(data);
		}
	}
	
	/**
	 * 设置接收指令行的终止字符
	 * 
	 * @return void
	 * @see 仅用于ReceiveStopFlg()函数
	 * */
	public void setRecvStopFlg(String sFlg) {
		mBtEndFlg = sFlg.getBytes();
	}
	
	/**
	 * 设置处理字符集(默认为UTF-8)
	 * 
	 * @param String
	 *            sCharset 设置字符集 GBK/GB2312
	 * @return void
	 * @see 此设置仅对ReceiveStopFlg()与Send()函数有效
	 * */
	public void setCharset(String sCharset) {
		mCharsetName = sCharset;
	}

	/**
	 * 接收设备数据，指令行模式（阻塞模式）<BR>
	 * 备注：即在接收时遇到终止字符后，才会输出结果，并在输出结果中会剔除终止符
	 * 
	 * @return null:未连接或连接中断/String:取到数据
	 * */
	public String recvStopFlg() {
		byte[] btTmp = null;

		if (null == mBtEndFlg)
			return new String(); // 未设置终止符

		btTmp = recvData_StopFlg(mBtEndFlg);

		if (null == btTmp)
			return null; // 无效的接收

		if (null == mCharsetName)
			return new String(btTmp);

		try {
			// 尝试对取得的值做字符集转换
			return new String(btTmp, mCharsetName);
		} catch (UnsupportedEncodingException e) {
			// 转换失败时直接用UTF-8输出
			return new String(btTmp);
		}
	}
}
