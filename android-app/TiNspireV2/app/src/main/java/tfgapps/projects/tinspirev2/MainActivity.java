package tfgapps.projects.tinspirev2;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    //widgets
    ListView devicelist;
    //Bluetooth
    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    public static String EXTRA_ADDRESS = "device_address";
    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Calling widgets
        devicelist = (ListView) findViewById(R.id.listView);

        //if the device has bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if (myBluetooth == null) {
            //Show a mensag. that the device has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            finish();
        } else if (!myBluetooth.isEnabled()) {
            //Ask to the user turn the bluetooth on
            Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnBTon, 1);
        }
        checkPerm();
        while (!myBluetooth.isEnabled()) {      }
    }

    @Override
    protected void onStart() {
        super.onStart();
        pairedDevicesList();
        checkForMessengerApi();
        askForDownloadIrCodes();
    }
    public void checkPerm() {
        int permissionReadContact = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        int permissionSendMessage = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        int permissionGetMessage = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        int permissionRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionReadContact != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_CONTACTS);
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (permissionGetMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        }
        if (permissionRead != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (permissionWrite != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);            /*return false; */
        }
    }
    private void pairedDevicesList() {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        } else {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener); //Method called when the device from the list is clicked

    }


    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Get the device MAC address, the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Make an intent to start next activity.
            Intent i = new Intent(MainActivity.this, messaging.class);
            //Change the activity.
            i.putExtra(EXTRA_ADDRESS, address); //this will be received at ledControl (class) Activity
            startActivity(i);
        }
    };

    public void askForDownloadIrCodes() {
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yup", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { downloadIrCodes(); dialog.dismiss(); }
            });
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Nop", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { dialog.dismiss(); }
            });
        dialog.setMessage("Do you want to download latest IrCodes ?");
        dialog.setTitle("Infrared");
        dialog.show();
    }
    static public boolean serverAvalible(Context context, String url) {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("curl "+url);
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
        return false;
    }
    public void checkForMessengerApi() {
        if(!serverAvalible(getApplicationContext(),"http://127.0.0.1:5000")) {
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Next", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { askForDownload(); dialog.dismiss(); }
            });
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "I know", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) { dialog.dismiss(); }
            });
            dialog.setMessage("It appear that the server is down, maybe it's not installed!");
            dialog.setTitle("Messenger");
            dialog.show();
        }
    }
    public String checkArch() {
        String getArchCommand = "uname -m";
        ShellExecuter exe = new ShellExecuter();
        String outp = exe.Executer(getArchCommand);
        return outp;
    }
    public void sendToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    public boolean fileExists(String filename) {
        File f = new File(filename);
        if(f.exists()) {
            return true;
        }
        return false;
    }
    void askForDownload() {
        AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
        dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Yup", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { downloadServer(); dialog.dismiss();  }
        });
        dialog.setButton(AlertDialog.BUTTON_POSITIVE,"Nop", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { dialog.dismiss();  }
        });
        dialog.setButton(AlertDialog.BUTTON_NEUTRAL,"Already done", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String path = Environment.getExternalStorageDirectory().getPath() + "/";
                String arch = checkArch();
                if(fileExists(path + "server-messenger_" + arch)) { dialog.dismiss(); } else {
                    AlertDialog dialog2 = new AlertDialog.Builder(MainActivity.this).create();
                    dialog2.setButton(AlertDialog.BUTTON_NEGATIVE,"YES CLOSE THIS THING ", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) { dialog.dismiss();  }
                    });
                    dialog2.setButton(AlertDialog.BUTTON_POSITIVE,"Oups misclick", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) { downloadServer(); dialog.dismiss();  }
                    });
                    dialog2.setMessage("Sure it doesn't appear to be here: "+path + "server-messenger_" + arch);
                    dialog2.setTitle("Messenger");
                    dialog2.show();
                }
            }
        });
        dialog.setMessage("Wanna download messenger serv? (This will allow you to connect this app with messenger)");
        dialog.setTitle("Messenger");
        dialog.show();
    }
    ProgressDialog mProgressDialog;
    public void downloadServer() {
        final String path = getApplicationInfo().dataDir + "/files/";
        final String arch = checkArch();
        String executableurl = "https://github.com/TurtleForGaming/nspire-communication/blob/master/messenger-server/dist/server-messenger_" + arch + "?raw=true";
        sendToast(executableurl);
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Downloading messenger server");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        final DownloadTaskServer DownloadTaskServer = new DownloadTaskServer(MainActivity.this);
        DownloadTaskServer.execute(executableurl);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                DownloadTaskServer.cancel(true);
                sendToast("Canceled! You won't be able to use messenger. Come on it's just ~25Mo");
                ShellExecuter exe = new ShellExecuter();
            }
        });
    }
    public void downloadIrCodes() {
        String path = Environment.getExternalStorageDirectory().getPath() + "/Ti-Nspire/";
        rm(path+"*.json");
        String IrLinks = "https://raw.githubusercontent.com/TurtleForGaming/nspire-communication/master/android-app/IrCodes/links.json";
        final DownloadTaskIrcodes downloadTask = new DownloadTaskIrcodes(MainActivity.this);
        mkdir(path);
        downloadTask.execute(IrLinks,path+"links.json");
    }
    public void mkdir(String path){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("mkdir "+path);
            int     exitValue = ipProcess.waitFor();
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
    }
    public void rm(String path){
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("rm "+path);
            int     exitValue = ipProcess.waitFor();
        } catch (IOException | InterruptedException e) { e.printStackTrace(); }
    }
    private class DownloadTaskServer extends AsyncTask<String, Integer, String> {
        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTaskServer(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                int fileLength = connection.getContentLength();
                // download the file
                input = connection.getInputStream();
                String path = Environment.getExternalStorageDirectory().getPath() + "/";
                String arch = checkArch();

                output = new FileOutputStream(path + "server-messenger_" + arch);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (input != null) {
                        input.close();
                    }
                } catch (IOException ignored) {
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null){
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            }else {
                String path = Environment.getExternalStorageDirectory().getPath() + "/";
                String arch = checkArch();
                // 1. Instantiate an AlertDialog.Builder with its constructor
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).create();
                dialog.setButton(AlertDialog.BUTTON_NEGATIVE,"Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { dialog.dismiss();  }
                });
                dialog.setMessage("Executable here: " + path + "server-messenger_" + arch);
                dialog.setTitle("Download finished");
                dialog.show();
            }
        }
    }
    private class DownloadTaskIrcodes extends AsyncTask<String, Integer, String> {
        private Context context;
        public DownloadTaskIrcodes(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(String... f) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(f[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                int fileLength = connection.getContentLength();
                // download the file
                input = connection.getInputStream();

                output = new FileOutputStream(f[1]);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) { output.close(); }
                    if (input != null) { input.close();  }
                } catch (IOException ignored) { }
                if (connection != null) { connection.disconnect();  }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                sendToast("Download error: " + result);
            }else {
                String path = Environment.getExternalStorageDirectory().getPath() + "/Ti-Nspire/";
                mkdir(path);
                String jsonStr = "[]";
                try {
                    FileInputStream stream = new FileInputStream(new File(path + "links.json"));
                    try {
                        FileChannel fc = stream.getChannel();
                        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                        jsonStr = Charset.defaultCharset().decode(bb).toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            stream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) { e.printStackTrace();}

                sendToast("Remotes list downloaded");

                try {
                    JSONArray list = new JSONArray(jsonStr);
                    for (int i = 0; i < list.length(); i++) {
                        // create a JSONObject for fetching single user data
                        JSONObject msgDetail = list.getJSONObject(i);
                        // fetch email and name and store it in arraylist
                        String link = msgDetail.getString("link");
                        String name = msgDetail.getString("name");
                        final DownloadTaskCorrect downloadTask2 = new DownloadTaskCorrect(MainActivity.this);
                        downloadTask2.execute(link, path + name + ".json");
                        sendToast("Downloading: "+name + ".json");
                    }
                } catch (JSONException e) { e.printStackTrace(); }
            }
        }
    }
    private class DownloadTaskCorrect extends AsyncTask<String, Integer, String> {
        private Context context;
        public DownloadTaskCorrect(Context context) {
            this.context = context;
        }
        @Override
        protected String doInBackground(String... f) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(f[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                int fileLength = connection.getContentLength();
                // download the file
                input = connection.getInputStream();

                output = new FileOutputStream(f[1]);

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null) { output.close(); }
                    if (input != null) { input.close();  }
                } catch (IOException ignored) { }
                if (connection != null) { connection.disconnect();  }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null){
                sendToast("Download error: " + result);
            }else {
                sendToast("Downloaded");
            }
        }
    }
}