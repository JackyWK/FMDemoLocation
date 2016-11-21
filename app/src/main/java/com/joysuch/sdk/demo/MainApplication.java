package com.joysuch.sdk.demo;


import com.fengmap.android.FMMapSDK;

import android.app.Application;

public class MainApplication extends Application{
	
	
	@Override
	public void onCreate() {
		FMMapSDK.init(this, "8nDxjoveVeOIOMJ3eehu");
		super.onCreate();
	}

}
