package io.lava.ion.fragments.about;

import io.lava.ion.R;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutFragment extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_about, container, false);
		
		TextView versionText = (TextView)v.findViewById(R.id.app_version);

		// display app version name
		try {
			PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getApplicationContext().getPackageName(), 0);
			String version = pInfo.versionName;
			versionText.setText("v" + version);
		} catch (NameNotFoundException e) {
			versionText.setText("Version Unknown");
		}

		return v;
	}
}
