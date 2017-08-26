package li.doerf.hacked.remote.haveibeenpwned;

import com.google.gson.JsonElement;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Created by moo on 05/09/16.
 */
public interface HaveIBeenPwned {

    @Headers({
            "Accept: application/vnd.haveibeenpwned.v2+json",
            "User-Agent: Hacked_Android_Client"
    })
    @GET("/api/v2/breachedaccount/{account}")
    Call<List<BreachedAccount>> listBreachedAccounts(@Path("account") String account);

    @Headers({
            "Accept: application/vnd.haveibeenpwned.v2+json",
            "User-Agent: Hacked_Android_Client"
    })
    @GET("/api/v2/breaches")
    Call<List<BreachedAccount>> getBreachedSites();

    @Headers({
            "Accept: application/vnd.haveibeenpwned.v2+json",
            "User-Agent: Hacked_Android_Client"
    })
    @FormUrlEncoded
    @POST("/api/v2/pwnedpassword")
    Call<Void> getPwnedPassword(@Field("password") String password);
}
