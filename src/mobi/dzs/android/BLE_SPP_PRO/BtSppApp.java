package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.bluetooth.BluetoothSppClient;
import mobi.dzs.android.util.PreferencesStorage;
import android.app.Application;

public class BtSppApp extends Application {
	/** 蓝牙SPP通信连接对象 */
	public BluetoothSppClient mBtSppCli = null;
	/** 动态公共存储对象 */
	public PreferencesStorage mDS = null;
	/** 应用自引用 */
	private static BtSppApp sBtSppApp = null;

	/**
	 * 覆盖构造
	 * */
	@Override
	public void onCreate() {
		mDS = new PreferencesStorage(this);
		sBtSppApp = this;
	}

	/**
	 * 建立蓝牙连接
	 * 
	 * @param String
	 *            mac 蓝牙硬件地址
	 * @return boolean
	 * */
	synchronized public boolean createConn(String mac) {
		if (null != mBtSppCli)
			return true;

		mBtSppCli = new BluetoothSppClient(mac);
		if (!mBtSppCli.createConn()) {
			mBtSppCli = null;
			return false;
		}
		
		return true;
	}

	/**
	 * 关闭并释放连接
	 * 
	 * @return void
	 * */
	synchronized public void closeConn() {
		if (null != mBtSppCli) {
			mBtSppCli.closeConn();
			mBtSppCli = null;
		}
	}
	
	/**
	 * 获取应用实例
	 * 
	 * @return BtSppApp
	 */
	synchronized public static BtSppApp getApplication() {
        return sBtSppApp;
    }
}
