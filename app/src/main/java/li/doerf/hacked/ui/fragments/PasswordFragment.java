package li.doerf.hacked.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import li.doerf.hacked.R;
import li.doerf.hacked.remote.pwnedpasswords.PwnedPasswordAsyncTask;
import li.doerf.hacked.ui.HibpInfo;

/**
 * Use the {@link PasswordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PasswordFragment extends Fragment {
    private static final String TAG = "PasswordFragment";
    private EditText passwordEditText;
    private ProgressBar progressBar;
    private TextView passwordOk;
    private TextView passwordPwned;
    private Tracker myTracker;

    public PasswordFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PasswordFragment.
     */
    public static PasswordFragment newInstance(Tracker aTracker) {
        PasswordFragment fragment = new PasswordFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        fragment.myTracker = aTracker;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_password, container, false);

        passwordEditText = view.findViewById(R.id.password);
        
        Button button = view.findViewById(R.id.check_pwned);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPassword();
            }
        });
        
        progressBar = view.findViewById(R.id.progressbar);
        passwordOk = view.findViewById(R.id.result_ok);
        passwordPwned = view.findViewById(R.id.result_pwned);

        HibpInfo.prepare( getContext(), (TextView) view.findViewById(R.id.hibp_info), null);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        myTracker.setScreenName("Fragment~Password");
        myTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    private void checkPassword() {
        String password = passwordEditText.getText().toString();
        PwnedPasswordAsyncTask passwordCheck = new PwnedPasswordAsyncTask(getContext(), progressBar, passwordOk, passwordPwned);
        passwordCheck.execute(password);
        myTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("CheckPasswordPwned")
                .build());
    }
}
