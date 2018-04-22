package tfgapps.projects.tinspirev2;

/**
 * Created by Samuel on 08/04/2018.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Looper;
import android.os.Messenger;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.*;
import static java.lang.Math.*;

import java.util.concurrent.ExecutionException;

import static java.lang.Thread.sleep;

public class BleService extends Service {
    private NotificationManager mNM;
    private int NOTIFICATIONID = 1;
    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    volatile boolean stopWorker;
    private ConnectedThread mConnectedThread;
    BleService thisservice = this;
    Handler handler;
    public static final int CONTACT_QUERY_LOADER = 0;
    public static final String QUERY_KEY_CONTACT = "querycontact";
    public static final String QUERY_KEY_NUMBER = "querynumber";
    Boolean isCalcHere = false;
    String currentContactName = "";
    String currentContactNum = "";
    List<String> contactNames = new ArrayList<>();
    List<String> contactNums  = new ArrayList<>();
    String calcMode = "";
    String messengerCurrentThread = "MESSENGER";
    final int messengerUpdateDelay = 800; //milliseconds
    CheckBox chkMessenger ;
    CheckBox chkSms ;
    RequestQueue requestQueue;
    List<Pair<String, String>> currentContactList = new ArrayList<>();

    public BleService() {
    }
    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateUI_Log("The New Service is starting");
        chkMessenger = messaging.getInstance().checkMESSENGER;
        chkSms = messaging.getInstance().checkSMS;
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job

        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                address = (String) extras.get("address");
                showNotification("Connection running", "Connected to " + address);
                updateUI_Log("Address: " + address);
                ConnectBT t = new ConnectBT();
                t.execute(); //Call the class to connect
                final Handler messengerHandler = new Handler();
                messengerHandler.postDelayed(new Runnable(){
                    public void run(){
                        if(chkMessenger.isChecked()) {
                            if(!serverAvalible(getApplicationContext(),"http://127.0.0.1:5000")) {
                                exceptionManager("SRV_START","onStartCommand.messengerHandler",new Exception("server unreachable"));
                                runOnUiThread(new Runnable() { @Override public void run() { chkMessenger.setChecked(false);}});
                            }
                            if(chkMessenger.isChecked()) { checkUnread(); }
                        }
                        if(messaging.getInstance().isMyServiceRunning(BleService.class));{
                            messengerHandler.postDelayed(this, messengerUpdateDelay);
                        }
                    }
                }, messengerUpdateDelay);
                if(!serverAvalible(getApplicationContext(),"http://127.0.0.1:5000")) {
                    exceptionManager("SRV_START","onStartCommand",new Exception("server unreachable"));
                    runOnUiThread(new Runnable() { @Override public void run() { chkMessenger.setChecked(false);}});
                    updateUI_Log("messenger unreachable (127.0.0.1:5000)");
                } else  {
                    updateUI_Log("Connected to messenger API server (127.0.0.1:5000)");
                }
            } else {
                exceptionManager("SRV_START","onStartCommand",new Exception("extras = null"));
            }
            return START_NOT_STICKY;
        } catch(Exception e) { exceptionManager("SRV_START","onStartCommand",e); }
        return START_NOT_STICKY;
    }
    @Override
    public void onCreate() {
        //Toast.makeText(this, "The new Service was Created", Toast.LENGTH_SHORT).show();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        handler = new Handler();
    }
    public void sendToast(String msg) {
        Toast.makeText(thisservice, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() { destroy(); }
    boolean hasSaidDestroyed = false;
    private void destroy() {
        if(!hasSaidDestroyed) { updateUI_Log("Service Destroyed"); hasSaidDestroyed=true;}
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                stopWorker = true;
                btSocket.getInputStream().close(); //close connection
                btSocket.getOutputStream().close(); //close connection
                btSocket.close(); //close connection
            }
            catch (Exception e)
            { exceptionManager("SRV_QUIT","destroy",e);}
        }
        mNM.cancel(NOTIFICATIONID);
    }
    private void showNotification(String title, String text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, BleService.class), 0);
        Notification notification = new Notification.Builder(this)
                .setTicker(text)  // the status text
                .setSmallIcon(R.drawable.icon_notif)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon_notif))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();
        // Send the notification.
        mNM.notify(NOTIFICATIONID, notification);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  /* UI Thread*/ {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            //progress = ProgressDialog.show(BleService.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            } catch (Exception e) {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                updateUI_Log("Connection Failed. Is the Bluetooth client reachable? ");
                exceptionManager("BLE_CONN","ConnectBT.onPostExecute",new Exception("Can't reach client"));
            }
            else
            {
                updateUI_Log("Connected to device.");
                isBtConnected = true;
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
            }
            //progress.dismiss();
        }
    }
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) { thisservice.destroy(); }
            mmInStream = tmpIn;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Looper.prepare();
            // Keep looping to listen for received messages
            while (true) {
                try {
                    if (mmInStream.available() > 1) {
                        try { sleep(500);  } catch (InterruptedException e) { e.printStackTrace(); }
                        bytes = mmInStream.read(buffer);            //read bytes from input buffer
                        final String readMessage = new String(buffer, 0, bytes);
                        // Send the obtained bytes to the UI Activity via handler
                        //btSocket.getOutputStream().write(readMessage.getBytes());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                    String data = readMessage;
                                    data = data.replace("\r", "");
                                    data = data.replace("\n", "");
                                    bleIncomingData(data);
                            }
                        });

                    }
                    //readMessage ="";
                } catch(Exception e) { thisservice.destroy(); }
            }
        }
        void bleIncomingData(String msg) {
            msg = msg.replace("\r", "");
            msg = msg.replace("\n", "");
            newBleMessage(msg);
        }
    }

    private final IBinder mBinder = new MyBinder();
    private Messenger outMessenger;

    @Override
    public IBinder onBind(Intent arg0) {
        Bundle extras = arg0.getExtras();
        Log.d("service","onBind");
        // Get messager from the Activity
        if (extras != null) {
            Log.d("service","onBind with extra");
            outMessenger = (Messenger) extras.get("MESSENGER");
        }
        return mBinder;
    }
    public class MyBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }

    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = "?";

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                //String contactId = contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        if(!name.equals("?")) { return name; } else { return number; }
    }
    public List<Pair<String, String>> getContacts(String name) {

        List<Pair<String, String>> results = new ArrayList<>();

        String[] projection = new String[] { ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER };
        String selection = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE '%" + name + "%'";
        //selection += " AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1"; // 1=local, 0=google acount
        Cursor cur = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, selection, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        while (cur.moveToNext()) {
            long contactId = cur.getLong(0);
            String name2 = cur.getString(1);
            String phone = cur.getString(2);

            if(checkValidNumber(phone)) {
                phone=parseNumber(phone);
                results.add(Pair.create(name2, phone));
            }
        }
        cur.close();
        if(results.isEmpty()) { Log.d("Contacts", "found: Nothing"); return null; } else { return results; }
    }
    public String parseNumber(String number) {
        number = number.replace("-","");
        if(number.charAt(0) == '0') { number = "+33"+number.substring(1); }
        return number;
    }
    public Boolean checkValidNumber(String number) {
        //Should have 12 digit
        //  -Should start with +336 or +337
        //Should have 5 digit
        //  -Should start with 3
        number = parseNumber(number);
        switch (number.length()) {
            case 12:
                if(number.substring(0, 4).equals("+336")|| number.substring(0, 4).equals("+337")) { return true; } else { return false; }
            case 5:
                if(number.substring(0, 1).equals("3")) { return true; } else { return false; }
            default:
                return false;
        }
    }
    public boolean onUnbind() { /* throw new RuntimeException("Stub!"); */ return true;}

    static public boolean serverAvalible(Context context, String url) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("curl "+url);
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
        return false;
    }


    public void exceptionManager(String tag, String function, Exception e) {
        if(function.contains("StringRequest") && e.getMessage().contains("null")) { return; }   //404Error
        if(function.contains("onStartCommand") && e.getMessage().contains("unreachable")) { return; }   //404Error
        if(function.contains("onStartCommand") && e.getMessage().contains("connection reset")) { return; }   //404Error
        try {
            messaging t = messaging.getInstance();
            t.exceptionManager("BleService",tag,function,e);
            if(!function.equals("destroy")) {
                //destroy();
                t.onServiceDestroy();
            }
        } catch (Exception er) {
            Log.wtf(tag,"WTFFFFFFFFF",e);
            Log.wtf(tag,"WTFFFFFFFFF",er);
        }
    }

    public void sendOverBle(String msg) {
        try {
            msg = msg.replace("\r", "");
            msg = msg.replace("\n", "");
            msg = msg + "\r";
            if (btSocket != null) {
                Log.i("BLE", "Sending Message!!");
                for (char ch : msg.toCharArray()) {
                    try {
                        btSocket.getOutputStream().write(ch);
                        try {
                            sleep(10);
                        } catch (Exception e) {
                            exceptionManager("BLE_GET_sleep","sendOverBle",e);
                        }
                    } catch (Exception e) {
                        exceptionManager("BLE_GET_btSocket","sendOverBle",e);
                    }
                }
            }
        } catch (Exception e) {
            exceptionManager("BLE_GET","sendOverBle",e);
        }
    }
    public void sendSMS(String msg) {
        if(!chkSms.isChecked()) { return; }
        if (!currentContactNum.equals("")) {
            try {
                SmsManager.getDefault().sendTextMessage(currentContactNum, null, msg, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateUI_Log(final String log) {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    messaging t = messaging.getInstance();
                    t.txtLog.setText(t.txtLog.getText() + log + "\r\n");
                    try {
                        t.scroll.fullScroll(View.FOCUS_DOWN);
                    } catch (Exception e) {
                        exceptionManager("UI_UPDATESSCROLL","updateUI_Log", e);
                    }
                }
            });
        } catch (Exception e) {
            exceptionManager("UI_UPDATE","updateUI_Log",e);
        }
    }
    public void newSms(String sender,String message) {
        if(!chkSms.isChecked()) { return; }
        try {
            String senderName = getContactDisplayNameByNumber(sender);
            sendOverBle("{" + senderName + "}  " + message);
        } catch (Exception e) {
            exceptionManager("SMS_NEW","newSms",e);
        }
    }
    public void bleMessgeSMS(String Message) {
        try {
            Message = Message.replace("\r", "");
            Message = Message.replace("\n", "");
            if (Message.startsWith("$#$UPDATENUM:")) {
                if(!chkSms.isChecked()) { sendOverBle("%%|NUMS|1|SMS desactivated use -1"); return; }
                currentContactList = getContacts(Message.substring(13));
                if (currentContactList == null || currentContactList.isEmpty()) {
                    sendOverBle("%%|NUMS|1|None");
                } else {
                    String respondCurrentTxt = "%%|NUMS|";
                    int size = currentContactList.size();
                    String respondnewText = respondCurrentTxt + String.valueOf(size);

                    for (Pair<String, String> l : currentContactList) {
                        respondnewText = respondnewText + "|" + l.first;
                        updateUI_Log("Contact found: " + l.first + ", " + l.second);
                    }
                    respondnewText = respondnewText + "\r";

                    sendOverBle(respondnewText);
                }
                //sendToast(msg.substring(13, msg.length()));
            }
            if (Message.startsWith("$#$CHANGENUM:")) {
                if(!chkSms.isChecked()) { sendOverBle("%%|NUMS|1|SMS desactivated use -1"); return; }
                List<Pair<String, String>> tmp = new ArrayList<>();
                tmp = getContacts(Message.substring(13));
                String name = tmp.get(0).first;
                String numb = tmp.get(0).second;
                currentContactName = name;
                currentContactNum = numb;
                updateUI_Log("Changing to: " + name + "-" + numb);
            }
            if (Message.startsWith("$#$SMS:")) {
                if(!chkSms.isChecked()) { sendOverBle("%%|NUMS|1|SMS desactivated use -1"); return; }
                sendSMS(Message.substring(7));
                updateUI_Log("Send sms: " + Message.substring(7));
                //sendToast(msg.substring(7, msg.length()));
            }
            if (Message.startsWith("$#$HELP:ME")) {
                sendOverBle("Avalible commands:");
                sendOverBle("  - $#$UPDATENUM: \t Search contact");
                sendOverBle("  - $#$CHANGENUM: \t Change contact");
                sendOverBle("  - #$#CALC:ENTERING:SMS: \t Enter");
                sendOverBle("  - #$#CALC:LEAVING:SMS: \t Leave");
                sendOverBle("  - $#$HELP:ME \t This message");
                updateUI_Log("Avalible commands:");
                updateUI_Log("  - $#$UPDATENUM: \t Search contact");
                updateUI_Log("  - $#$CHANGENUM: \t Change contact");
                updateUI_Log("  - #$#CALC:ENTERING:SMS: \t Enter");
                updateUI_Log("  - #$#CALC:LEAVING:SMS: \t Leave");
                updateUI_Log("  - $#$HELP:ME \t This message");
            }
        } catch (Exception e) {
            exceptionManager("BLE_NEW", "newBleMessage", e);
        }
    }

    void checkUnread() {
        if(!chkMessenger.isChecked()) { searchContactsRetrieve("%%|NUMS|1|Messenger desactivated use -1"); return;}
        try {
            if (requestQueue == null) { requestQueue = Volley.newRequestQueue(this); }
            StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://127.0.0.1:5000/fb/messenger/1.0/fetchUnread/",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String json) {
                            try {
                                JSONArray list = new JSONArray(json);
                                for (int i = 0; i < list.length(); i++) {
                                    // create a JSONObject for fetching single user data
                                    JSONObject msgDetail = list.getJSONObject(i);
                                    // fetch email and name and store it in arraylist
                                    String authorId = msgDetail.getString("authorId");
                                    String authorName = msgDetail.getString("authorName");
                                    String trdId = msgDetail.getString("trdId");
                                    String trdName = msgDetail.getString("trdName");
                                    String formatedMsg = msgDetail.getString("formatedMsg");
                                    String type = msgDetail.getString("type");
                                    newUnreadMessage(trdId, trdName, authorId, authorName, type, formatedMsg);
                                }
                            } catch (JSONException error) {
                                exceptionManager("Json", "checkUnread.StringRequest.onResponse", error);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            exceptionManager("Request", "checkUnread.StringRequest.onErrorResponse", error);
                        }
                    });
            RetryPolicy policy = new DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            stringRequest.setRetryPolicy(policy);
            requestQueue.add(stringRequest);
        } catch (Exception e) {}
    }
    void newUnreadMessage(String threadId,String threadName,String authorId,String authorName,String type,String msg) {
        sendOverBle(msg);
    }
    void searchContactsSearch(String searchurl) {
        if(!chkMessenger.isChecked()) { searchContactsRetrieve("%%|NUMS|1|Messenger desactivated use -1"); return;}
        if (requestQueue == null) { requestQueue = Volley.newRequestQueue(this); }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, searchurl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String json) {
                        try {
                            JSONArray list = new JSONArray(json);
                            String respondTxt = "%%|NUMS|"+String.valueOf(min(list.length(),25));
                            for (int i = 0; i < min(list.length(),25); i++) {
                                // create a JSONObject for fetching single user data
                                JSONObject msgDetail = list.getJSONObject(i);
                                // fetch email and name and store it in arraylist
                                String name     = msgDetail.getString("name");
                                String type   = msgDetail.getString("type");
                                if(type.equals("GROUP")) { name = "["+name+"]"; }
                                if(type.equals("USER")) { name = "<"+name+">"; }
                                respondTxt=respondTxt+"|"+name;
                            }
                            searchContactsRetrieve(respondTxt);
                        } catch (JSONException error) {
                            exceptionManager("Json","searchContactsSearch.StringRequest.onResponse",error);;
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        exceptionManager("Json","searchContactsSearch.StringRequest.onResponse",error);
                    }
                });
        requestQueue.add(stringRequest);
        RetryPolicy policy = new DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
    }
    void searchContactsLasts(String url) {
        if(!chkMessenger.isChecked()) { searchContactsRetrieve("%%|NUMS|1|Messenger desactivated use -1"); return;}
        if (requestQueue == null) { requestQueue = Volley.newRequestQueue(this); }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String json) {
                        try {
                            JSONArray list = new JSONArray(json);
                            String respondTxt = "%%|NUMS|"+String.valueOf(min(list.length(),25));
                            for (int i = 0; i < min(list.length(),25); i++) {
                                // create a JSONObject for fetching single user data
                                JSONObject msgDetail = list.getJSONObject(i);
                                // fetch email and name and store it in arraylist
                                String name     = msgDetail.getString("name");
                                String type   = msgDetail.getString("type");
                                if(type.equals("GROUP")) { name = "["+name+"]"; }
                                if(type.equals("USER")) { name = "<"+name+">"; }
                                respondTxt=respondTxt+"|"+name;
                            }
                            searchContactsRetrieve(respondTxt);
                        } catch (JSONException e) {
                            exceptionManager("Json","searchContactsLasts.StringRequest.onResponse",e);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        exceptionManager("Json","searchContactsLasts.StringRequest.onResponse",error);
                    }
                });
        requestQueue.add(stringRequest);
        RetryPolicy policy = new DefaultRetryPolicy(8000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        stringRequest.setRetryPolicy(policy);
    }
    void searchContactsRetrieve(String str) {
        sendOverBle(str);
    }
    void sendMessage(final String msg,String name) {
        if(!chkMessenger.isChecked()) { searchContactsRetrieve("%%|NUMS|1|Messenger desactivated use -1"); return;}
        final BleService t = this;
        if (requestQueue == null) { requestQueue  = Volley.newRequestQueue(t); }
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://127.0.0.1:5000/fb/messenger/1.0/fetchThread/"+name,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String json) {
                        try {
                            JSONArray list = new JSONArray(json);
                            for (int i = 0; i < max(list.length(),1); i++) {
                                JSONObject msgDetail = list.getJSONObject(i);
                                String name     = msgDetail.getString("name");
                                String type   = msgDetail.getString("type");
                                String uid   = msgDetail.getString("uid");
                                if (requestQueue == null) { requestQueue  = Volley.newRequestQueue(t); }
                                StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://127.0.0.1:5000/fb/messenger/1.0/send/"+uid+"/"+type+"/"+msg,
                                        new Response.Listener<String>() {
                                            @Override
                                            public void onResponse(String json) {  }
                                        },
                                        new Response.ErrorListener() {
                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                                exceptionManager("Volley","sendMessage.StringRequest.onResponse",error);
                                            }
                                        });
                                requestQueue.add(stringRequest);

                            }
                        } catch (JSONException error) {
                            exceptionManager("Json","sendMessage.StringRequest.onResponse",error);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { Toast.makeText(getApplicationContext(), "Error " + error.getMessage(), Toast.LENGTH_SHORT).show(); }
                });
        requestQueue.add(stringRequest);
    }
    public void bleMessgeMES(String Message) {
        try {
            Message = Message.replace("\r", "");
            Message = Message.replace("\n", "");
            if (Message.startsWith("$#$SEARCHTHR:")) { searchContactsSearch("http://127.0.0.1:5000/fb/messenger/1.0/fetchThread/"+Message.substring(13)); }
            if (Message.startsWith("$#$GETLASTTHR:")) { searchContactsLasts("http://127.0.0.1:5000/fb/messenger/1.0/fetchLastConv/");  }
            if (Message.contains("GETHR:")) { //$#$CHANGETHR:<name>
                String m = Message.substring(13).replace("<","").replace(">","").replace("[","").replace("]","");
                messengerCurrentThread = m;
                updateUI_Log("Changing to: " + m);
            }
            if (Message.startsWith("$#$MES:")) {
                if(!chkMessenger.isChecked()) { searchContactsRetrieve("%%|NUMS|1|Messenger desactivated use -1"); return;}
                sendMessage(Message.substring(7),messengerCurrentThread);
                updateUI_Log("Send messenger: " + Message.substring(7));
            }
        } catch (Exception e) {
            exceptionManager("BLE_NEW", "newBleMessage", e);
        }
    }

    public void newBleMessage(String Message) {
        if (Message.startsWith("#$#CALC:ENTERING")) {
            if(Message.contains("ING:SMS")) { calcMode = "SMS"; sendOverBle("Welcome SMS");}
            if(Message.contains("ING:MES")) { calcMode = "MESSENGER"; sendOverBle("Welcome MESSENGER");}
            isCalcHere = true;  updateUI_Log("Calc here");
        }
        if (Message.startsWith("#$#CALC:LEAVING")) { isCalcHere = false; updateUI_Log("Calc out"); }

        bleMessgeSMS(Message);
        bleMessgeMES(Message);
        updateUI_Log(Message);
    }

}