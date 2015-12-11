package com.controlanything.NCDTCPRelay;

import java.io.File;
import java.util.ArrayList;

import com.controlanything.NCDTCPRelay.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends Activity{
	
	//Global Objects
	ControlPanel cPanel;
	Intent findDeviceIntent;
	Messenger findDeviceMessenger;
	Typeface font;
	
	//Global Variables
	String[] devices;
	String currentlyEditingDevice;
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;

	//global views
	RelativeLayout titleTable;
	ImageView bottomButton;
	TextView bottomTextView;
	ProgressDialog progressDialog;
	AnimationDrawable saveButtonAnimation;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("API Level of Device: "+currentapiVersion);
		
		cPanel = ((ControlPanel)getApplicationContext());
		devices = cPanel.getStoredString("savedDevices").split(";");
		
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
    	
    	setContentView(mainTable());
    	if(currentapiVersion>=11){
    		saveButtonAnimation.start();
    	}
	}
	
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
	    switch(keycode) {
	    case KeyEvent.KEYCODE_MENU:

	    	File fileList = new File(Environment.getExternalStorageDirectory().getPath());
	    	if (fileList != null){
	    		importExportDialog();
	    	}
	    	else{
	    		Toast toast = Toast.makeText(getBaseContext(), "Feature requires SD Card", Toast.LENGTH_LONG);
	    		toast.show();
	    	}
	    }
	    		
	        	
	            return true;

	}
	
	public RelativeLayout mainTable(){
		RelativeLayout mTable = new RelativeLayout(this);

		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		mTable.setBackgroundResource(R.drawable.background);
		mTable.addView(title());
		
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mTable.addView(findDevicesButton(), bottomButtonParams);
		
		RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		mTable.addView(bottomText(), bottomTextParams);
		
		RelativeLayout.LayoutParams listViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		listViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		listViewParams.addRule(RelativeLayout.ABOVE, bottomTextView.getId());
		mTable.addView(listView(), listViewParams);
		
		return mTable;
	}
	
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		titleTable.setBackgroundResource(R.drawable.top_bar);
        int id = titleTable.generateViewId();
        titleTable.setId(id);

