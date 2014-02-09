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

import com.geodial.DialScript;
import com.geodial.R;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

	final static String TAG = "GeoDial";
	final boolean display = true;
	
	private DialScript script = new DialScript();
	private TextView numberView = null;
	private ListView scriptView = null;
	private DialScriptListAdapter scriptViewAdapter = null;
	private LinearLayout buttonsLayout = null;
	PackageManager packageManager = null;
	
	List<ResolveInfo> handlers = null;
	List<ResolveInfo> handlersContacts = null;

	
	protected void updateHandlers () {
		Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel","1234567890",null));
		callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		handlers = packageManager.queryIntentActivities(callIntent, 0);

		Intent contactIntent = new Intent(Intent.ACTION_INSERT);
		contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, "1234567890");
		contactIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		handlersContacts = packageManager.queryIntentActivities(contactIntent, 0);
		
		updateButtonsView();
	}
	
	protected void chooseActivity (Intent intent, String title) {
		PackageManager packageManager = getPackageManager();
		List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
		boolean isIntentSafe = activities.size() > 0;
		if (BuildConfig.DEBUG)
			Log.d(TAG, "Intent can be handled by " + activities.size() + " activities");  
		if (isIntentSafe) {
			startActivity(Intent.createChooser(intent, title));
		}else if (BuildConfig.DEBUG)
			Log.e(TAG, "No apps are available.");
	}
	
	@Override
    public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
       		setContentView(R.layout.activity_main);
		} else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			setContentView(R.layout.activity_main_landscape);
		}
		refreshControls ();
    }
	
	protected void refreshControls () {
    	numberView = (TextView)findViewById (R.id.textFinalNumber);
    	scriptView = (ListView)findViewById (R.id.script_view);
    	buttonsLayout = (LinearLayout)findViewById (R.id.buttons_layout);
    	if (scriptView != null){
    		scriptView.setAdapter(scriptViewAdapter);
    		scriptView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    		scriptView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,	int arg2, long arg3) {
					scriptViewAdapter.setSelection(arg2);
				}
    		});
    		scriptView.setItemsCanFocus(true);
    		scriptView.setFocusableInTouchMode(true);
    		scriptViewAdapter.setSelection (-1);
    	}
        connectDialpadButtons();
		updateView();
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    	//script.parseString("(123) 456-7891*9x987", true);
    	packageManager = getPackageManager();
    	updateHandlers();
		scriptViewAdapter = new DialScriptListAdapter(this);
		if (BuildConfig.DEBUG)
			Log.d(TAG, "onCreate()");
        if (display) {
        	if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) 
        		setContentView(R.layout.activity_main_landscape);
        	else
        		setContentView(R.layout.activity_main);
        	refreshControls();
        }
        handleIntent (getIntent());
    }

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		handleIntent (intent);
	}
	
	protected void handleIntent (Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
    		if (BuildConfig.DEBUG)
    			Log.d(TAG, "data = [" + data + "]");
        	if (display) {
        		TextView tv = (TextView)findViewById (R.id.text_view);
        		tv.setText(data.toString());
        	}
        	String number = null;
        	if (data.getScheme().equals("geo")) {
        		if (!display) {
        			this.setVisible(false);
        			finish();
        		}

        		String query = data.getEncodedQuery();
        		if (BuildConfig.DEBUG)
        			Log.d(TAG, "query = [" + query + "]");

        		number = (query != null && query.length() > 2 )? query.substring(2) : null;
        		/*
        	if (number != null) {
        		number = number.replaceAll("[xp]|[cp]", new String(new char[]{PhoneNumberUtils.PAUSE,PhoneNumberUtils.PAUSE}));
        		number = number.replaceAll("[w]", Character.toString(PhoneNumberUtils.WAIT));
        		if (data.toString().contains("#"))
        			number += "#";
        		// making sure we have a valid number to call. The following removes all non-phone characters from the number :
        		number = PhoneNumberUtils.extractNetworkPortion(number) + PhoneNumberUtils.extractPostDialPortion(number); 
        		if (Log.isLoggable(TAG, Log.DEBUG))
        			Log.d(TAG, "number = [" + number + "]");
        	}*/

        		// Se if that was not something we could handle:
        	}else if(data.getScheme().equals("tel")) {
        		number = data.getSchemeSpecificPart();
        	}
    		if (number == null || number.isEmpty() ) {
    			Intent callIntent = new Intent(Intent.ACTION_VIEW, data);
    			callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			chooseActivity (callIntent, getResources().getString(R.string.geo_chooser_title));
    			return;
    		}
    		updateHandlers();
    		handleNumber (number, data.toString().contains("#"));
        }
	}
	
	protected void handleNumber (String number, boolean appendHash) {
		script.parseString(number, appendHash);
		updateView();
	}
	
	public void dial (ComponentName component) {
    	// If DIAL is used the dialer's Dialtacs activity is launched instead and it drops everything after , or ;
    	// must use CALL, which sends number directly to phone app
    	Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel",script.toString(),null));
    	callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	callIntent.setComponent(component);
    	startActivity (callIntent);
	}

	public void addContact (ComponentName component) {
    	Intent contactIntent = new Intent(Intent.ACTION_INSERT);
		contactIntent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		contactIntent.putExtra(ContactsContract.Intents.Insert.PHONE, script.toString());
    	contactIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	contactIntent.setComponent(component);
    	startActivity (contactIntent);
	}

	protected void updateView() {
		if (display) {
			updateScriptView();
			updateNumberView();
			updateButtonsView();
		}
	}
	protected void updateScriptView() {
		updateScriptView(0);
	}
	protected void updateNumberView() {
		if (numberView != null) {
			numberView.setText(script.toString());
		}
	}
	
	public class ActivityClickListener implements OnClickListener {
		private ResolveInfo resolveInfo;
		private boolean contact;
		ActivityClickListener (ResolveInfo ri, boolean contact) {
			super();
			resolveInfo = ri;
			this.contact = contact; 
		}
		@Override
		public void onClick(View v) {
			ActivityInfo activity = resolveInfo.activityInfo;
			if (contact)
				addContact (new ComponentName(activity.applicationInfo.packageName, activity.name));
			else
				dial (new ComponentName(activity.applicationInfo.packageName, activity.name));
		}
	}

	public class DialpadClickListener implements OnClickListener {
		char c;
		public DialpadClickListener(char c) {
			super();
			this.c = c;
		}
		
		@Override
		public void onClick(View v) {
			switch (c) {
				case '\b': backspace(); break;
				case 'p' : addPause(); break;
				default : appendChar(c); 
			}
		}
	}

	protected void updateButtonsView() {
		if (buttonsLayout != null) {
			buttonsLayout.removeAllViews();
			for (ResolveInfo ri : handlers) {
				ImageButton ib = new ImageButton (this);
				ib.setOnClickListener(new ActivityClickListener (ri, false));
				ib.setImageDrawable(ri.activityInfo.loadIcon(packageManager));
				buttonsLayout.addView (ib);
			}
			for (ResolveInfo ri : handlersContacts) {
				ImageButton ib = new ImageButton (this);
				ib.setOnClickListener(new ActivityClickListener (ri, true));
				ib.setImageDrawable(ri.activityInfo.loadIcon(packageManager));
				buttonsLayout.addView (ib);
			}
		}
	}
	
	public void connectDialpadButton (int buttonId, char c){
		View v = findViewById (buttonId);
		if (v instanceof Button)
			((Button)v).setOnClickListener(new DialpadClickListener(c));
		else if (v instanceof ImageButton)
			((ImageButton)v).setOnClickListener(new DialpadClickListener(c));
	}
	
	private void connectDialpadButtons(){
		connectDialpadButton(R.id.button_0, '0');
		connectDialpadButton(R.id.button_1, '1');
		connectDialpadButton(R.id.button_2, '2');
		connectDialpadButton(R.id.button_3, '3');
		connectDialpadButton(R.id.button_4, '4');
		connectDialpadButton(R.id.button_5, '5');
		connectDialpadButton(R.id.button_6, '6');
		connectDialpadButton(R.id.button_7, '7');
		connectDialpadButton(R.id.button_8, '8');
		connectDialpadButton(R.id.button_9, '9');
		connectDialpadButton(R.id.button_Hash, '#');
		connectDialpadButton(R.id.button_Star, '*');
		connectDialpadButton(R.id.button_Backspace, '\b');
		connectDialpadButton(R.id.button_Pause, 'p');
	}
	
	private class DialScriptItemView  extends RelativeLayout {
        private TextView title;
        private TextView length;
        private TextView number;
        
        public static final int TITLE_ID = 1000;
        public static final int LENGTH_ID = 1001;
        public static final int NUMBER_ID = 1002;
        
        @SuppressLint("NewApi") public DialScriptItemView(Context context) {
            super(context);
            //setOrientation(HORIZONTAL);
            setBackgroundResource(R.drawable.list_item_back_selector);
            setLayoutParams(new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            ColorStateList textCSL = null;
            XmlResourceParser textXRP = getResources().getXml(R.drawable.list_item_text_selector);  
            try {  
                textCSL = ColorStateList.createFromXml(getResources(), textXRP);  
            } catch (Exception e) {  } 
            title = new TextView(context);
            title.setText(context.getString(R.string.pause_title));
            title.setTextAppearance(context, R.style.PauseStyle);
            title.setTextColor(textCSL);
            title.setId(TITLE_ID);
            length = new TextView(context);
            length.setTextAppearance(context, R.style.PauseStyle);
            length.setTextColor(textCSL);
            length.setId(LENGTH_ID);

            number = new TextView(context);
            number.setTextAppearance(context, R.style.NumberStyle);
            number.setTextColor(textCSL);
            number.setId(NUMBER_ID);
        }

        public void setItem(int position, DialScript.Item item) {
            if (item instanceof DialScript.DialNumber) {
                removeView(length);
                removeView(title);
            	number.setText(position == 1?PhoneNumberUtils.formatNumber(item.toString()) : item.toString());
            	if (number.getParent() == null)
            		addView(number, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            }else if (item instanceof DialScript.DialPause){
                removeView(number);
            	DialScript.DialPause pause = (DialScript.DialPause)item;
                if (title.getParent() == null)
                	addView(title, new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                length.setText(Integer.toString(pause.length));
                if (length.getParent() == null) {
                	LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
                	//lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
                	lp.addRule(RelativeLayout.RIGHT_OF, TITLE_ID);
                	addView(length, lp);
                }
            }
        }
    }
 	
    private class DialScriptListAdapter extends BaseAdapter {
        private Context context;
        
        // Can't have focusable ListView with selection in Touch mode,
        // we shall create custom implementation:
        private int selection = -1;
        
        public DialScriptListAdapter(Context context) {
            this.context = context;
        }

        public int getCount() {
            return script.length();
        }

        public Object getItem(int position) {
            return script.getItem(position);
        }

        public DialScript.Item getCurrentItem() {
        	if (selection < 0)
        		return null;
            return script.getItem(selection);
        }

        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) {
            DialScriptItemView siv = convertView == null ? new DialScriptItemView(context) : (DialScriptItemView) convertView;
            if (convertView != null && position != selection)
            	siv.setActivated(false);
            siv.setItem(position, script.getItem(position));
            if (BuildConfig.DEBUG)
                Log.d(TAG,"getView("+position+"), selection == " + selection + ", view = " + siv );
            return siv;
        }

        public void setSelection(int index) {
        	if (BuildConfig.DEBUG)
        	    Log.d(TAG,"setSelection("+index+"), last selection = " + selection );
            if (index >= 0)
                selection = index;

            scriptView.post(new SelectionSetter());
        }
        
        
        public int getSelection() {
            return selection;
        }
        
        public class SelectionSetter implements Runnable {
       	
            @Override
            public void run() {
            	if (BuildConfig.DEBUG)
            	    Log.d(TAG,"SelectionSetter: selection = " + selection );
            	int selPosition = getListItemViewPos(scriptView, selection);
            	int i = scriptView.getChildCount();
            	while (--i >= 0)
            		scriptView.getChildAt(i).setActivated (i == selPosition);
            }
        }
    }

    public static int getListItemViewPos(ListView lv, int position) {
    	// stolen from http://stackoverflow.com/questions/257514/android-access-child-views-from-a-listview\
    	int firstPosition = lv.getFirstVisiblePosition() - lv.getHeaderViewsCount(); // This is the same as child #0
    	// Say, first visible position is 8, you want position 10, wantedChild will now be 2
    	// So that means your view is child #2 in the ViewGroup:
    	return position - firstPosition;
   	}

    public static View getListItemViewAt(ListView lv, int position) {
    	// stolen from http://stackoverflow.com/questions/257514/android-access-child-views-from-a-listview\
    	int wantedChild = getListItemViewPos(lv, position);
    	if (wantedChild < 0 || wantedChild >= lv.getChildCount()) {
    	  Log.w(TAG, "Unable to get view for desired position, because it's not being displayed on screen.");
    	  return null;
    	}
    	// Could also check if wantedPosition is between listView.getFirstVisiblePosition() and listView.getLastVisiblePosition() instead.
    	return lv.getChildAt(wantedChild);
    }
    
    protected void updateScriptView(int selectedItem) {
    	if (BuildConfig.DEBUG)
    	    Log.d(TAG,"updateScriptView("+selectedItem+")" );
        if (selectedItem >= 0)
        	scriptViewAdapter.setSelection (selectedItem);

        scriptViewAdapter.notifyDataSetChanged ();
    	scriptView.clearFocus();
    	scriptView.post(scriptViewAdapter.new SelectionSetter());
    	updateNumberView();
    }
    
    public void appendChar (char c) {
    	DialScript.Item item = scriptViewAdapter.getCurrentItem();
    	int selection = scriptViewAdapter.getSelection();
    	if (item != null) {
    		if (item instanceof DialScript.DialNumber)
    			item.appendChar (c);
    		else if (selection == 0)
    			script.addNumberFront (Character.toString(c));
    		else if (selection < script.length()-1)
    			return;
    		else
    			item = null;
    	}
    		
    	if (item == null) {
    		script.addNumber (Character.toString(c));
    		selection = script.length()-1;
    	}
		updateScriptView (selection);
    }
    
    public void backspace() {
    	DialScript.Item item = scriptViewAdapter.getCurrentItem();
    	if (item != null) {
    		int selection = scriptViewAdapter.getSelection();
    		if (item.backspace ()) {
    			script.removeItem (item);
    			if (selection > 0)
    				selection--;
    		}
    		updateScriptView (selection);
    	}
    }
    public void addPause() {
    	updateScriptView (script.addPause(scriptViewAdapter.getSelection(), 1));
    }
}
