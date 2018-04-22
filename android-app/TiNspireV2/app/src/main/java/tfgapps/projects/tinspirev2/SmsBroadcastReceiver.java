package tfgapps.projects.tinspirev2;


/**
 * Created by Samuel on 13/01/2018.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;


public class SmsBroadcastReceiver extends BroadcastReceiver {
    private Bundle bundle;
    private SmsMessage currentSMS;
    private String message;

    public void exceptionManager(String tag, String function, Exception e) {
        try {
            messaging t = messaging.getInstance();
            t.exceptionManager("SmsBroadcastReceiver", tag, function, e);
        } catch (Exception er) {
            Log.wtf(tag,"WTFFFFFFFFF",e);
            Log.wtf(tag,"WTFFFFFFFFF",er);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
                bundle = intent.getExtras();
                if (bundle != null) {
                    Object[] pdu_Objects = (Object[]) bundle.get("pdus");
                    if (pdu_Objects != null) {

                        for (Object aObject : pdu_Objects) {

                            currentSMS = getIncomingMessage(aObject, bundle);
                            //Toast.makeText(context, "Sender num: " + senderNo + " :\n message: " + message, Toast.LENGTH_SHORT).show();
                            //TextView textView = (TextView) findViewById(R.id.textView);
                            //textView.setText("Sender num: " + senderNo + " :\n message: " + message);

                            //Control inst = Control.instance();
                            //inst.onSMS(message,senderNo);
                            //Toast.makeText(context, "starting Service ", Toast.LENGTH_SHORT).show();

                            messaging t = messaging.getInstance();
                            if(t.myService != null) {
                                t.myServiceBinder.newSms(currentSMS.getDisplayOriginatingAddress() ,currentSMS.getDisplayMessageBody());
                            }
                        }
                        this.abortBroadcast();
                        // End of loop
                    }
                }
            } // bundle null
        } catch (Exception e) {
            exceptionManager("SMS_NEW","onReceive",e);
        }
    }


    private SmsMessage getIncomingMessage(Object aObject, Bundle bundle) {
        SmsMessage currentSMS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String format = bundle.getString("format");
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject, format);
        } else {
            currentSMS = SmsMessage.createFromPdu((byte[]) aObject);
        }
        return currentSMS;
    }
}