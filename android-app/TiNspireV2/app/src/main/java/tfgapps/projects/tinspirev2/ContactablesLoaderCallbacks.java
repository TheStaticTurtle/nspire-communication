package tfgapps.projects.tinspirev2;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;

public class ContactablesLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

    Context mContext;
    String isQuerringNumber;

    public static final String QUERY_KEY_CONTACT = "querycontact";
    public static final String QUERY_KEY_NUMBER = "querynumber";

    public static final String TAG = "CLC";

    public ContactablesLoaderCallbacks(Context context) {
        mContext = context;
    }

    public void exceptionManager(String tag, String function, Exception e) {
        try {
            messaging t = messaging.getInstance();
            t.exceptionManager("BleService",tag,function,e);
        } catch (Exception er) {
            Log.wtf(tag,"WTFFFFFFFFF",e);
            Log.wtf(tag,"WTFFFFFFFFF",er);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderIndex, Bundle args) {
        try {
            // Where the Contactables table excels is matching text queries,
            // not just data dumps from Contacts db.  One search term is used to query
            // display name, email address and phone number.  In this case, the query was extracted
            // from an incoming intent in the handleIntent() method, via the
            // intent.getStringExtra() method.

            // BEGIN_INCLUDE(uri_with_query)
            String query = args.getString(QUERY_KEY_CONTACT);
            isQuerringNumber = args.getString(QUERY_KEY_NUMBER);

            Uri uri = Uri.withAppendedPath(ContactsContract.CommonDataKinds.Contactables.CONTENT_FILTER_URI, query);
            // Easy way to limit the query to contacts with phone numbers.
            String selection = ContactsContract.CommonDataKinds.Contactables.HAS_PHONE_NUMBER + " = " + 1;
            // Sort results such that rows for the same contact stay together.
            String sortBy = ContactsContract.CommonDataKinds.Contactables.LOOKUP_KEY;

            return new CursorLoader(
                    mContext,  // Context
                    uri,       // URI representing the table/resource to be queried
                    null,      // projection - the list of columns to return.  Null means "all"
                    selection, // selection - Which rows to return (condition rows must match)
                    null,      // selection args - can be provided separately and subbed into selection.
                    sortBy);   // string specifying sort order
            // END_INCLUDE(cursor_loader)
        } catch (Exception e) {
            exceptionManager("CONTACT_ONCREATE","onCreateLoader",e);
            return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
        try {
            // Pulling the relevant value from the cursor requires knowing the column index to pull
            // it from.
            // BEGIN_INCLUDE(get_columns)

            int phoneColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.DISPLAY_NAME);
            int lookupColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.LOOKUP_KEY);
            int typeColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Contactables.MIMETYPE);
            // END_INCLUDE(get_columns)

            cursor.moveToFirst();
            // Lookup key is the easiest way to verify a row of data is for the same
            // contact as the previous row.
            String lookupKeyname = "";
            String lookupKeyphone = "";
            messaging inst = messaging.instance();

            String outputName = "None";
            String outputNum = "0000000000";
            Boolean doneNum = false;

            while (cursor.moveToNext()) {
                // BEGIN_INCLUDE(lookup_key)

                String currentLookupKeyname = cursor.getString(lookupColumnIndex);
                String currentLookupKeyphone = cursor.getString(lookupColumnIndex);

                String mimeType = cursor.getString(typeColumnIndex);
                if (!lookupKeyphone.equals(currentLookupKeyphone) && mimeType.equals(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                    String num = cursor.getString(phoneColumnIndex);
                    num = praseNumber(num);
                    if (isMobilePhone(num)) {
                        outputNum = num;
                        lookupKeyphone = currentLookupKeyphone;
                        doneNum = true;
                    }
                }

                if (isMobilePhone(outputNum) && doneNum) {
                    if (!lookupKeyname.equals(currentLookupKeyname)) {
                        String displayName = cursor.getString(nameColumnIndex);
                        outputName = displayName;
                        lookupKeyname = currentLookupKeyname;
                        if (isQuerringNumber.equals("yes")) {
                            //inst.searchAContact_Get(outputName, outputNum,2);
                            break;
                        } else {
                            //inst.searchAContact_Get(outputName, outputNum,1);
                        }

                    }
                }
            }
            if (!isQuerringNumber.equals("yes")) {
                //inst.searchAContact_Get("", "", 0);
            }
        } catch (Exception e) {
            exceptionManager("CONTACT_QUERY","onLoadFinished",e);
        }
    }

    public String praseNumber(String number) {
        number = number.replace("-","");
        if(number.charAt(0) == '0') { number = "+33"+number.substring(1); }
        return number;
    }

    public Boolean isMobilePhone(String number) {
        try {
            String tmpNum = number.substring(0, 4);
            if(tmpNum.equals("+336") || tmpNum.equals("+337")) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            exceptionManager("CONTACT_TEST","isMobilePhone",e);
            return false;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
    }
}