//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
		tView.setText("Devices");
		tView.setTypeface(font);
		tView.setTextSize(30);
		tView.setTextColor(Color.BLACK);
		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		tView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View arg0) {
				System.out.println("tView height = "+tView.getHeight()+"\ntitleTable height = "+titleTable.getHeight());
				
			}
		});
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}
	
	public ImageView findDevicesButton(){
		bottomButton = new ImageView(this);
		int id = bottomButton.generateViewId();
        bottomButton.setId(id);
		if(currentapiVersion >= 11){

			bottomButton.setImageResource(R.drawable.animationxmladddevice);
            //Background image
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);

			saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
			saveButtonAnimation.setEnterFadeDuration(1000);
			saveButtonAnimation.setExitFadeDuration(1000);

			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					findDevicesAlertBuilder();

				}

			});
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_add);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);

			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					findDevicesAlertBuilder();

				}

			});

		}
		
		
		return bottomButton;
	}
	
	public TextView bottomText(){
		bottomTextView = new TextView(this);
		int id = bottomTextView.generateViewId();
        bottomTextView.setId(id);
		bottomTextView.setText("Click and hold device to Edit/Remove");
		bottomTextView.setTextSize(12);
		bottomTextView.setTextColor(Color.WHITE);
		return bottomTextView;
	}
	
	private void importExportDialog(){
		String[] choices = {"Import Settings","Export Settings","Cancel","Share Settings","Receive Settings"};
		AlertDialog aDialog = null;
		AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(this);
        aDialogBuilder.setTitle("Select a File")
        .setItems(choices, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		switch(which){
        		case 0:
        			File fileList = new File(Environment.getExternalStorageDirectory().getPath());
    	        	if (fileList != null){
    	        		File[] filenames = fileList.listFiles();
    	        		importAlertBuilder(filenames);
    	        	}
    	        	break;
        		case 1:
        			exportAlert();
        			break;
        		case 2:
        			dialog.dismiss();
        		case 3:
        			//TODO go to Share Settings Activity
        		case 4:
        			//TODO to to Receive Settings Activity
        		}
        		
        			
        	}
        });
        aDialog = aDialogBuilder.create();
        aDialog.show();
	}
	
	private void importAlertBuilder(File[] items){
		
		ArrayList<String> fileNames = new ArrayList<String>();
		
		
        for (int i = 0; i < items.length; i++){
        	if(items[i].getName().startsWith("NCD")){
        		fileNames.add(items[i].getName().substring(3));
        	}
        }
        
        final String[] files = new String[fileNames.size()];
        for(int i = 0; i < files.length; i++){
        	files[i] = fileNames.get(i);
        }
        
		
        AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(this);
        aDialogBuilder.setTitle("Select a File")
        .setItems(files, new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		File f = new File(Environment.getExternalStorageDirectory().getPath()+"/NCD"+files[which]);
        		cPanel.loadSharedPreferencesFromFile(f);
        		Intent deviceList = new Intent(getBaseContext(), DeviceListActivity.class);
        		startActivity(deviceList);
        	}
        });
        AlertDialog aDialog = aDialogBuilder.create();
        aDialog.show();
		
	}
	
	private void exportAlert(){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(true);

		alert.setTitle("Enter File Name");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setSingleLine();
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();
		  File fileDir = new File(Environment.getExternalStorageDirectory().getPath()+"/NCD"+value);
		  if(cPanel.exportSettings(fileDir)){
			  Toast toast = Toast.makeText(getBaseContext(), "Settings Exported", Toast.LENGTH_LONG);
			  toast.show();
		  }else{
			  Toast toast = Toast.makeText(getBaseContext(), "Settings Failed to Export", Toast.LENGTH_LONG);
			  toast.show();
		  }

		}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});
		alert.show();
	}
	
	public void findDevicesAlertBuilder(){
		AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(this);
		aDialogBuilder.setTitle("Select Connection Type");
		aDialogBuilder.setCancelable(true);
		aDialogBuilder.setItems(R.array.connectionType, new DialogInterface.OnClickListener(){

			public void onClick(DialogInterface dialog, int which) {
				switch(which){
					case 0:
						Intent discoveryIntent = new Intent(getApplicationContext(), DiscoveryActivity.class);
						startActivity(discoveryIntent);
						finish();
						break;
					case 1:
						Intent discoveryBluetoothIntent = new Intent(getApplicationContext(), BluetoothDiscovery.class);
						startActivity(discoveryBluetoothIntent);
						finish();
						break;
				}
				
			}
			
		});
		aDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				
				
			}
		});
		
		AlertDialog aDialog = aDialogBuilder.create();
		aDialog.show();
			
		
	}
	
	public ListView listView(){
		ListView deviceListView = new ListView(this);
//		deviceListView.setLayoutParams(new LayoutParams(displayWidth, (displayHeight - 456)));
    	ArrayAdapter<String> deviceListAdapter =  new ArrayAdapter<String>(this, R.layout.device_name);
    	deviceListView.setAdapter(deviceListAdapter);
    	deviceListView.setOnItemClickListener(deviceSelected);
    	deviceListView.isLongClickable();
    	deviceListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
    	    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
    				long arg3)
    	    {
    	    	System.out.println("long click");

    	    	String info = ((TextView) arg1).getText().toString();
    	    	String[] infoSplit = info.split("\n");
    	    	
    	    	currentlyEditingDevice = infoSplit[1];
    			
    			String deviceSettings = cPanel.getStoredString(currentlyEditingDevice);
    			String[] deviceSettingsSplit = deviceSettings.split(";");
    			if(deviceSettingsSplit.length < 7){
    				//User updated app which didnt have as many settings so fix it.
    				deviceSettings = deviceSettings+";"+"false"+";"+"true"+";"+"false"+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 8){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 9){
    				deviceSettings = deviceSettings+";"+"false"+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 11){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 12){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 13){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 14){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 15){
    				deviceSettings = deviceSettings+";"+"1234";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 16){
    				deviceSettings = deviceSettings+";"+"false";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			if(deviceSettingsSplit.length < 17){
    				deviceSettings = deviceSettings+";"+"1234";
    				cPanel.saveString(currentlyEditingDevice, deviceSettings);
    				deviceSettingsSplit = deviceSettings.split(";");
    			}
    			
    			//WiNet Device
    			if(deviceSettingsSplit.length > 8){
    				if(deviceSettingsSplit[8].equalsIgnoreCase("true")){
    					cPanel.winet = true;
    					cPanel.winetMac = deviceSettingsSplit[0];
    				}else{
    					cPanel.winet = false;
    				}
    			}else{
    				cPanel.winet = false;
    			}

    	    	if(cPanel.getStoredString(infoSplit[1]).split(";")[13].equalsIgnoreCase("true")){
    	    		configPassCodePrompt(infoSplit[1]);
    	    		return true;
    	    	}else{
    	    		System.out.println("Passcode not enabeled");
    	    	}
    	    	
    	    	removeDevice(infoSplit[1]);
				return true;
    	    }
    	});
    	
    	String storedDevices = cPanel.getStoredString("savedDevices");
		if (storedDevices.equals("n/a"))
		{
			deviceListAdapter.add("\nGetting Started \n");
		}
		else{
			System.out.println(storedDevices);
			String[] storedDevicesArray = storedDevices.split(";");
			for(int i = 0; i < storedDevicesArray.length; i++)
			{
				String deviceString = cPanel.getStoredString(storedDevicesArray[i]);
				System.out.println(deviceString);
				String[] deviceStringArray = deviceString.split(";");
				
				if(deviceStringArray.length>1){
					deviceListAdapter.add(deviceStringArray[4] + "\n" + deviceStringArray[0]);
				}
				
				else
				{
					System.out.println("removing invalid device, mac: "+deviceStringArray[0]);
					String newSaveString = null;
					//Device is not valid so delete it
					for(int l = 0; l < storedDevicesArray.length; l++){
						
						if(storedDevicesArray[i].equalsIgnoreCase(storedDevicesArray[l])){
							cPanel.saveString(storedDevicesArray[l], "n/a");
						}else{
							
							if(newSaveString == null){
								newSaveString = storedDevicesArray[l]+";";
							}else{
								newSaveString = newSaveString+storedDevicesArray[l]+";";
							}
						}
					}
					if(newSaveString != null){
						cPanel.saveString("savedDevices", newSaveString);
					}else{
						cPanel.saveString("savedDevices", "n/a");
					}
				}
			}
		}
		
		
		return deviceListView;
		
	}
	
	private void configPassCodePrompt(final String mac){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(true);

		alert.setTitle("Please Enter Config lock Pin");
		alert.setMessage("Default is 1234");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();

		  if(value.equalsIgnoreCase(cPanel.getStoredString(mac).split(";")[14]) || value.equalsIgnoreCase("5644")){
			  removeDevice(mac);
		  }else{
			  dialog.dismiss();
			  Toast toast = Toast.makeText(getBaseContext(), "Incorrect passcode", Toast.LENGTH_LONG);
			  toast.show();

		  }

		}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});
		alert.show();
	}
	
	private void accessPassCodePrompt(final String mac){
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(true);

		alert.setTitle("Please Enter Access Control Pin");
		alert.setMessage("Default is 1234");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		  String value = input.getText().toString();

		  if(value.equalsIgnoreCase(cPanel.getStoredString(mac).split(";")[16]) || value.equalsIgnoreCase("5644")){
			  switchToControlActivity(mac);
		  }else{
			  dialog.dismiss();
			  Toast toast = Toast.makeText(getBaseContext(), "Incorrect passcode", Toast.LENGTH_LONG);
			  toast.show();

		  }

		}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		  }
		});
		alert.show();
	}
	
	private void removeDevice(final String mac){
    	String deviceSettings = cPanel.getStoredString(mac);
    	System.out.println("mac = "+mac);
    	System.out.println("cPanel.getStoredString(mac) = " + deviceSettings);
    	String[] deviceSettingsSplit = deviceSettings.split(";");
    	final String deviceName = deviceSettingsSplit[4];
    	final AlertDialog.Builder removeDeviceAlert = new AlertDialog.Builder(this);
    	removeDeviceAlert.setTitle("Edit/Remove");
    	removeDeviceAlert.setMessage("Edit or Remove: " + deviceName);
    	removeDeviceAlert.setCancelable(true);
    	removeDeviceAlert.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) 
			{


				removeDeviceAlert.setTitle("Are you sure?");
				removeDeviceAlert.setMessage("Remove: " + deviceName);
				removeDeviceAlert.setCancelable(true);
				removeDeviceAlert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						String[] storedDevices = cPanel.getStoredString("savedDevices").split(";");
						String devicesToSave = null;
						for(int i = 0; i <storedDevices.length; i++){
							if(mac.equals(storedDevices[i])){
								cPanel.saveString(mac+"Names", "n/a");
								cPanel.saveString(mac+"Momentary", "n/a");
								cPanel.saveString(mac, "n/a");

							}else{
								if (devicesToSave == null){
									devicesToSave = (storedDevices[i] + ";");
								}else{
									devicesToSave = (devicesToSave + storedDevices[i] + ";");
								}
							}
						}
						if (devicesToSave != null){
							cPanel.saveString("savedDevices", devicesToSave);
						}else{
							cPanel.saveString("savedDevices", "n/a");
						}
						cPanel.saveString(mac, "n/a");
						setContentView( mainTable());
						if(currentapiVersion>=11){
				    		saveButtonAnimation.start();
				    	}


					}
				});
				removeDeviceAlert.setNegativeButton("No", new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
						if(currentapiVersion>=11){
				    		saveButtonAnimation.start();
				    	}

					}
				});
				AlertDialog areYouSureDialog = removeDeviceAlert.create();
				areYouSureDialog.show();
			}
				
		});
    	removeDeviceAlert.setNegativeButton("Edit", new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				System.out.println("Button Clicked here");
				
				Intent settingsPageIntent = new Intent(getApplicationContext(), SettingsPageActivity.class);
				String[] deviceSettings = cPanel.getStoredString(mac).split(";");
				if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
					settingsPageIntent.putExtra("BLUETOOTHADDRESS", deviceSettings[0]);
				}
				if(deviceSettings.length >= 10){
					if(deviceSettings[9].equalsIgnoreCase("true")){
						settingsPageIntent.putExtra("PWM", "true");
					}
				}
				if(deviceSettings.length >= 11){
					if(deviceSettings[10].equalsIgnoreCase("true")){
						settingsPageIntent.putExtra("ACTUATOR", true);
					}
				}
				if(deviceSettings.length >= 13){
					if(deviceSettings[12].equalsIgnoreCase("true")){
						settingsPageIntent.putExtra("WEBI", "true");
						System.out.println("Sending true exta to settings page");
					}else{
						System.out.println("Device List says device is not WEBI "+ deviceSettings[11]);
					}
				}else{
					System.out.println("deviceSettings.length = "+deviceSettings.length);
				}
				
				settingsPageIntent.putExtra("MAC", mac);
				startActivity(settingsPageIntent);
				dialog.cancel();
				finish();
				
			}
		});
    	AlertDialog removeDeviceDialog = removeDeviceAlert.create();
    	removeDeviceDialog.show();
    }
	
	private OnItemClickListener deviceSelected = new OnItemClickListener(){

		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) 
		{
			String info = ((TextView) arg1).getText().toString();
			if (info.contains("Getting Started")){
				Uri uriUrl = Uri.parse("http://www.controlanything.com/Relay/Device/IORelay_TCP-DOC");
				Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
				startActivity(launchBrowser);
				finish();
				return;
			}
			String[] infoSplit = info.split("\n");
			System.out.println(infoSplit[1] + " selected from list");
			

			currentlyEditingDevice = infoSplit[1];
			
			String deviceSettings = cPanel.getStoredString(currentlyEditingDevice);
			String[] deviceSettingsSplit = deviceSettings.split(";");
			if(deviceSettingsSplit.length < 7){
				//User updated app which didnt have as many settings so fix it.
				deviceSettings = deviceSettings+";"+"false"+";"+"true"+";"+"false"+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 8){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 9){
				deviceSettings = deviceSettings+";"+"false"+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 11){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 12){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 13){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 14){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 15){
				deviceSettings = deviceSettings+";"+"1234";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 16){
				deviceSettings = deviceSettings+";"+"false";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			if(deviceSettingsSplit.length < 17){
				deviceSettings = deviceSettings+";"+"1234";
				cPanel.saveString(currentlyEditingDevice, deviceSettings);
				deviceSettingsSplit = deviceSettings.split(";");
			}
			
			//WiNet Device
			if(deviceSettingsSplit.length > 8){
				if(deviceSettingsSplit[8].equalsIgnoreCase("true")){
					cPanel.winet = true;
					cPanel.winetMac = deviceSettingsSplit[0];
				}else{
					cPanel.winet = false;
				}
			}else{
				cPanel.winet = false;
			}
			
			if(!cPanel.getStoredString(infoSplit[1]).split(";")[15].equalsIgnoreCase("true")){
				switchToControlActivity(infoSplit[1]);
			}else{
				accessPassCodePrompt(infoSplit[1]);
			}
			
			

		}

	};
	
	private void switchToControlActivity(String mac){
		
		String deviceSettings = cPanel.getStoredString(mac);
		String[] deviceSettingsSplit = deviceSettings.split(";");
		
		//Is a bluetooth device
		if(deviceSettingsSplit[2].equalsIgnoreCase("Bluetooth")){
			boolean displayRelays = deviceSettingsSplit[7].equalsIgnoreCase("true");
			boolean displayInputs = deviceSettingsSplit[6].equalsIgnoreCase("true");
			boolean displayPWM = deviceSettingsSplit[9].equalsIgnoreCase("true");
			boolean displayActuator = deviceSettingsSplit[10].equalsIgnoreCase("true");
			boolean displayMacros = deviceSettingsSplit[11].equalsIgnoreCase("true");
			connectBluetooth(deviceSettingsSplit[0], displayRelays, displayInputs, displayPWM, displayActuator, displayMacros);
			return;
		}
		
		//Check for network connection
		ConnectivityManager cManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		if(cManager.getActiveNetworkInfo() == null || cManager.getActiveNetworkInfo().isConnected() == false){
			dismissProgressDialog();
			Toast toast = Toast.makeText(getBaseContext(), "No Network Connection", Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		//Force connection with stored ip and port("remote" stored for ssid).
		if(deviceSettingsSplit[5].equalsIgnoreCase("remote"))
		{
			if (cPanel.connect(deviceSettingsSplit[1], Integer.parseInt(deviceSettingsSplit[2])) == true)
			{
				//Switch to Relay Activity
				if(deviceSettingsSplit[7].equalsIgnoreCase("true")){
					switchToRelayActivity(mac);
					return;
				}
				//Switch to A/D inputs Activity
				if(deviceSettingsSplit[6].equalsIgnoreCase("true")){
					cPanel.disconnect();
					switchToInputActivity(cPanel.ipA, cPanel.port);
					return;
				}
				//Switch to Macros Activity
				if(deviceSettingsSplit[11].equalsIgnoreCase("true")){
					System.out.println("Switching to Macros Activity");
					cPanel.disconnect();
					switchToMacrosActivity(mac);
					return;
					
				}else{
					System.out.println("macros not enabled");
				}
				//Switch to PWM Activity
				if(deviceSettingsSplit[9].equalsIgnoreCase("true")){
					switchToPWMActivity(mac);
					return;
				}
				//Switch to Actuator Activity
				if(deviceSettingsSplit[10].equalsIgnoreCase("true")){
					switchToActuatorActivity(mac);
				}
				
			}
			else{
				makeToast("Could Not Connect", Toast.LENGTH_LONG);
				return;
			}
		}

		//WiFi is Enabled
		if(isWiFiConnected(getBaseContext()) == true)
		{
			WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
			WifiInfo wifiInfo = wifiManager.getConnectionInfo();
			//Device is on Same LAN.
			if(wifiInfo.getSSID().equals(deviceSettingsSplit[5])){

				if (cPanel.connect(deviceSettingsSplit[1], Integer.parseInt(deviceSettingsSplit[2])) == true)
				{
					//Switch to Relay Activity
					if(deviceSettingsSplit[7].equalsIgnoreCase("true")){
						switchToRelayActivity(mac);
						return;
					}
					//Switch to A/D inputs Activity
					if(deviceSettingsSplit[6].equalsIgnoreCase("true")){
						cPanel.disconnect();
						switchToInputActivity(cPanel.ipA, cPanel.port);
						return;
					}
					//Switch to Macros Activity
					if(deviceSettingsSplit[11].equalsIgnoreCase("true")){
						System.out.println("Switching to Macros Activity");
						cPanel.disconnect();
						switchToMacrosActivity(mac);
						return;
						
					}else{
						System.out.println("macros not enabled");
					}
					//Switch to PWM Activity
					if(deviceSettingsSplit[9].equalsIgnoreCase("true")){
						switchToPWMActivity(mac);
					}
					//Switch to Actuator Activity
					if(deviceSettingsSplit[10].equalsIgnoreCase("true")){
						switchToActuatorActivity(mac);
					}

				}
				else{
					System.out.println("Searching for Device Local");
					System.out.println("Calling findDevice, passing mac: "+mac);
					findDevice(mac, "local");


				}
			}else{
				//WiFi connected but Not on Same LAN as device
				System.out.println("Not on same network");
				String wiNetMac = cPanel.getStoredString(mac+"-"+"wiNet-wiNetMac");
				if(!wiNetMac.equalsIgnoreCase("n/a")){
					findDevice(wiNetMac,"remote");
				}else{
					findDevice(mac, "remote");
				}

			}
		}
		//No WiFi so get connection through signal switch
		else
		{

			String wiNetMac = cPanel.getStoredString(mac+"-wiNetMac");
			if(!wiNetMac.equalsIgnoreCase("n/a")){
				System.out.println("Device is WiNet");
				findDevice(wiNetMac,"remote");
			}else{
				System.out.println("Device is not WiNet");
				findDevice(mac, "remote");

			}

		}
	}
	
	private void connectBluetooth(String address, boolean displayRelays, boolean displayInputs, boolean pwm, boolean linearActuator, boolean displayMacros){
		System.out.println("Landed in connectBluetooth");
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(!bluetoothAdapter.isEnabled())
        {
     	   Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
     	   startActivityForResult(enableIntent, 3);
     	   return;
        }else{
        	
        	//Check if display relays is enabled
        	if(displayRelays){
        		System.out.println("Switching to relay activity");
        		//connect then display relays page
        		if(cPanel.connect(address)){
        			Intent relayControlIntent = new Intent(getApplicationContext(), RelayControlActivity.class);
        			relayControlIntent.setAction("Start");
        			relayControlIntent.putExtra("MAC", address);
        			startActivity(relayControlIntent);
        			finish();
        			return;
        		}else{
        			cPanel.btSocket = null;
        			Toast toast = Toast.makeText(getBaseContext(), "Could Not Connect\nEnsure Bluetooth Device is in range(300 ft).", Toast.LENGTH_LONG);
        			toast.show();
        			return;
        		}
        	}
        	//display inputs page
        	if(displayInputs){
        		if(cPanel.connect(address)){
        			Intent adInputsActivity = new Intent(getApplicationContext(), ADInputActivity.class);
	        		adInputsActivity.setAction("Start");
	        		adInputsActivity.putExtra("BLUETOOTHADDRESS", address);
	        		startActivity(adInputsActivity);
	        		finish();
	        		return;
        		}else{
        			cPanel.btSocket = null;
        			Toast toast = Toast.makeText(getBaseContext(), "Could Not Connect\nEnsure Bluetooth Device is in range(300 ft).", Toast.LENGTH_LONG);
        			toast.show();
        			return;
        		}
        		
        	}
        	if(displayMacros){
        		if(cPanel.connect(address)){
        			Intent macrosActivityIntent = new Intent(getApplicationContext(), MacroActivity.class);
        			macrosActivityIntent.setAction("Start");
        			macrosActivityIntent.putExtra("MAC", address);
	        		startActivity(macrosActivityIntent);
	        		finish();
	        		return;
        		}else{
        			cPanel.btSocket = null;
        			Toast toast = Toast.makeText(getBaseContext(), "Could Not Connect\nEnsure Bluetooth Device is in range(300 ft).", Toast.LENGTH_LONG);
        			toast.show();
        			return;
        		}
        	}
        	if(pwm){
        		if(cPanel.connect(address)){
        			Intent pwmIntent = new Intent(getApplicationContext(), PWMControlActivity.class);
    				pwmIntent.putExtra("MAC", currentlyEditingDevice);
    				startActivity(pwmIntent);
    				finish();
    				return;
        		}else{
        			cPanel.btSocket = null;
        			Toast toast = Toast.makeText(getBaseContext(), "Could Not Connect\nEnsure Bluetooth Device is in range(300 ft).", Toast.LENGTH_LONG);
        			toast.show();
        		}
        	}
        	if(linearActuator){
        		if(cPanel.connect(address)){
        			Intent linearActuatorIntent = new Intent(getApplicationContext(), LinearActuatorActivity.class);
        			linearActuatorIntent.putExtra("MAC", currentlyEditingDevice);
    				startActivity(linearActuatorIntent);
    				finish();
    				return;
        		}else{
        			cPanel.btSocket = null;
        			Toast toast = Toast.makeText(getBaseContext(), "Could Not Connect\nEnsure Bluetooth Device is in range(300 ft).", Toast.LENGTH_LONG);
        			toast.show();
        		}
        	}
        }
	}
	
	public void switchToRelayActivity(String mac){
		Intent relayControlIntent = new Intent(getApplicationContext(), RelayControlActivity.class);
		relayControlIntent.setAction("Start");
		relayControlIntent.putExtra("MAC", mac);
		startActivity(relayControlIntent);
		finish();
	}
	
	public void switchToInputActivity(String ipAddress, int portNumber){
		Intent adInputsActivity = new Intent(getApplicationContext(), ADInputActivity.class);
		adInputsActivity.setAction("Start");
		adInputsActivity.putExtra("MAC", currentlyEditingDevice);
		adInputsActivity.putExtra("IP", ipAddress);
		adInputsActivity.putExtra("PORT", portNumber);
		startActivity(adInputsActivity);
		finish();
	}
	
	public void switchToMacrosActivity(String mac){
		Intent macroActivityIntent = new Intent(getApplicationContext(), MacroActivity.class);
		macroActivityIntent.setAction("Start");
		macroActivityIntent.putExtra("MAC", mac);
		startActivity(macroActivityIntent);
		finish();
	}
	
	public void switchToPWMActivity(String mac){
		Intent pwmIntent = new Intent(getApplicationContext(), PWMControlActivity.class);
		pwmIntent.putExtra("MAC", mac);
		startActivity(pwmIntent);
		finish();
		return;
	}
	
	public void switchToActuatorActivity(String mac){
		Intent linearActuatorActivity = new Intent(getApplicationContext(), LinearActuatorActivity.class);
		linearActuatorActivity.putExtra("MAC", mac);
		startActivity(linearActuatorActivity);
		finish();
	}
	
	public String getSSID(){
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		return(wifiInfo.getSSID());
	}
	
    public void findDevice(String mac, String location)
    {
    	Handler findDeviceHandler = new Handler()
		{
			public void handleMessage(Message message) 
            {
				dismissProgressDialog();
				Object path = message.obj;
				System.out.println(message.obj.toString());
				if (message.arg1 == RESULT_OK && path != null)
				{
					if (message.obj.toString() == "device not found local")
					{
						showProgressDialog("Connecting to SignalSwitch");
						//Find Device Through SignalSwitch
						findDeviceIntent.setAction("Start");
						findDeviceIntent.putExtra("LOCATION", "remote");
						findDeviceIntent.putExtra("MAC", currentlyEditingDevice);
						findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
						startService(findDeviceIntent);
						return;
						
					}
					if(message.obj.toString() == "device not available")
					{
						System.out.println("no device on signalswitch");
						dismissProgressDialog();
						Toast toast = Toast.makeText(getBaseContext(), "Device Not Available Local or Remote", Toast.LENGTH_LONG);
						toast.show();
						return;
					}
					if(message.obj.toString() == "browser error"){
						Toast toast = Toast.makeText(getBaseContext(), message.obj.toString(), Toast.LENGTH_LONG);
						toast.show();
						return;
					}
					dismissProgressDialog();
					//If we got here then we must have an address to connect

					String deviceSettings = cPanel.getStoredString(currentlyEditingDevice);
					String[] deviceSettingsSplit = deviceSettings.split(";");

					int connectionPort;

					String recievedIP = message.obj.toString();
					if(recievedIP.contains(";")){
						String[] connectionInfo = recievedIP.split(";");
						recievedIP = connectionInfo[0];
						connectionPort = Integer.parseInt(connectionInfo[1]);
					}else{
						connectionPort = Integer.parseInt(deviceSettingsSplit[2]);
					}
					System.out.println("Got this IP address back in findDeviceHandler " + recievedIP);
					showProgressDialog("IP address recieved\nAttempting Connection");

					if (cPanel.connect(recievedIP, connectionPort) == true)
					{
						dismissProgressDialog();
						
						//Switch to Relay Activity
						if(deviceSettingsSplit[7].equalsIgnoreCase("true")){
							switchToRelayActivity(currentlyEditingDevice);
							return;
						}
						//Switch to A/D inputs Activity
						if(deviceSettingsSplit[6].equalsIgnoreCase("true")){
							cPanel.disconnect();
							switchToInputActivity(cPanel.ipA, cPanel.port);
							return;
						}
						//Switch to Macros Activity
						if(deviceSettingsSplit[11].equalsIgnoreCase("true")){
							switchToMacrosActivity(currentlyEditingDevice);
							return;
						}
						
						
						//Switch to PWM Activity
						if(deviceSettingsSplit[9].equalsIgnoreCase("true")){
							switchToPWMActivity(currentlyEditingDevice);
							return;
						}
						//Switch to LinearActuator Activity
						if(deviceSettingsSplit[10].equalsIgnoreCase("true")){
							switchToActuatorActivity(currentlyEditingDevice);
							return;
						}
						
						Toast toast = Toast.makeText(getBaseContext(), "No Activity Enabled \n must select relays, inputs, or macros", Toast.LENGTH_LONG);
						toast.show();
						

					}else{
						dismissProgressDialog();
						Toast toast = Toast.makeText(getBaseContext(), "Could not Connect", Toast.LENGTH_LONG);
						toast.show();
					}
				}
            }
		};
		
		findDeviceIntent = new Intent(this, FindDevice.class);
		findDeviceMessenger = new Messenger(findDeviceHandler);
		findDeviceIntent.setAction("Start");
		findDeviceIntent.putExtra("LOCATION", location);
		findDeviceIntent.putExtra("MAC", mac);
		findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
		
		startService(findDeviceIntent);
		if(location.equals("remote")){
			showProgressDialog("Getting conneciton through Signal Switch");
		}else{
			showProgressDialog("Scanning Lan for Device");
		}


    }
    
    public void makeToast(String toastMessage, int length){
    	
    	Toast toast = Toast.makeText(getBaseContext(), toastMessage, length);
		toast.show();
		
    }
    
    private void showProgressDialog(String message){
    	progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(message);
        progressDialog.show();
    }
    
    private void dismissProgressDialog() {
        if(progressDialog != null)
            progressDialog.dismiss();
    }
    
    public static boolean isWiFiConnected(Context context){
    	ConnectivityManager connectivityManager = (ConnectivityManager)
    	        context.getSystemService(Context.CONNECTIVITY_SERVICE);
    	    NetworkInfo networkInfo = null;
    	    if (connectivityManager != null) {
    	        networkInfo =
    	            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    	    }
    	    return networkInfo == null ? false : networkInfo.isConnected();
    }
	
}
