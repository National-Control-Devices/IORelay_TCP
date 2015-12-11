package com.controlanything.NCDTCPRelay;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class RelayControlActivity extends Activity{
	
	ControlPanel cPanel;
	Class<? extends Intent> callingClass;
	
	Intent i;
	String deviceMacAddress;
	String[] relayNames;
	int[] momentaryIntArray; 
	int numberOfRelays;
	String deviceName;
	String defaultIP;
	int port;
	
	Typeface font;
	AnimationDrawable saveButtonAnimation;
	
	//Info on Device
	int currentapiVersion = android.os.Build.VERSION.SDK_INT;
	
	ImageButton[] relayButtons;
	TextView[] relayLabels;
	RelativeLayout titleTable;
	ImageView bottomButton;
	public int[] relayStatusArray;
	public TextView tvSocketConnection;
	TextView bText;
	int textColor = Color.WHITE;
	int subTextSize = 20;
	
	AlertDialog lostConnectionDialog;
	ProgressDialog progressDialog;
	
	Intent findDeviceIntent;
	Messenger findDeviceMessenger;
	
	ScrollView sView;
	
	GestureDetector gDetector;
	boolean displayInputs;
	boolean displayMacros;
	boolean fusion = false;
	int[] currentBankStatus = {0,0,0,0,0,0,0,0};
	
	//Haptic feedback
	private Vibrator myVib;
	
	//Bluetooth connection objects
	BluetoothAdapter btAdapter;
	BluetoothDevice btDevice;
	android.bluetooth.BluetoothSocket btSocket;
	String btDeviceAddress;
	boolean bluetooth;
	boolean winet;
	boolean switchToADActivity = false;
	boolean switchToMacrosActivity = false;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		cPanel = ((ControlPanel)getApplicationContext());
		gDetector = new GestureDetector(getApplicationContext(), new MyGestureDetector());
		myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
		font = Typeface.createFromAsset(getAssets(), "fonts/neuropolxfree.ttf");
		
		System.out.println("RelayControlActivity ID = "+this.toString());
		
		i = getIntent();
		callingClass = i.getClass();
		System.out.println("Calling Class = "+callingClass.toString());
		deviceMacAddress = i.getStringExtra("MAC");
	}
	
	public void getDeviceInfo(){
		//Get device settings for its name and number of relays.
		String[] deviceSettings = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceSettings[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
			btDeviceAddress = deviceMacAddress;
		}else{
			bluetooth = false;
		}
		numberOfRelays = Integer.parseInt(deviceSettings[3]);
		deviceName = deviceSettings[4];
		if(!bluetooth){
			defaultIP = deviceSettings[1];
			port = Integer.parseInt(deviceSettings[2]);
		}
		
		
		relayButtons = new ImageButton[numberOfRelays];
		relayLabels = new TextView[numberOfRelays];
		relayStatusArray = new int [numberOfRelays];
		
		//Get Relay Names
		relayNames = cPanel.getStoredString(deviceMacAddress+"Names").split(";");
		
		//Get button momentary or not.
		String[] momentaryString = cPanel.getStoredString(deviceMacAddress+"Momentary").split(";");
		momentaryIntArray = new int[numberOfRelays];
		for(int i = 0; i<momentaryString.length; i++){
			if (momentaryString[i].equals("1")){
//				System.out.println("momentaryString[i] = 1|0");
				momentaryIntArray[i] = 1;
			}else
			{
				momentaryIntArray[i] = 0;
			}
		}
		
		if(deviceSettings[6].equalsIgnoreCase("true")){
			displayInputs = true;
		}else{
			displayInputs = false;
		}
		if(deviceSettings.length > 8){
			if(deviceSettings[8].equalsIgnoreCase("true")){
				winet = true;
				cPanel.winet = true;
			}else{
				cPanel.winet = false;
			}
		}else{
			cPanel.winet = false;
		}
		if(deviceSettings[11].equalsIgnoreCase("true")){
			displayMacros = true;
		}
		
		
	}
	
	public RelativeLayout mainViewTable(){
		RelativeLayout mTable = new RelativeLayout(this);
		mTable.setBackgroundResource(R.drawable.background);
		mTable.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		mTable.addView(title());
				
		RelativeLayout.LayoutParams bottomButtonParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		bottomButtonParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mTable.addView(deviceListButton(), bottomButtonParams);	
		
		if(displayInputs || displayMacros){
			RelativeLayout.LayoutParams bottomTextParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			bottomTextParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
			if(displayInputs){
				mTable.addView(bottomText("Swipe left to display Inputs Page"), bottomTextParams);
			}else{
				mTable.addView(bottomText("Swipe left to display Macros Page"), bottomTextParams);
			}
		}
	
		RelativeLayout.LayoutParams scrollViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		scrollViewParams.addRule(RelativeLayout.BELOW, titleTable.getId());
		if(displayInputs || displayMacros){
			scrollViewParams.addRule(RelativeLayout.ABOVE, bText.getId());
		}else{
			scrollViewParams.addRule(RelativeLayout.ABOVE, bottomButton.getId());
		}
		
		mTable.addView(scrollView(), scrollViewParams);
		

		
		return mTable;
	}
	
	public RelativeLayout title(){
		titleTable = new RelativeLayout(this);
		titleTable.setBackgroundResource(R.drawable.top_bar);
		titleTable.setId(1);
		
//		table.setLayoutParams(new LayoutParams(displayWidth,(int) convertPixelsToDp(248, this)));
		
		final TextView tView = new TextView(this);
//		tView.setPadding(15, 70, 0, 0);
		tView.setText(deviceName);
		tView.setTypeface(font);
		tView.setTextSize(30);
		tView.setTextColor(Color.BLACK);

		RelativeLayout.LayoutParams titleLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		
		titleLayoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
		
		titleTable.addView(tView,titleLayoutParams);
		
		return titleTable;
	}
		
	public TextView bottomText(String message){
		bText = new TextView(this);
		bText.setId(3);
		bText.setText(message);
		bText.setTextColor(textColor);
		bText.setTextSize(this.subTextSize);
		return bText;
	}
	
	public ScrollView scrollView(){
		sView = new ScrollView(this);
		//Allows for height of bottom button
//		sView.setPadding(0, 0, 0, 171);

		sView.addView(controlsTable());
		
		if(displayInputs || displayMacros){
			sView.setOnTouchListener(new View.OnTouchListener() {
				
				public boolean onTouch(View v, MotionEvent event) {					
					
					
					if(gDetector.onTouchEvent(event)){
						if(displayInputs){
						System.out.println("Switching to ADInput Activity");
						switchToADActivity = true;
						Intent adIntent = new Intent(getApplicationContext(), ADInputActivity.class);
						adIntent.putExtra("MAC", deviceMacAddress);
						if(!bluetooth){
							adIntent.putExtra("IP", cPanel.sAddress.getAddress().getHostAddress());
							adIntent.putExtra("PORT", cPanel.sAddress.getPort());
						}else{
							adIntent.putExtra("BLUETOOTHADDRESS", deviceMacAddress);
						}
						showProgressDialog("Loading");
						startActivity(adIntent);
						dismissProgressDialog();
						return true;
						}
						if(displayMacros){
							switchToMacrosActivity = true;
							System.out.println("Switching to Macros Activity");
							
							Intent macroActivityIntent = new Intent(getApplicationContext(), MacroActivity.class);
							macroActivityIntent.setAction("Start");
							macroActivityIntent.putExtra("MAC", deviceMacAddress);
							showProgressDialog("Loading");
							startActivity(macroActivityIntent);
							dismissProgressDialog();
							return true;
							
						}
					}
					return false;
				}
			});
		}
		
		
		
		return sView;
	}
	
	public ImageView deviceListButton(){
		bottomButton = new ImageView(this);
		bottomButton.setId(2);
		
		if(currentapiVersion>=11){
			bottomButton.setImageResource(R.drawable.animationxmldevicelist);
			bottomButton.setBackgroundResource(0);
//			bottomButton.setMinimumHeight(120);
			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);

            saveButtonAnimation = (AnimationDrawable)bottomButton.getDrawable();
            saveButtonAnimation.setEnterFadeDuration(1000);
            saveButtonAnimation.setExitFadeDuration(1000);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listView);
					if(cPanel.connected == true){
						cPanel.disconnect();
					}
					finish();
					
				}
				
			});
			
		}else{
			bottomButton.setImageResource(R.drawable.bottom_bar_list);
			bottomButton.setBackgroundResource(0);
//			bottomButton.setMinimumHeight(120);
//			bottomButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			bottomButton.setBackgroundResource(R.drawable.bottom_bar);
			
			bottomButton.setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					Intent listView = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(listView);
					if(cPanel.connected == true){
						cPanel.disconnect();
					}else{
						System.out.println("cPanel.connected == false");
					}
					finish();
					
				}
				
			});
		}
		
		
		
		return bottomButton;
	}
	
	public TableLayout controlsTable(){
		TableLayout cTable = new TableLayout(this);
		
		if(cPanel.connected == false){
			if(bluetooth){
				cPanel.connect(deviceMacAddress);
			}else{
				if(cPanel.remote == true){
					cPanel.connect(cPanel.sAddress.getAddress().getHostAddress(), cPanel.port);
				}else{
					cPanel.connect(defaultIP, port);
				}
			}
			
			fusion = cPanel.checkFusion();
		}else{
			fusion = cPanel.checkFusion();
		}
		
		for(int i = 0; i<numberOfRelays; i++){
			LinearLayout relayControlRow = new LinearLayout(this);
			relayControlRow.setOrientation(LinearLayout.HORIZONTAL);
			
			relayButtons[i] = new ImageButton(this);
			relayLabels[i] = new TextView(this);
			relayControlRow.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
			
			
			relayLabels[i].setText(relayNames[i]);
			relayLabels[i].setTextSize(30);
			relayLabels[i].setTextColor(Color.WHITE);
			relayLabels[i].setGravity(Gravity.CENTER_VERTICAL);
			relayLabels[i].setPadding(10, 35, 0, 0);
			relayLabels[i].setTypeface(font);
			relayLabels[i].setMaxLines(2);
			relayLabels[i].setLineSpacing(50, 1);
			relayLabels[i].setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
			
			
			
			if (i <= 7)
    		{
				relayButtons[i] = newButton(i, 1);
    		}
    		else
    		{
    			if (i <= 15)
    			{
    				relayButtons[i] = newButton(i, 2);
    			}
    			else
    			{
    				if (i <= 23)
    				{
    					relayButtons[i] = newButton(i, 3);
    				}
    				else
    				{
    					relayButtons[i] = newButton(i, 4);
    				}
    			}
    		}
			relayButtons[i].setId(i);
			relayControlRow.addView(relayButtons[i]);
			relayControlRow.addView(relayLabels[i]);
			cTable.addView(relayControlRow);
			
		}
		
		if (numberOfRelays <= 8)
		{
			updateButtonText(1);
		}
		else
		{
			if (numberOfRelays == 16)
			{
				updateButtonText(1);
				updateButtonText(2);
			}
			else
			{
				if (numberOfRelays == 24)
				{
					updateButtonText(1);
					updateButtonText(2);
					updateButtonText(3);
				}
				else
				{
					updateButtonText(1);
					updateButtonText(2);
					updateButtonText(3);
					updateButtonText(4);
				}
			}
		}
		
		return cTable;
		
	}
	
	public ImageButton newButton(final int relayNumber, final int bankNumber){
		final ImageButton relayButton = new ImageButton(this);
  		relayButton.setAdjustViewBounds(true);
  		relayButton.setImageResource(R.drawable.button_dead);
  		relayButton.setBackgroundResource(0);

  		//Fusion
  		if(fusion){
  			System.out.println("Fusion Button");
  			
  			if (momentaryIntArray[relayNumber] == 0)
			{
				relayButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {

						if (relayStatusArray[relayNumber] == 0)
						{
							int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						else {
							int[] returnedStatus = (cPanel.TurnOffRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						
					}
				});


				
			}
			else
			{
				relayButton.setOnTouchListener(new View.OnTouchListener()
				{
					public boolean onTouch(View v, MotionEvent event) {
												
						if (event.getAction() == MotionEvent.ACTION_DOWN){
							int[] returnedStatus = (cPanel.TurnOnRelayFusion((relayNumber + ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
						}
						else if (event.getAction() == MotionEvent.ACTION_UP)
						{
							int[] returnedStatus = (cPanel.TurnOffRelayFusion((relayNumber - ((bankNumber-1)*8)), bankNumber));
							if(returnedStatus[0] != 260){
								myVib.vibrate(50);
								updateButtonTextFusion(bankNumber, returnedStatus);
							}else{
								changeTitleToRed();
							}
	  					
	  					}
						return false;
					}
	  			});
			}
  		}
  		//Not Fusion
  		else{
  			if (momentaryIntArray[relayNumber] == 0)
			{
				relayButton.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {

						if (relayStatusArray[relayNumber] == 0) 
						{
							if (cPanel.TurnOnRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == false)
							{
								
								relayButton.setEnabled(false);
								changeTitleToRed();								
								
							}else{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
							}
						}
						else {
							if (cPanel.TurnOffRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true){
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
								
							}else{
								relayButton.setEnabled(false);
								changeTitleToRed();								
							}
						}
						
					}
				});


				
			}
			else
			{
				relayButton.setOnTouchListener(new View.OnTouchListener()
				{
					public boolean onTouch(View v, MotionEvent event) {
												
						if (event.getAction() == MotionEvent.ACTION_DOWN){
							if (cPanel.TurnOnRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true)
							{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
							}
							else{
								relayButton.setEnabled(false);
								changeTitleToRed();								
							}
						}
						else if (event.getAction() == MotionEvent.ACTION_UP)
						{
							if (cPanel.TurnOffRelay((relayNumber - ((bankNumber-1)*8)), bankNumber) == true)
	  						{
								myVib.vibrate(50);
								changeTitleToGreen();
								updateButtonText(bankNumber);
	  						}else{
	  							relayButton.setEnabled(false);
	  							changeTitleToRed();
	  						}
	  					
	  					}
						return false;
					}
	  			});
			}
  		}
  		
  		
  		
  		return relayButton;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(cPanel.connected == true){
			if(!switchToADActivity || switchToMacrosActivity){
				cPanel.disconnect();
			}
			
		}else{
			System.out.println("cPanel.connected == false");
		}
		cPanel.connected = false;
		finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		tvSocketConnection = new TextView(this);
		getDeviceInfo();
		setContentView(mainViewTable());
		System.out.println("Bottom Button Height"+bottomButton.getHeight());
		
		if(currentapiVersion>=11){
			saveButtonAnimation.start();
		}
		
		String[] deviceInfo = cPanel.getStoredString(deviceMacAddress).split(";");
		if(deviceInfo[2].equalsIgnoreCase("Bluetooth")){
			bluetooth = true;
		}else{
			bluetooth = false;
		}
		
		if(cPanel.connected == false){
			if(bluetooth){
				if(cPanel.connect(deviceMacAddress) == false){
					System.out.println("Could not connect");
				}
			}else{
				if(cPanel.connect(defaultIP, port) == false){
					System.out.println("Could not connect");
				}
			}
			
//			fusion = cPanel.checkFusion();
		}else{
//			fusion = cPanel.checkFusion();
		}
		System.out.println("Bottom Button Height"+bottomButton.getHeight());
	}
	
	private void updateButtonText(int bankNumber) {
		int[] returnStatus = cPanel.getBankStatus(bankNumber);
		System.out.println("Return status = "+Arrays.toString(returnStatus));
		if(returnStatus != null){
			
			if (numberOfRelays < 8){
				
				for(int i = 0; i < numberOfRelays; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = returnStatus[i];
				}
				
				for (int i = 0; i < numberOfRelays; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
			}
			else {
				for(int i = 0; i < 8; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = returnStatus[i];
				}
				for (int i = 0; i < 8; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);
					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
			}
		}else{
			changeTitleToRed();
		}
	}
	
	private void updateButtonTextFusion(int bankNumber, int[] status) {
		if(status != null){
			
			if(numberOfRelays < 8){
				
				for(int i = 0; i < numberOfRelays; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = status[i];
				}
				
				for (int i = 0; i < numberOfRelays; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
				
			}else{
				
				for(int i = 0; i < 8; i++){
					relayStatusArray[i+((bankNumber-1)*8)] = status[i];
				}
				
				for (int i = 0; i < 8; i++) {

					if (relayStatusArray[i+((bankNumber-1)*8)] != 0){
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.blue_button_no_glow);

					}
					else {
						relayButtons[i + ((bankNumber-1)*8)].setImageResource(R.drawable.button_dead);
					}
				}
				
			}

				System.out.println("Current relay's status "+Arrays.toString(relayStatusArray));
		}else{
			changeTitleToRed();
		}
	}
	
	public void changeTitleToRed(){
    	showAlertDialog("Connection Lost");
    	
    }
    
    public void changeTitleToGreen(){
    	tvSocketConnection.setBackgroundColor(Color.GREEN);
    	tvSocketConnection.setText("NCD TCP Relay: Connected");
    	
    }
    
    public void changeTitleToYellow(){
    	tvSocketConnection.setBackgroundColor(Color.YELLOW);
    	tvSocketConnection.setText("NCD TCP Relay: Connecting....");
    }
	
	public void showAlertDialog(String title){
		System.out.println("showAlertDialog called");
		final AlertDialog.Builder removeDeviceAlert = new AlertDialog.Builder(this);
		
		if(bluetooth){
			
			removeDeviceAlert.setTitle(title);
	    	removeDeviceAlert.setMessage("Retry Connection");
	    	removeDeviceAlert.setCancelable(false);
	    	removeDeviceAlert.setPositiveButton("Retry", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					showProgressDialog("Connecting");
					System.out.println("Passing "+btDeviceAddress+" as bt address");
					if(cPanel.connect(btDeviceAddress)){
						dismissProgressDialog();
						Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
						toast.show();
					}else{
						dismissProgressDialog();
						showAlertDialog("Could Not Connect");
					}
					
				}
	    		
	    	});
	    	removeDeviceAlert.setNeutralButton("Exit", new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					if(cPanel.connected){
						cPanel.disconnect();
					}
					Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
					startActivity(deviceListActivity);
					finish();
					
				}
			});
			
		}else{

			if(title.equals("Connection Lost")){
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}


				removeDeviceAlert.setTitle(title);
				//	    	removeDeviceAlert.setMessage("Retry Connection");
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						showProgressDialog("Searching Lan for device");
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							System.out.println("cPanel.connect = true");
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();

						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");

						}
					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});
			}
			if(title.equals("Could Not Connect Local")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});
			}
			if(title.equals("Could Not Connect")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});

			}
			if(title.equals("Could Not Connect Remote")){
				removeDeviceAlert.setTitle(title);
				if(isWiFiConnected(getBaseContext()) == true){
					removeDeviceAlert.setMessage("Retry Connection");
				}else{
					removeDeviceAlert.setMessage("Retry Connection \nWiFi Not Available");
				}
				removeDeviceAlert.setCancelable(false);
				removeDeviceAlert.setPositiveButton("Local", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which){
						cPanel.disconnect();

						if(cPanel.connect(defaultIP, port)==true){
							dismissProgressDialog();
							dialog.dismiss();
							sView.removeAllViews();
							sView.addView(controlsTable());
							Toast toast = Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG);
							toast.show();
						}else{
							startFindDeviceService(deviceMacAddress, "local");
						}
					}
				});
				removeDeviceAlert.setNeutralButton("Remote", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						showProgressDialog("Getting Connection through Signal Switch");
						cPanel.disconnect();

						String wiNetMac = cPanel.getStoredString(deviceMacAddress+"-"+"wiNet-wiNetMac");
						if(!wiNetMac.equalsIgnoreCase("n/a")){
							startFindDeviceService(wiNetMac, "remote");

						}else{
							startFindDeviceService(deviceMacAddress, "remote");
						}

					}

				});
				removeDeviceAlert.setNegativeButton("Exit", new DialogInterface.OnClickListener(){

					public void onClick(DialogInterface dialog, int which) {
						Intent deviceListActivity = new Intent(getApplicationContext(), DeviceListActivity.class);
						startActivity(deviceListActivity);
						finish();
					}

				});

			}
		}
		lostConnectionDialog = removeDeviceAlert.create();
    	lostConnectionDialog.show();
	}
	
	public void startFindDeviceService(String deviceMac, String location ){
		findDeviceIntent = new Intent(getApplicationContext(), FindDevice.class);
		findDeviceMessenger = new Messenger(findDeviceHandler());
		findDeviceIntent.setAction("Start");
		findDeviceIntent.putExtra("LOCATION", location);
		findDeviceIntent.putExtra("MAC", deviceMac);
		findDeviceIntent.putExtra("MESSENGER", findDeviceMessenger);
		startService(findDeviceIntent);
	}
	
	private Handler findDeviceHandler(){
		Handler fdHandler = new Handler(){
			public void handleMessage(Message message){
				dismissProgressDialog();
				System.out.println(message.obj.toString());
				System.out.println("RelayControlActivity finddevice handler called");
				if (message.obj.toString() == "device not found local")
				{
					showAlertDialog("Could Not Connect Local");
					return;
				}
				
				if(message.obj.toString() == "device not available"){
					showAlertDialog("Could Not Connect Remote");
					return;
				}
				
				String recievedIP = message.obj.toString();
				System.out.println("Got this IP back");
				Toast toast = Toast.makeText(getBaseContext(), "Got This IP back: "+recievedIP, Toast.LENGTH_LONG);
				toast.show();
				if(cPanel.connect(recievedIP, port)){
					lostConnectionDialog.dismiss();
					dismissProgressDialog();
					sView.removeAllViews();
    				sView.addView(controlsTable());
				}else{
					System.out.println("Could not Connect to "+recievedIP);
					dismissProgressDialog();
					Toast toast1 = Toast.makeText(getBaseContext(), "Could not find device", Toast.LENGTH_LONG);
					toast1.show();
					
					lostConnectionDialog.dismiss();
					showAlertDialog("Could Not Connect");
					
				}
			}
		};
		
		return fdHandler;
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
    
    public class MyGestureDetector extends SimpleOnGestureListener
    {
    	@Override
    	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
    		if(e1 != null && e2 != null){
    			if(e1.getX()-e2.getX() > 200){
        			System.out.println("onFling returning true");
        			return true;
        		}
    		}
    		
    		System.out.println("onFling returning false");
			return false;
    		
    	}
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
