package mobi.dzs.android.BLE_SPP_PRO;

import java.util.ArrayList;
import java.util.Hashtable;

import mobi.dzs.android.bluetooth.BluetoothCtrl;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * 主界面<br />
 * 维护蓝牙的连接与通信操作，首先进入后检查蓝牙状态，没有启动则开启蓝牙，然后立即进入搜索界面。<br/>
 * 得到需要连接的设备后，在主界面中建立配对与连接，蓝牙对象被保存在globalPool中，以便其他的不同通信模式的功能模块调用。
 * 
 * @author JerryLi
 * 
 */
public class actMain extends Activity {
	/** CONST: scan device menu id */
	public static final byte MEMU_RESCAN = 0x01;
	/** CONST: exit application */
	public static final byte MEMU_EXIT = 0x02;
	/** CONST: about me */
	public static final byte MEMU_ABOUT = 0x03;
	/** 全局静态对象池 */
	private BtSppApp mBtSppApp = null;
	/** 手机的蓝牙适配器 */
	private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
	/** 蓝牙设备连接句柄 */
	private BluetoothDevice mBtDev = null;
	/** 控件:Device Info显示区 */
	private TextView mDevInfo = null;
	/** 控件:Service UUID显示区 */
	private TextView mSrvUUID = null;
	/** 控件:设备信息显示区容器 */
	private LinearLayout mDevInfoView = null;
	/** 控件:选择连接成功后的设备通信模式面板 */
	private LinearLayout mChoseModeView = null;
	/** 控件:配对按钮 */
	private Button mBtnPair = null;
	/** 控件:通信按钮 */
	private Button mBtnComm = null;
	/** 常量:搜索页面返回 */
	public static final byte REQUEST_DISCOVERY = 0x01;
	/** 常量:从字符流模式返回 */
	public static final byte REQUEST_BYTE_STREAM = 0x02;
	/** 常量:从命令行模式返回 */
	public static final byte REQUEST_CMD_LINE = 0x03;
	/** 常量:从键盘模式返回 */
	public static final byte REQUEST_KEY_BOARD = 0x04;
	/** 常量:从关于页面返回 */
	public static final byte REQUEST_ABOUT = 0x05;
	/** 选定设备的配置信息 */
	private Hashtable<String, String> mDevInfoTxt = new Hashtable<String, String>();
	/** 蓝牙配对进程操作标志 */
	private Boolean mBonded = false;
	/** 获取到的UUID Service 列表信息 */
	private ArrayList<String> mUuidList = new ArrayList<String>();
	/** 保存蓝牙进入前的开启状态 */
	private boolean mBtOriginIsOpen = false;
	
	private static final String BTDEV_EXTRA_UUID = "android.bluetooth.device.extra.UUID";
	
