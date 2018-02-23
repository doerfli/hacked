package li.doerf.hacked.remote.pwnedpasswords;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.util.HashMap;

import li.doerf.hacked.R;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Created by moo on 26.08.17.
 */

public class PwnedPasswordAsyncTask extends AsyncTask<String,Void,String> {
    private static final String TAG = "PwnedPasswordAsyncTask";
    private ProgressBar progressBar;
    private TextView passwordOk;
    private TextView passwordPwned;
    private Exception exception;
    private Context myContext;

    public PwnedPasswordAsyncTask(Context context, ProgressBar aProgressBar, TextView aPasswordOk, TextView aPasswordPwned) {
        progressBar = aProgressBar;
        passwordOk = aPasswordOk;
        passwordPwned = aPasswordPwned;
        myContext = context;
    }

    @Override
    protected void onPreExecute() {
        passwordOk.setVisibility(View.GONE);
        passwordPwned.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        super.onPreExecute();
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
        boolean pwned = false;

        try {
            Response<String> response = call.execute();
            int code = response.code();
            Log.d(TAG, "response code: " + code);
            Log.d(TAG, response.body());

            HashMap<String, String> hashes = new HashMap<>();
            for ( String l : response.body().split("\r\n")) {
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
        progressBar.setVisibility(View.GONE);

        if ( exception != null ) {
            Toast.makeText(myContext, myContext.getString(R.string.error_download_data), Toast.LENGTH_SHORT).show();
        } else if ( pwned == null ) {
            passwordOk.setVisibility(View.VISIBLE);
        } else {
            passwordPwned.setVisibility(View.VISIBLE);
            passwordPwned.setText(myContext.getString(R.string.password_pwned, pwned));
        }

        super.onPostExecute(pwned);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
