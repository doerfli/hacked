package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonElement;

import java.io.IOException;

import li.doerf.hacked.R;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by moo on 26.08.17.
 */

public class HIBPPasswordAsyncTask extends AsyncTask<String,Void,Boolean> {
    private static final String TAG = "HIBPPasswordAsyncTask";
    private ProgressBar progressBar;
    private TextView passwordOk;
    private TextView passwordPwned;
    private Exception exception;
    private Context myContext;
    
    public HIBPPasswordAsyncTask(Context context, ProgressBar aProgressBar, TextView aPasswordOk, TextView aPasswordPwned) {
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
    protected Boolean doInBackground(String... strings) {
        String password = strings[0];

        Log.d(TAG, "checking password: ");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
        Call<Void> call = service.getPwnedPassword(password);
        boolean pwned = false;

        try {
            Response<Void> response = call.execute();
            int code = response.code();

            switch ( code ) {
                case 200:
                    pwned = true;
                    break;

                case 404:
                    pwned = false;
                    break;

                default:
                    throw new IOException("unexected code: " + code);
            }
        } catch (IOException e) {
            Log.e(TAG, "caught IOException while checking password", e);
            exception = e;
        }

        return pwned;
    }

    @Override
    protected void onPostExecute(Boolean pwned) {
        progressBar.setVisibility(View.GONE);

        if ( exception != null ) {
            Toast.makeText(myContext, myContext.getString(R.string.error_download_data), Toast.LENGTH_SHORT).show();
        } else if ( ! pwned ) {
            passwordOk.setVisibility(View.VISIBLE);
        } else {
            passwordPwned.setVisibility(View.VISIBLE);
        }

        super.onPostExecute(pwned);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }
}
