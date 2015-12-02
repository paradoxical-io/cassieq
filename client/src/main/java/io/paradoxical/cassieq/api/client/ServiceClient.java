package io.paradoxical.cassieq.api.client;

import com.squareup.okhttp.ResponseBody;
import retrofit.Call;
import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ServiceClient {
    static ServiceClient createClient(String baseUri) {
        Retrofit retrofit =
                new Retrofit.Builder()
                        .baseUrl(baseUri)
                        .addConverterFactory(JacksonConverterFactory.create())
                        .build();

        ServiceClient service = retrofit.create(ServiceClient.class);

        return service;
    }

    @GET("api/v1/ping/{echo}")
    Call<ResponseBody> ping(@Path("echo") String echo);
}
