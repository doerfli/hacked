package li.doerf.hacked.remote.pwnedpasswords;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by moo on 26.08.17.
 */

public class PwnedPasswordAsyncTask extends AsyncTask<String,Void,String> {
    public static final String BROADCAST_ACTION_PASSWORD_PWNED = "li.doerf.hacked.BROADCAST_ACTION_PASSWORD_PWNED";
    public static final String EXTRA_PASSWORD_PWNED = "ExtraPwned";
    public static final String EXTRA_EXCEPTION = "ExtraException";
    private static final String TAG = "PwnedPasswordAsyncTask";
    private final WeakReference<Context> myContext;
    private Exception exception;

    public PwnedPasswordAsyncTask(Context context) {
        myContext = new WeakReference<>(context);
    }

    @Override
    protected String doInBackground(String... strings) {
        String password = strings[0];
        String pwdHash = new String(Hex.encodeHex(DigestUtils.sha1(password))).toUpperCase();
        String pwdHashHead = pwdHash.substring(0, 5);

        Log.d(TAG, "checking password: ");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.pwnedpasswords.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        PwnedPasswords service = retrofit.create(PwnedPasswords.class);
        Call<String> call = service.getRange(pwdHashHead);

        try {
            Response<String> response = call.execute();
            int code = response.code();
            Log.d(TAG, "response code: " + code);
            String body = response.body();
            Log.d(TAG, body);

            if (body == null) {
                Log.w(TAG, "body was null");
                return null;
            }

            HashMap<String, String> hashes = new HashMap<>();
            for ( String l : body.split("\r\n")) {
                String[] t = l.split(":");
                hashes.put(pwdHashHead + t[0], t[1]);
                Log.d(TAG, pwdHashHead + t[0] + "   " + t[1]);
            }

            if (hashes.containsKey(pwdHash)) {
                return hashes.get(pwdHash);
            }

            return null;

        } catch (IOException e) {
            Log.e(TAG, "caught IOException while checking password", e);
            exception = e;
        }

        return null;
    }

    @Override
    protected void onPostExecute(String pwned) {
        Intent localIntent = new Intent(BROADCAST_ACTION_PASSWORD_PWNED);
        localIntent.putExtra(EXTRA_PASSWORD_PWNED, pwned);

        if(exception != null) {
            localIntent.putExtra(EXTRA_EXCEPTION, true);
        }

        LocalBroadcastManager.getInstance(myContext.get()).sendBroadcast(localIntent);
        Log.d(TAG, "broadcast finish sent");

        super.onPostExecute(pwned);
    }

}
