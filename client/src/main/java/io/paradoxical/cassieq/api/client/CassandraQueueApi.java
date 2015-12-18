package io.paradoxical.cassieq.api.client;

import com.squareup.okhttp.ResponseBody;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.UpdateMessageRequest;
import io.paradoxical.cassieq.model.UpdateMessageResponse;
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

public interface CassandraQueueApi {

    static CassandraQueueApi createClient(URI baseUri) {
        return createClient(baseUri.toString());
    }

    static CassandraQueueApi createClient(String baseUri) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUri)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        return retrofit.create(CassandraQueueApi.class);
    }

    @POST("api/v1/queues")
    Call<ResponseBody> createQueue(@Body QueueCreateOptions queueName);

    @GET("api/v1/queues/{queueName}/messages/next")
    Call<GetMessageResponse> getMessage(@Path("queueName") QueueName queueName);

    @GET("api/v1/queues/{queueName}/messages/next")
    Call<GetMessageResponse> getMessage(
            @Path("queueName") QueueName queueName,
            @Query("invisibilityTimeSeconds") Long invisibilityTimeSeconds);

    @POST("api/v1/queues/{queueName}/messages")
    Call<ResponseBody> addMessage(@Path("queueName") QueueName queueName, @Body Object message);

    @PUT("api/v1/queues/{queueName}/messages")
    Call<UpdateMessageResponse> updateMessage(
            @Path("queueName") QueueName queueName,
            @Query("popReceipt") String popReciept,
            @Body UpdateMessageRequest message);

    @POST("api/v1/queues/{queueName}/messages")
    Call<ResponseBody> addMessage(
            @Path("queueName") QueueName queueName,
            @Body Object message,
            @Query("initialInvisibilitySeconds") Long initialInvisibilitySeconds);

    @DELETE("api/v1/queues/{queueName}/messages")
    Call<ResponseBody> ackMessage(
            @Path("queueName") QueueName queueName,
            @Query("popReceipt") String popReceipt);

    @DELETE("/api/v1/queues/{queueName}")
    Call<ResponseBody> deleteQueue(@Path("queueName") QueueName queueName);

    @DELETE("/api/v1/queues")
    Call<ResponseBody> purgeInactiveQueues();

}
