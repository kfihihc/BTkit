package com.seeedstudio.btkit;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class BTkitMain extends Activity {
    // Debugging
    private static final String TAG = "BTkitMain";
    private static final boolean D = true;

    // Message types sent from the BTkitService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BTkitService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    private List<double[]> xValues = null;
    private List<double[]> yValues = null;
    private double[] tempD;

    // Layout Views
    private TextView mTitle;
    private TextView mValue;
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    //private Button mScanButton;
    //private Button mDiscoverableButton;
    private ToggleButton mToggleButton;
    private Button mMoreButton;

    // Name of the connected device, 链接的设备的名称 
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread, 会话的适配器
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages, 将要发出的字符串
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services,  聊天服务的对象
    private BTkitService mKitService = null;
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        mValue = (TextView)this.findViewById(R.id.value);
        mValue.setText("Value: " + "00");
        
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    // after initialize the UI and bluetooth adapter, 
    // then turn the bluetooth on and setup up the session.
	@Override
	protected void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");
        
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mKitService == null) setupSession();
        }
	}
	
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mKitService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mKitService.getState() == BTkitService.STATE_NONE) {
              // Start the Bluetooth chat services
              mKitService.start();
            }
        }
    }
    
	private void setupSession() {
        Log.d(TAG, "setupSession()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });
        
/*        // Set up the Scan and Discoverable button.
        mScanButton = (Button) findViewById(R.id.bluetooth_scan);
        mScanButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		//call DeviceList activity to change other bluetooth device
        		Intent serverIntent = new Intent(BTkitMain.this, BTkitDeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        });
        
        mDiscoverableButton = (Button) findViewById(R.id.bluetooth_discoverable);
        mDiscoverableButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		//ensure local bluetooth can be discover.
        		ensureDiscoverable();
        	}
        });*/
        
        // Set up Toggle Button for open or close.
        // send 1 (open) or 0 (close) to target.
        mToggleButton = (ToggleButton) this.findViewById(R.id.toggle_button);
        mToggleButton.setOnClickListener( new OnClickListener() {
        	public void onClick(View v) {
        		//switch on or off
        		byte[] message = new byte[1];
        		
        		if(mToggleButton.isChecked()){
        			message[0] = 1;
        			sendByte(message);
//        			String message = new String("1");
//        			sendMessage(message);
                    mToggleButton.setText(R.string.open);
        			Toast.makeText(BTkitMain.this, message[0] + " ON", Toast.LENGTH_SHORT).show();
        		}else{
        			message[0] = 0;
        			sendByte(message);
/*        			String message = new String("0");
        			sendMessage(message);*/
                    mToggleButton.setText(R.string.close);
                    Toast.makeText(BTkitMain.this, message[0] + " OFF", Toast.LENGTH_SHORT).show();
        		}
        	}
        });
        
        mMoreButton = (Button)this.findViewById(R.id.more_action);
        mMoreButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		showDialog(BTkitMain.this);
        	}
        });
        
        // Initialize the BTkitService to perform bluetooth connections
        mKitService = new BTkitService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
	}
	
	// set up the bluetooth can be discover, it called by DiscoverableButton.
/*    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        //if bluetooth is not DISCOVERABLE, turn to DISCOVERABLE, for 300s.
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            //extra intent for bluetoothAdapter, bluetooth appears in 300s.
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }*/

    private void showDialog(Context context) {
    	if(D) Log.d(TAG, "show dialog");
    	//set up the dialog layout by the more.xml.
    	LayoutInflater inflater = LayoutInflater.from(this);  
        final View dialogLayout = inflater.inflate(R.layout.more, (ViewGroup)findViewById(R.id.more_root));       
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        
        //set up more.xml layout button.
        Button connectButton = (Button) dialogLayout.findViewById(R.id.connect_button);
        connectButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		Intent serverIntent = new Intent(BTkitMain.this, BTkitDeviceList.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        	}
        });
        
        Button testButton = (Button) dialogLayout.findViewById(R.id.test_button);
        testButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		Toast.makeText(BTkitMain.this, "Read Charting", Toast.LENGTH_SHORT).show();
        		BTkitChart btChart;

        		if(xValues.equals(null)) {
        			btChart = new BTkitChart(BTkitMain.this, mHandler, 
        					"Bettary", yValues);
        		}else{
        			btChart = new BTkitChart(BTkitMain.this, mHandler, 
        					"Bettary", yValues, xValues);
        		}   
    			btChart.start();
        	}
        });
        
        //set up the dialog builder.
        builder.setCancelable(false)
        		.setTitle(R.string.more_action)
        		.setView(dialogLayout)
        		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});       
        builder.show();
    }
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mKitService.getState() != BTkitService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
        	byte[] send = message.getBytes();
                if(D) Log.d(TAG, message.length() + " " + send);
                mKitService.write(send, true);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    private void sendByte(byte[] message){
        if (mKitService.getState() != BTkitService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if(message.length > 0){
        	if(D) Log.d(TAG, message.length + " " + message);
        	mKitService.write(message, false);
        	
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }
    
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BTkitService
    // 线程的交流，更新 UI 线程。
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BTkitService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BTkitService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BTkitService.STATE_LISTEN:
                case BTkitService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;          
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 4, msg.arg1);
                
                for(int i = 0; i < readMessage.length(); i++) {
                	tempD[i] = Double.parseDouble(readMessage);
                }
                yValues.add(tempD);
                if(D) Log.d(TAG, "temp:" + tempD.toString() + "--" + "yValues:" +yValues.toString());
                
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                mValue.setText("Value: " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    // Called when an activity you launched exits(back to this activity).
    // 一般是 Intent 发出 requestCode 后，返回这个 requestCode 所要触发的事件。
    // e.g :
    // Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    // startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(BTkitDeviceList.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mKitService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupSession();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mKitService!= null) mKitService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
	@Override
	public void finish() {
		super.finish();
		if(D) Log.e(TAG, "---- Finish ----");
	}
    
}