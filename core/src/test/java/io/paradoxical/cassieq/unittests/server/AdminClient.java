package io.paradoxical.cassieq.unittests.server;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.model.QueryAuthUrlResult;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.GetAuthQueryParamsRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.mappers.Mappers;
import retrofit.Call;
import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;
import retrofit.http.Path;

public interface AdminClient {
    static AdminClient createClient(String baseUri) {

        OkHttpClient client = new OkHttpClient();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(JacksonConverterFactory.create(Mappers.getJson()))
                .client(client)
                .build();

        return retrofit.create(AdminClient.class);
    }

    @POST("admin/api/v1/accounts")
    Call<AccountDefinition> createAccount(@Body AccountName accountName);

    @POST("admin/api/v1/permissions")
    Call<QueryAuthUrlResult> createPermissions(@Body GetAuthQueryParamsRequest request);

    @DELETE("admin/api/v1/accounts/{accountName}/keys/{keyName}")
    Call<ResponseBody> deleteAccountKey(@Path("accountName") AccountName accountName, @Path("keyName") KeyName keyName);
}
