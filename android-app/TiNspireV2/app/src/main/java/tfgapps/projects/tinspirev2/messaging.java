package tfgapps.projects.tinspirev2;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class messaging extends AppCompatActivity {

    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static messaging inst;
    public static final int CONTACT_QUERY_LOADER = 0;
    public static final String QUERY_KEY_CONTACT = "querycontact";
    public static final String QUERY_KEY_NUMBER = "querynumber";
    Boolean isCalcHere = false;
    String currentContactName = "";
    String currentContactNum = "";

    List<String> contactNames = new ArrayList<>();
    List<String> contactNums  = new ArrayList<>();

    private static final int PERMISSION_REQUEST_CODE = 1;

    Button btnStart;
    Button btnStop;
    Button btnSend;
    Button btnCommand;
    EditText etCommand;
    Button btnClear;
    TextView txtLog;
    ScrollView scroll;
    public CheckBox checkSMS;
    public CheckBox checkMESSENGER;
    public CheckBox checkIR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_messaging);
        inst = this;
        checkPerm();


        scroll = (ScrollView) findViewById(R.id.scrollView2);
        btnStart = (Button) findViewById(R.id.btnstart);
        btnStop = (Button) findViewById(R.id.btnstop);
        btnCommand = (Button) findViewById(R.id.btnSend);
        etCommand = (EditText) findViewById(R.id.eTCommands);
        btnClear= (Button) findViewById(R.id.btnClear);
        txtLog = (TextView) findViewById(R.id.textLog);
        checkSMS = (CheckBox) findViewById(R.id.checkBoxSMS);
        checkMESSENGER = (CheckBox) findViewById(R.id.checkBoxMESSENGER);
        checkIR = (CheckBox) findViewById(R.id.checkBoxIr);
        checkMESSENGER.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { }
        });
        btnCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { myServiceBinder.sendOverBle(etCommand.getText().toString()); }
        });
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txtLog.setText(" --- Log:");
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startThread();
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopThread();
            }
        });
        btnStop.setEnabled(false);
        btnCommand.setEnabled(false);
        sInstance = this;
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static messaging getInstance() {
        return sInstance ;
    }

    private static messaging sInstance = null;

    @Override
    protected void onStop() {
        super.onStop();
        //unbindService(mConnection);
    }

    public void onBackPressed() {
        super.onBackPressed();
        stopThread();
    }

    public void onServiceDestroy() {
        stopThread();
    }

    public static messaging instance() {
        return inst;
    }

    public void exceptionManager(final String activity,final String tag, final String function, final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.wtf(tag, "[" + activity + " / "+function+"] " + e.getMessage());
                if(!function.equals("messaging")) {
                    updateUI_Log("WTF:"+tag+" "+"[" + activity + " / "+function+"] " + e.getMessage());
                }
            }
        });

    }

    public void checkPerm() {
        int permissionReadContact = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS);
        int permissionGetMessage = ContextCompat.checkSelfPermission(this,Manifest.permission.RECEIVE_SMS);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionReadContact != PackageManager.PERMISSION_GRANTED) { listPermissionsNeeded.add(android.Manifest.permission.READ_CONTACTS);  }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) { listPermissionsNeeded.add(Manifest.permission.SEND_SMS);  }
        if (permissionGetMessage != PackageManager.PERMISSION_GRANTED) { listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);  }
        if (!listPermissionsNeeded.isEmpty()) { ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),PERMISSION_REQUEST_CODE);            /*return false; */ }
    }

    public void startThread() {
        Intent i = new Intent(this, BleService.class);
        i.putExtra("address",address);
        startService(i);
        btnStop.setEnabled(true);
        btnCommand.setEnabled(true);
        btnStart.setEnabled(false);
        if (myService == null) { doBindService(); }
        if (myService == null) { doBindService(); }
        if (myService == null) { doBindService(); }
    }

    public void stopThread() {
        if (myService != null) {  unbindService(myConnection); myService = null; }
        if (myService != null) {  unbindService(myConnection); myService = null; }
        if (myService != null) {  unbindService(myConnection); myService = null; }
        stopService(new Intent(this, BleService.class));
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        btnCommand.setEnabled(false);
    }

    public void updateUI_Log(String log) {
        try {
            txtLog.setText(txtLog.getText() + log + "\r\n");
            try {
                scroll.fullScroll(View.FOCUS_DOWN);
            } catch (Exception e) {
                exceptionManager("messaging","UI_UPDATESSCROLL","updateUI_Log", e);
            }
        } catch (Exception e) {
            exceptionManager("messaging","UI_UPDATE","updateUI_Log",e);
        }
    }


    public BleService myServiceBinder;
    public ServiceConnection myService = null;

    public void sendToBind(String txt) {
        if (myService != null) {
            myServiceBinder.sendOverBle(txt);
        }
    }

    public ServiceConnection myConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            myServiceBinder = ((BleService.MyBinder) binder).getService();
            updateUI_Log("BleService_Connection : connected");
            myService = myConnection;
        }

        public void onServiceDisconnected(ComponentName className) {
            updateUI_Log("BleService_Connection : disconnected");
            myService = null;
        }
    };

    public Handler myHandler = new Handler() {
        public void handleMessage(Message message) {
            Bundle data = message.getData();
        }
    };

    public void onDestroy() {
        super.onDestroy();
        stopThread();

    }

    public void doBindService() {
        try {
            Intent intent = null;
            intent = new Intent(this, BleService.class);
            // Create a new Messenger for the communication back
            // From the Service to the Activity
            Messenger messenger = new Messenger(myHandler);
            intent.putExtra("MESSENGER", messenger);

            bindService(intent, myConnection, Context.BIND_AUTO_CREATE);
        } catch(Exception e) { exceptionManager("messaging","BIND","doBindService",e); }
    }

}
