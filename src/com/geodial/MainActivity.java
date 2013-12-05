/*
 * Copyright (C) 2013 Sasha Vasko <sasha at aftercode dot net> 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geodial;

import java.util.List;

import com.geodial.R;


import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.widget.TextView;
import android.telephony.*;

public class MainActivity extends Activity {

	final String TAG = "GeoDial";
	final boolean display = false;
	
	protected void chooseActivity (Intent intent, String title) {
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
		boolean isIntentSafe = activities.size() > 0;
		if (Log.isLoggable(TAG, Log.DEBUG))
			Log.d(TAG, "Intent can be handled by " + activities.size() + " activities");  
		if (isIntentSafe) {
			startActivity(Intent.createChooser(intent, title));
		}else if (Log.isLoggable(TAG, Log.ERROR))
			Log.e(TAG, "No apps are available.");
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
		Intent intent = getIntent();
        Uri data = intent.getData();

        if (display) {
        	setContentView(R.layout.activity_main);
        	TextView tv = (TextView)findViewById (R.id.text_view);
        	tv.setText(data.toString());
        }
        	
        if (data.getScheme().equals("geo")) {
        	if (!display) {
        		this.setVisible(false);
        		finish();
        	}
        	String query = data.getEncodedQuery();
        	if (Log.isLoggable(TAG, Log.DEBUG))
        		Log.d(TAG, "query = [" + query + "]");

        	String number = (query != null && query.length() >= 9)? query.substring(2) : null;
        	if (number != null) {
        		number = number.replaceAll("[xp]|[cp]", new String(new char[]{PhoneNumberUtils.PAUSE,PhoneNumberUtils.PAUSE}));
        		number = number.replaceAll("[w]", Character.toString(PhoneNumberUtils.WAIT));
        		if (data.toString().contains("#"))
        			number += "#";
        		// making sure we have a valid number to call. The following removes all non-phone characters from the number :
        		number = PhoneNumberUtils.extractNetworkPortion(number) + PhoneNumberUtils.extractPostDialPortion(number); 
        		if (Log.isLoggable(TAG, Log.DEBUG))
        			Log.d(TAG, "number = [" + number + "]");
        	}
        	
        	if (number == null || number.isEmpty() ) {
        		Intent callIntent = new Intent(Intent.ACTION_VIEW, data);
        		callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		chooseActivity (callIntent, getResources().getString(R.string.geo_chooser_title));
        	} else {
        		// If DIAL is used the dialer's Dialtacs activity is launched instead and it drops everything after , or ;
        		// must use CALL, which sends number directly to phone app
        		// Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"+number));
        		Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel",number,null));
        		callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        		chooseActivity (callIntent, getResources().getString(R.string.chooser_title));
        	}
        }
    }
}
