package li.doerf.hacked.remote.pwnedpasswords;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by moo on 05/09/16.
 */
public interface PwnedPasswords {

    @GET("/range/{sha5c}")
    Call<String> getRange(@Path("sha5c") String firstFiveCharsOfSha1);
}
