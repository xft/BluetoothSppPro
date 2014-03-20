package mobi.dzs.android.BLE_SPP_PRO;

import mobi.dzs.android.bluetooth.BtSppClient;
import mobi.dzs.android.util.PreferencesStorage;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BtSppApp extends Application {
	/** 蓝牙SPP通信连接对象 */
	public BtSppClient mBtSppCli = null;
	/** 动态公共存储对象 */
	public PreferencesStorage mDS = null;
	/** 应用自引用 */
	private static BtSppApp sBtSppApp = null;

	public static final String BT_DEV_NAME = "bluetooth_device_name";
	public static final String BT_DEV_MAC = "bluetooth_device_mac";
	private SharedPreferences mBtDevPrefs = null;

	/**
	 * 覆盖构造
	 * */
	@Override
	public void onCreate() {
		mDS = new PreferencesStorage(this);
		sBtSppApp = this;
		mBtDevPrefs = PreferenceManager.getDefaultSharedPreferences(this);
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

		mBtSppCli = new BtSppClient(mac);
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
	synchronized public static BtSppApp getApp() {
        return sBtSppApp;
    }

	/**
	 * 保存蓝牙设备名称
	 * @param name 设备名
	 * @see #getSavedBtDevName()
	 * @see #saveBtDevMac(String)
	 */
	public void saveBtDevName(String name) {
		SharedPreferences.Editor editPrefs = mBtDevPrefs.edit();
		editPrefs.putString(BT_DEV_NAME, name);
		editPrefs.commit();
	}

	/**
	 * 保存蓝牙设备硬件地址
	 * @param mac 蓝牙硬件地址
	 * @see #getSavedBtDevMac()
	 * @see #saveBtDevName(String)
	 */
	public void saveBtDevMac(String mac) {
		SharedPreferences.Editor editPrefs = mBtDevPrefs.edit();
		editPrefs.putString(BT_DEV_MAC, mac);
		editPrefs.commit();
	}

	/**
	 * 获取保存的蓝牙设备名称
	 * @return 蓝牙设备名
	 * @see #saveBtDevName(String)
	 * @see #getSavedBtDevMac()
	 */
	public String getSavedBtDevName() {
		return mBtDevPrefs.getString(BT_DEV_NAME, "");
	}

	/**
	 * 获取保存的蓝牙设备硬件地址
	 * @return 蓝牙硬件地址
	 * @see #saveBtDevMac(String)
	 * @see #getSavedBtDevName()
	 */
	public String getSavedBtDevMac() {
		return mBtDevPrefs.getString(BT_DEV_MAC, "");
	}
}
