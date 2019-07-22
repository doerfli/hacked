package li.doerf.hacked.remote.haveibeenpwned;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HIBPProxy {

    @GET("/search")
    Call<Void> search(@Query("account") String account, @Query("device_token") String deviceToken);

}
