package io.paradoxical.cassieq.api.client;

import com.godaddy.logging.Logger;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.UpdateMessageRequest;
import io.paradoxical.cassieq.model.UpdateMessageResponse;
import io.paradoxical.cassieq.model.accounts.AccountName;
import retrofit.Call;
import retrofit.JacksonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.godaddy.logging.LoggerFactory.getLogger;

public interface CassieqApi {

    static CassieqApi createClient(URI baseUri, CassieqCredentials cassieqCredentials) {
        return createClient(baseUri.toString(), cassieqCredentials);
    }

    static CassieqApi createClient(String baseUri, CassieqCredentials cassieqCredentials) {

        final Logger logger = getLogger(CassieqApi.class);

        OkHttpClient client = new OkHttpClient();
        client.interceptors().add(chain -> {
            final Request request = chain.request();
            try {
                return chain.proceed(cassieqCredentials.authorize(request));
            }
            catch (InvalidKeyException | NoSuchAlgorithmException e) {
                logger.error(e, "Error authorizing credentials!");

                throw new RuntimeException("Error authorizing credentials!", e);
            }
        });

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(client)
                .build();

        return retrofit.create(CassieqApi.class);
    }

    @POST("api/v1/accounts/{accountName}/queues")
    Call<ResponseBody> createQueue(
            @Path("accountName") AccountName accountName,
            @Body QueueCreateOptions queueDefinition);

    @GET("api/v1/accounts/{accountName}/queues/{queueName}/messages/next")
    Call<GetMessageResponse> getMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName);

    @GET("api/v1/accounts/{accountName}/queues/{queueName}/messages/next")
    Call<GetMessageResponse> getMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName,
            @Query("invisibilityTimeSeconds") Long invisibilityTimeSeconds);

    @POST("api/v1/accounts/{accountName}/queues/{queueName}/messages")
    Call<ResponseBody> addMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName,
            @Body Object message);

    @PUT("api/v1/accounts/{accountName}/queues/{queueName}/messages")
    Call<UpdateMessageResponse> updateMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName,
            @Query("popReceipt") String popReciept,
            @Body UpdateMessageRequest message);

    @POST("api/v1/accounts/{accountName}/queues/{queueName}/messages")
    Call<ResponseBody> addMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName,
            @Body Object message,
            @Query("initialInvisibilitySeconds") Long initialInvisibilitySeconds);

    @DELETE("api/v1/accounts/{accountName}/queues/{queueName}/messages")
    Call<ResponseBody> ackMessage(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName,
            @Query("popReceipt") String popReceipt);

    @DELETE("/api/v1/accounts/{accountName}/queues/{queueName}")
    Call<ResponseBody> deleteQueue(
            @Path("accountName") AccountName accountName,
            @Path("queueName") QueueName queueName);
}