	/** 广播监听:获取UUID服务 */
	private BroadcastReceiver _mGetUuidServiceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent intent) {
			String action = intent.getAction();
			int iLoop = 0;
			
			if (BluetoothDevice.ACTION_UUID.equals(action)) {
				Parcelable[] uuidExtra = intent.getParcelableArrayExtra(BTDEV_EXTRA_UUID);
				if (null != uuidExtra)
					iLoop = uuidExtra.length;
				/*
				 * uuidExtra should contain my service's UUID among his files,
				 * but it doesn't!!
				 */
				for (int i = 0; i < iLoop; i++)
					mUuidList.add(uuidExtra[i].toString());
			}
		}
	};
	
	/** 广播监听:蓝牙配对处理 */
	private BroadcastReceiver _mPairingRequest = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			BluetoothDevice device = null;
			if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
				// 配对状态改变时的广播处理
				device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
					// 蓝牙配对设置成功
					synchronized (mBonded) {
						mBonded = true;
					}
				} else {
					// 蓝牙配对进行中或者配对失败
					synchronized (mBonded) {
						mBonded = false;
					}
				}
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.act_main_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.actMain_menu_rescan:
			// 关闭连接
			mBtSppApp.closeConn();
			// 进入扫描时，显示界面初始化
			initActivityView();
			// 进入搜索页面
			openDiscovery();
			return true;
		case R.id.actMain_menu_exit:
			finish();
			return true;
		case R.id.actMain_menu_about:
			// 打开关于页面
			openAbout();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * 页面构造
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.act_main);

		mDevInfo = (TextView) findViewById(R.id.actMain_device_info);
		mSrvUUID = (TextView) findViewById(R.id.actMain_service_uuid);
		mDevInfoView = (LinearLayout) findViewById(R.id.actMain_device_info_view);
		mChoseModeView = (LinearLayout) findViewById(R.id.actMain_choose_mode_view);
		mBtnPair = (Button) findViewById(R.id.actMain_btn_pair);
		mBtnComm = (Button) findViewById(R.id.actMain_btn_conn);

		// 初始化窗口控件的视图
		initActivityView();

		// 得到全局对象的引用
		mBtSppApp = BtSppApp.getApplication(); 

		// 启动蓝牙设备
		new startBluetoothDeviceTask().execute(""); 
	}

	/**
	 * 初始化显示界面的控件
	 * 
	 * @return void
	 * */
	private void initActivityView() {
		mDevInfoView.setVisibility(View.GONE); // 隐藏 扫描到的设备信息
		mBtnPair.setVisibility(View.GONE); // 隐藏 配对按钮
		mBtnComm.setVisibility(View.GONE); // 隐藏 连接按钮
		mChoseModeView.setVisibility(View.GONE); // 隐藏 通信模式选择
	}

	/**
	 * 析构处理
	 * */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		mBtSppApp.closeConn();// 关闭连接

		// 检查如果进入前蓝牙是关闭的状态，则退出时关闭蓝牙
		if (!mBtOriginIsOpen)
			mBtAdapter.disable();
	}

	/**
	 * 进入搜索蓝牙设备列表页面
	 * */
	private void openDiscovery() {
		// 进入蓝牙设备搜索界面
		Intent intent = new Intent(this, actDiscovery.class);
		startActivityForResult(intent, REQUEST_DISCOVERY); // 等待返回搜索结果
	}

	/**
	 * 进入关于页面
	 * */
	private void openAbout() {
		// 进入蓝牙设备搜索界面
		Intent intent = new Intent(this, actAbout.class);
		startActivityForResult(intent, REQUEST_ABOUT); // 等待返回搜索结果
	}

	/**
	 * 显示选中设备的信息
	 * 
	 * @return void
	 * */
	private void showDeviceInfo() {
		/* 显示需要连接的设备信息 */
		mDevInfo.setText(String.format(
				getString(R.string.actMain_device_info),
				mDevInfoTxt.get("NAME"), mDevInfoTxt.get("MAC"),
				mDevInfoTxt.get("COD"), mDevInfoTxt.get("RSSI"),
				mDevInfoTxt.get("DEVICE_TYPE"),
				mDevInfoTxt.get("BOND")));
	}

	/**
	 * 显示Service UUID信息
	 * 
	 * @return void
	 * */
	private void showServiceUUIDs() {
		if (Build.VERSION.SDK_INT >= 15) {
			// 对于4.0.3以上的系统支持获取UUID服务内容的操作
			new GetUUIDServiceTask().execute("");
		} else {
			// 不支持获取uuid service信息
			mSrvUUID.setText(getString(R.string.actMain_msg_does_not_support_uuid_service));
		}
	}

	/**
	 * 蓝牙设备选择完后返回处理
	 * */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_DISCOVERY:
			if (Activity.RESULT_OK == resultCode) {
				mDevInfoView.setVisibility(View.VISIBLE); // 显示设备信息区

				mDevInfoTxt.put("NAME", data.getStringExtra("NAME"));
				mDevInfoTxt.put("MAC", data.getStringExtra("MAC"));
				mDevInfoTxt.put("COD", data.getStringExtra("COD"));
				mDevInfoTxt.put("RSSI", data.getStringExtra("RSSI"));
				mDevInfoTxt.put("DEVICE_TYPE", data.getStringExtra("DEVICE_TYPE"));
				mDevInfoTxt.put("BOND", data.getStringExtra("BOND"));

				showDeviceInfo();// 显示设备信息

				// 如果设备未配对，显示配对操作
				if (mDevInfoTxt.get("BOND").equals(getString(R.string.actDiscovery_bond_nothing))) {
					mBtnPair.setVisibility(View.VISIBLE); // 显示配对按钮
					mBtnComm.setVisibility(View.GONE); // 隐藏通信按钮
					// 提示要显示Service UUID先建立配对
					mSrvUUID.setText(getString(R.string.actMain_tv_hint_service_uuid_not_bond));
				} else {
					// 已存在配对关系，建立与远程设备的连接
					mBtDev = mBtAdapter.getRemoteDevice(mDevInfoTxt.get("MAC"));
					showServiceUUIDs();// 显示设备的Service UUID列表
					mBtnPair.setVisibility(View.GONE); // 隐藏配对按钮
					mBtnComm.setVisibility(View.VISIBLE); // 显示通信按钮
				}
			} else if (Activity.RESULT_CANCELED == resultCode) {
				// 未操作，结束程序
				finish();
			}
			break;
		case REQUEST_BYTE_STREAM:
		case REQUEST_CMD_LINE:
		case REQUEST_KEY_BOARD:
			// 从通信模式返回的处理
			if (null == mBtSppApp.mBtSppCli || !mBtSppApp.mBtSppCli.isConnect()) {
				// 通信连接丢失，重新连接
				mChoseModeView.setVisibility(View.GONE); // 隐藏 通信模式选择
				mBtnComm.setVisibility(View.VISIBLE); // 显示 建立通信按钮
				mBtSppApp.closeConn();// 释放连接对象
				// 提示连接丢失
				Toast.makeText(this, R.string.msg_bt_connect_lost, Toast.LENGTH_SHORT).show();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * 配对按钮的单击事件
	 * 
	 * @return void
	 * */
	public void onClickPairBtn(View v) {
		new PairTask().execute(mDevInfoTxt.get("MAC"));
		mBtnPair.setEnabled(false); // 冻结配对按钮
	}

	/**
	 * 建立设备的串行通信连接 建立成功后出现通信模式的选择按钮
	 * 
	 * @return void
	 * */
	public void onClickConnBtn(View v) {
		new connSocketTask().execute(mBtDev.getAddress());
	}

	/**
	 * 通信模式选择-串行流模式
	 * 
	 * @return void
	 * */
	public void onClickSerialStreamModeBtn(View v) {
		// 进入串行流模式
		Intent intent = new Intent(this, actByteStream.class);
		startActivityForResult(intent, REQUEST_BYTE_STREAM); // 等待返回搜索结果
	}

	/**
	 * 通信模式选择-键盘模式
	 * 
	 * @return void
	 * */
	public void onClickKeyBoardModeBtn(View v) {
		// 进入键盘模式界面
		Intent intent = new Intent(this, actKeyBoard.class);
		startActivityForResult(intent, REQUEST_KEY_BOARD); // 等待返回搜索结果
	}

	/**
	 * 通信模式选择-命令行模式
	 * 
	 * @return void
	 * */
	public void onClickCommandLineBtn(View v) {
		// 进入命令行模式界面
		Intent intent = new Intent(this, actCmdLine.class);
		startActivityForResult(intent, REQUEST_CMD_LINE); // 等待返回搜索结果
	}

	// ----------------
	/* 多线程处理(开机时启动蓝牙) */
	private class startBluetoothDeviceTask extends AsyncTask<String, String, Integer> {
		/** 常量:蓝牙已经启动 */
		private static final int RET_BULETOOTH_IS_START = 0x0001;
		/** 常量:休眠遇到错误(不应该出现这个错误) */
		private static final int RET_SLEEP_FAILE = 0x0002;
		/** 常量:设备启动失败 */
		private static final int RET_BLUETOOTH_START_FAIL = 0x04;

		/** 等待蓝牙设备启动的最长时间(单位S) */
		private static final int WATI_TIME = 15;
		/** 每次线程休眠时间(单位ms) */
		private static final int SLEEP_TIME = 150;
		/** 进程等待提示框 */
		private ProgressDialog mprogressDialog;

		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute() {
			/* 定义进程对话框 */
			mprogressDialog = new ProgressDialog(actMain.this);
			// 蓝牙启动中
			mprogressDialog.setMessage(getString(R.string.actDiscovery_msg_starting_device));
			// 不可被终止
			mprogressDialog.setCancelable(false);
			// 点击外部不可终止
			mprogressDialog.setCanceledOnTouchOutside(false);
			mprogressDialog.show();
			// 保存进入前的蓝牙状态
			mBtOriginIsOpen = mBtAdapter.isEnabled();
		}

		/** 异步的方式启动蓝牙，如果蓝牙已经启动则直接进入扫描模式 */
		@Override
		protected Integer doInBackground(String... arg0) {
			int wait = WATI_TIME * 1000;// 倒减计数器
			/* BT isEnable */
			if (mBtAdapter.isEnabled())
				return RET_BULETOOTH_IS_START;

			mBtAdapter.enable(); // 启动蓝牙设备

			// 等待miSLEEP_TIME秒，启动蓝牙设备后再开始扫描
			while (wait > 0) {
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					return RET_SLEEP_FAILE; // 延迟错误
				}
				
				wait -= SLEEP_TIME; // 剩余等待时间计时
				
				if (mBtAdapter.isEnabled())
					return RET_BULETOOTH_IS_START; // 启动成功
			}

			return RET_BLUETOOTH_START_FAIL;
		}

		/**
		 * 阻塞任务执行完后的清理工作
		 */
		@Override
		public void onPostExecute(Integer result) {
			if (mprogressDialog.isShowing())
				mprogressDialog.dismiss();// 关闭等待对话框

			if (RET_BLUETOOTH_START_FAIL == result) { // 蓝牙设备启动失败
				AlertDialog.Builder builder = new AlertDialog.Builder(actMain.this); // 对话框控件
				builder.setTitle(getString(R.string.dialog_title_sys_err));// 设置标题
				builder.setMessage(getString(R.string.actDiscovery_msg_start_bluetooth_fail));
				builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								mBtAdapter.disable();
								// 蓝牙设备无法启动，直接终止程序
								finish();
							}
						});
				builder.create().show();
			} else if (RET_BULETOOTH_IS_START == result) { // 蓝牙启动成功
				openDiscovery(); // 进入搜索页面
			}
		}
	}

	// ----------------
	/* 多线程处理(配对处理线程) */
	private class PairTask extends AsyncTask<String, String, Integer> {
		/** 常量:配对成功 */
		static private final int RET_BOND_OK = 0x00;
		/** 常量: 配对失败 */
		static private final int RET_BOND_FAIL = 0x01;
		/** 常量: 配对等待时间(10秒) */
		static private final int TIMEOUT = 1000 * 10;

		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute() {
			// 提示开始建立配对
			Toast.makeText(actMain.this, R.string.actMain_msg_bluetooth_Bonding, Toast.LENGTH_SHORT).show();
			/* 蓝牙自动配对 */
			// 监控蓝牙配对请求
			registerReceiver(_mPairingRequest, new IntentFilter(BluetoothCtrl.PAIRING_REQUEST));
			// 监控蓝牙配对是否成功
			registerReceiver(_mPairingRequest, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
		}

		@Override
		protected Integer doInBackground(String... arg0) {
			final int stepTime = 150;
			int wait = TIMEOUT; // 设定超时等待时间
			// 开始配对
			try {
				// 获得远端蓝牙设备
				mBtDev = mBtAdapter.getRemoteDevice(arg0[0]);
				BluetoothCtrl.createBond(mBtDev);
				synchronized (mBonded) {
					mBonded = false; // 初始化配对完成标志
				}
			} catch (Exception e1) { // 配对启动失败
				Log.w(getString(R.string.app_name), "create Bond failed!");
				e1.printStackTrace();
				return RET_BOND_FAIL;
			}
			
			while (wait > 0) {
				try {
					Thread.sleep(stepTime);
				} catch (InterruptedException e) {
				}
				
				wait -= stepTime;
				
				synchronized (mBonded) {
					if (mBonded)
						break;
				}
			}
			
			return (int) ((wait > 0) ? RET_BOND_OK : RET_BOND_FAIL);
		}

		/**
		 * 阻塞任务执行完后的清理工作
		 */
		@Override
		public void onPostExecute(Integer result) {
			unregisterReceiver(_mPairingRequest); // 注销监听

			if (RET_BOND_OK == result) {
				// 配对建立成功
				Toast.makeText(actMain.this, R.string.actMain_msg_bluetooth_Bond_Success, Toast.LENGTH_SHORT).show();
				mBtnPair.setVisibility(View.GONE); // 隐藏配对按钮
				mBtnComm.setVisibility(View.VISIBLE); // 显示通信按钮
				mDevInfoTxt.put("BOND", getString(R.string.actDiscovery_bond_bonded));// 显示已绑定
				showDeviceInfo(); // 刷新配置信息
				showServiceUUIDs(); // 显示远程设备提供的服务
			} else { // 在指定时间内未完成配对
				Toast.makeText(actMain.this, R.string.actMain_msg_bluetooth_Bond_fail, Toast.LENGTH_LONG).show();
				try {
					BluetoothCtrl.removeBond(mBtDev);
				} catch (Exception e) {
					Log.d(getString(R.string.app_name), "removeBond failed!");
					e.printStackTrace();
				}
				
				mBtnPair.setEnabled(true); // 解冻配对按钮
			}
		}
	}

	// ----------------
	/* 多线程处理(读取UUID Service信息线程) */
	private class GetUUIDServiceTask extends AsyncTask<String, String, Integer> {
		/** 延时等待时间 */
		private static final int WATI_TIME = 4 * 1000;
		/** 每次检测的时间 */
		private static final int REF_TIME = 200;
		/** uuis find service is run */
		private boolean mfindServiceIsRun = false;

		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute() {
			mUuidList.clear();
			// 提示UUID服务搜索中
			mSrvUUID.setText(getString(R.string.actMain_find_service_uuids));
			/* Register the BroadcastReceiver
			 * Don't forget to unregister during onDestroy */
			registerReceiver(_mGetUuidServiceReceiver, new IntentFilter(BluetoothDevice.ACTION_UUID));
			mfindServiceIsRun = mBtDev.fetchUuidsWithSdp();
		}

		/**
		 * 线程异步处理
		 */
		@Override
		protected Integer doInBackground(String... arg0) {
			int wait = WATI_TIME;// 倒减计数器
			int uuidList = 0;
			StringBuilder sbTmp = new StringBuilder();

			if (!this.mfindServiceIsRun)
				return null; // UUID Service扫瞄服务器启动失败

			while (wait > 0) {
				if (mUuidList.size() > 0 && wait > 1500)
					wait = 1500; // 如果找到了第一个UUID则继续搜索N秒后结束
				
				try {
					Thread.sleep(REF_TIME);
				} catch (InterruptedException e) {
				}
				
				wait -= REF_TIME;// 每次循环减去刷新时间

				// 如果存在数据，则自动刷新
				if (mUuidList.size() > 0 && mUuidList.size() != uuidList) {
					for (int i = 0; i < mUuidList.size(); i++)
						sbTmp.append(mUuidList.get(i) + "\n");
					
					publishProgress(sbTmp.toString());
					
					uuidList = mUuidList.size();// 保存当前列表中的数据量
				}
			}
			
			return null;
		}

		/**
		 * 线程内更新UI
		 */
		protected void onProgressUpdate(String... progress) {
			mSrvUUID.setText(progress[0]);
		}

		/**
		 * 阻塞任务执行完后的清理工作
		 */
		@Override
		public void onPostExecute(Integer result) {
			unregisterReceiver(_mGetUuidServiceReceiver); // 注销监听

			if (mUuidList.size() == 0) // 未发现UUIS服务列表
				mSrvUUID.setText(R.string.actMain_not_find_service_uuids);
		}
	}

	// ----------------
	/* 多线程处理(建立蓝牙设备的串行通信连接) */
	private class connSocketTask extends AsyncTask<String, String, Integer> {
		/** 进程等待提示框 */
		private ProgressDialog mProgressDialog = null;
		/** 常量:连接建立失败 */
		private static final int CONN_FAIL = 0x01;
		/** 常量:连接建立成功 */
		private static final int CONN_SUCCESS = 0x02;

		/**
		 * 线程启动初始化操作
		 */
		@Override
		public void onPreExecute() {
			/* 定义进程对话框 */
			mProgressDialog = new ProgressDialog(actMain.this);
			mProgressDialog.setMessage(getString(R.string.actMain_msg_device_connecting));
			mProgressDialog.setCancelable(false);// 可被终止
			mProgressDialog.setCanceledOnTouchOutside(false);// 点击外部可终止
			mProgressDialog.show();
		}

		@Override
		protected Integer doInBackground(String... arg0) {
			return mBtSppApp.createConn(arg0[0]) ? CONN_SUCCESS : CONN_FAIL ;
		}

		/**
		 * 阻塞任务执行完后的清理工作
		 */
		@Override
		public void onPostExecute(Integer result) {
			mProgressDialog.dismiss();
			mProgressDialog = null;

			if (CONN_SUCCESS == result) { // 通信连接建立成功
				mBtnComm.setVisibility(View.GONE); // 隐藏 建立通信按钮
				mChoseModeView.setVisibility(View.VISIBLE); // 显示通信模式控制面板
				Toast.makeText(actMain.this, R.string.actMain_msg_device_connect_succes, Toast.LENGTH_SHORT).show();
			} else { // 通信连接建立失败
				Toast.makeText(actMain.this, R.string.actMain_msg_device_connect_fail, Toast.LENGTH_SHORT).show();
			}
		}
	}
}
