package mobi.dzs.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * 系统动态存储 <br/>
 * key -&gt; val 模式存储方式 key由主键与子键联合组成
 * 
 * @author JerryLi(lijian@dzs.mobi)
 * @version 1.0 (2012-07-30)
 * @see setVal()使用后必须用saveStorage()保存数据到存储区，否则设置值将丢失
 * */
public class PreferencesStorage {
	private static final String mDELIMITER = "|_|";
	/** 系统容器 */
	private Context mContext = null;
	/** 包名 */
	private String mPkgName;
	/** 存储器对象 */
	private Editor mSaveData = null;
	private SharedPreferences mSharedPref = null;

	/** 构造函数 */
	public PreferencesStorage(Context context) {
		mContext = context;

		PackageManager manager = mContext.getPackageManager();
		PackageInfo info;
		try {
			info = manager.getPackageInfo(mContext.getPackageName(), 0);
			mPkgName = info.packageName;
			mSharedPref = mContext.getSharedPreferences(mPkgName, Context.MODE_PRIVATE);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			mPkgName = "";
		}
	}

	/**
	 * 自动创建一个新的存储区 (如果不存在存储区对象的话)
	 * */
	private void newStorage() {
		if (null == mSaveData) {
			// 本地存储区
			mSaveData = mContext.getSharedPreferences(mPkgName, Context.MODE_PRIVATE).edit();
		}
	}

	/**
	 * 生成关键字索引名
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return String 合并后的关键字索引名
	 * */
	private String getIdxKey(String sKey, String sSubKey) {
		return sKey + mDELIMITER + sSubKey;
	}

	/**
	 * 保存设置值
	 * 
	 * @return boolean false:失败，不存在值
	 * @see setVal()后，需要使用这个函数保存值
	 * */
	public boolean saveStorage() {
		if (null != mSaveData) {
			mSaveData.commit();// 提交数据保存
			mSaveData = null; // 保存后释放存储区
			return true;
		} else
			return false;
	}

	/**
	 * 保存系统值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @param sVal
	 *            保存的字符串值
	 * */
	public void setVal(String sKey, String sSubKey, String sVal) {
		newStorage();
		mSaveData.putString(getIdxKey(sKey, sSubKey), sVal);
	}

	/**
	 * 保存系统值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @param iVal
	 *            保存的整形值
	 * */
	public void setVal(String sKey, String sSubKey, int iVal) {
		newStorage();
		mSaveData.putInt(getIdxKey(sKey, sSubKey), iVal);
	}

	/**
	 * 保存系统值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @param fVal
	 *            保存的浮点值
	 * */
	public void setVal(String sKey, String sSubKey, float fVal) {
		newStorage();
		mSaveData.putFloat(getIdxKey(sKey, sSubKey), fVal);
	}

	/**
	 * 保存系统值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @param lVal
	 *            保存的长整形
	 * */
	public void setVal(String sKey, String sSubKey, long lVal) {
		newStorage();
		mSaveData.putLong(getIdxKey(sKey, sSubKey), lVal);
	}

	/**
	 * 保存系统值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @param bVal
	 *            保存的布尔值
	 * */
	public void setVal(String sKey, String sSubKey, boolean bVal) {
		newStorage();
		mSaveData.putBoolean(getIdxKey(sKey, sSubKey), bVal);
	}

	/**
	 * 获取关键字保存值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return String /不存在时返回 ""
	 * */
	public String getStringVal(String sKey, String sSubKey) {
		return mSharedPref.getString(getIdxKey(sKey, sSubKey), "");
	}

	/**
	 * 获取关键字保存值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return float /不存在时返回 0.0f
	 * */
	public float getFloatVal(String sKey, String sSubKey) {
		return mSharedPref.getFloat(getIdxKey(sKey, sSubKey), 0.0f);
	}

	/**
	 * 获取关键字保存值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return int /不存在时返回0
	 * */
	public int getIntVal(String sKey, String sSubKey) {
		return mSharedPref.getInt(getIdxKey(sKey, sSubKey), 0);
	}

	/**
	 * 获取关键字保存值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return int /不存在时返回0
	 * */
	public long getLongVal(String sKey, String sSubKey) {
		return mSharedPref.getLong(getIdxKey(sKey, sSubKey), 0);
	}

	/**
	 * 获取关键字保存值
	 * 
	 * @param sKey
	 *            主关键字
	 * @param sSubKey
	 *            子关键字
	 * @return boolean /不存在时返回 false
	 * */
	public boolean getBooleanVal(String sKey, String sSubKey) {
		return mSharedPref.getBoolean(getIdxKey(sKey, sSubKey), false);
	}
}
