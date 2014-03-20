package mobi.dzs.android.BLE_SPP_PRO;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.text.TextUtils;

public class Settings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.bt_dev_preferences);
		
		findPreference(BtSppApp.BT_DEV_NAME).setOnPreferenceChangeListener(this);
		findPreference(BtSppApp.BT_DEV_MAC).setOnPreferenceChangeListener(this);
		
		updateUi();
	}
	
	private void updateUi() {
		EditTextPreference preference;
		String settingValue;

		preference = (EditTextPreference) findPreference(BtSppApp.BT_DEV_NAME);
		settingValue = BtSppApp.getApp().getSavedBtDevName();
		//preference.setText(settingValue);
		preference.setSummary(TextUtils.isEmpty(settingValue) ?
				getString(R.string.bt_dev_name_summ) : settingValue);

		preference = (EditTextPreference) findPreference(BtSppApp.BT_DEV_MAC);
		settingValue = BtSppApp.getApp().getSavedBtDevMac();
		//preference.setText(settingValue);
		preference.setSummary(TextUtils.isEmpty(settingValue) ?
				getString(R.string.bt_dev_mac_summ) : settingValue);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		if (key == null)
			return false;

		String summary;

		if (key.equals(BtSppApp.BT_DEV_NAME)) {
			summary = TextUtils.isEmpty((String) newValue) ? getString(R.string.bt_dev_name_summ)
					: (String) newValue;
			preference.setSummary(summary);
			return true;
		} else if (key.equals(BtSppApp.BT_DEV_MAC)) {
			summary = TextUtils.isEmpty((String) newValue) ? getString(R.string.bt_dev_mac_summ)
					: (String) newValue;
			preference.setSummary(summary);
			return true;
		}

		return false;
	}

}