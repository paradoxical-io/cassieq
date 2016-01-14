package io.paradoxical.cassieq.admin.resources.api.v1;

import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.resources.api.BaseQueueResource;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.factories.QueueRepositoryFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.Getter;
import org.joda.time.DateTime;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Path("/api/v1/debug/accounts/{accountName}/queues")
@Api(value = "/api/v1/debug/accounts/{accountName}/queues", description = "Queue diagnostic api", tags = {"cassieq", "debug"})
@Produces(MediaType.APPLICATION_JSON)
public class QueueDebugResource extends BaseQueueResource {

    private final DataContextFactory dataContextFactory;

    @Inject
    public QueueDebugResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            QueueRepositoryFactory queueRepository,
            DataContextFactory dataContextFactory,
            @PathParam("accountName") AccountName accountName) {
        super(readerFactory, messageRepoFactory, monotonicRepoFactory, queueRepository.forAccount(accountName), accountName);
        this.dataContextFactory = dataContextFactory;
    }

    @GET
    @ApiOperation(value = "Get all queues")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getQueues() {

        final List<QueueDefinition> queues = getQueueRepository().getActiveQueues();

        return Response.ok(queues)
                       .status(Response.Status.OK)
                       .build();
    }

    @GET
    @Path("/{queueName}/buckets/{bucketPointer}/messages")
    @ApiOperation(value = "Get bucket raw messages")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getBucketMessages(
            @PathParam("queueName") QueueName queueName,
            @PathParam("bucketPointer") Long bucketPointer,
            @QueryParam("onlyUnacked") @DefaultValue("false") Boolean onlyUnacked) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }


        List<Message> messages = getMessageRepoFactory()
                .forQueue(queueDefinition.get())
                .getBucketContents(ReaderBucketPointer.valueOf(bucketPointer));

        if(onlyUnacked){
            messages = messages.stream().filter(Message::isAcked).collect(toList());
        }

        return Response.ok(messages)
                       .status(Response.Status.OK)
                       .build();
    }

    @GET
    @Path("/{queueName}/buckets/current/messages")
    @ApiOperation(value = "Get bucket raw messages")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getCurrentBucketMessages(
            @PathParam("queueName") QueueName queueName,
            @QueryParam("onlyUnacked") @DefaultValue("false") Boolean onlyUnacked) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final QueueDataContext dataContext = dataContextFactory.forQueue(queueDefinition.get());

        List<Message> messages =
                getMessageRepoFactory()
                        .forQueue(queueDefinition.get())
                        .getBucketContents(dataContext.getPointerRepository().getReaderCurrentBucket());

        if (onlyUnacked) {
            messages = messages.stream().filter(Message::isAcked).collect(toList());
        }

        return Response.ok(messages)
                       .status(Response.Status.OK)
                       .build();
    }

    @GET
    @Path("/{queueName}/buckets/{bucketPointer}/tombstone")
    @ApiOperation(value = "Get bucket sealed time")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response isBucketSealed(
            @PathParam("queueName") QueueName queueName,
            @PathParam("bucketPointer") Long bucketPointer) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final Optional<DateTime> tombstoneTime = getMessageRepoFactory().forQueue(queueDefinition.get()).tombstoneExists(ReaderBucketPointer.valueOf(bucketPointer));

        return Response.ok(tombstoneTime)
                       .status(Response.Status.OK)
                       .build();
    }

    @GET
    @Path("/{queueName}/messages/{messagePointer}")
    @ApiOperation(value = "Raw getAccountRepository message")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getMessage(
            @PathParam("queueName") QueueName queueName,
            @PathParam("messagePointer") Long messagePointer) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final Message message = getMessageRepoFactory().forQueue(queueDefinition.get())
                                                       .getMessage(MonotonicIndex.valueOf(messagePointer));

        return Response.ok(message)
                       .status(Response.Status.OK)
                       .build();


    }

    @GET
    @Path("/{queueName}/monotons/current")
    @ApiOperation(value = "Get current monoton value")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getCurrentMonotonValue(
            @PathParam("queueName") QueueName queueName) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final MonotonicRepository monotonicRepository = getMonotonicRepoFactory().forQueue(queueDefinition.get().getId());

        return Response.ok(monotonicRepository.getCurrent())
                       .status(Response.Status.OK)
                       .build();


    }

    @GET
    @Path("/{queueName}/pointers")
    @ApiOperation(value = "Get current pointer values")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getPointers(
            @PathParam("queueName") QueueName queueName) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final QueueDataContext dataContext = dataContextFactory.forQueue(queueDefinition.get());

        final PointerRepository pointerRepository = dataContext.getPointerRepository();

        final InvisibilityMessagePointer currentInvisPointer = pointerRepository.getCurrentInvisPointer();
        final ReaderBucketPointer readerCurrentBucket = pointerRepository.getReaderCurrentBucket();
        final RepairBucketPointer repairCurrentBucketPointer = pointerRepository.getRepairCurrentBucketPointer();

        return Response.ok(
                new Object() {
                    @Getter
                    private final InvisibilityMessagePointer currentInvisibilityPointer = currentInvisPointer;

                    @Getter
                    private final ReaderBucketPointer currentReaderBucket = readerCurrentBucket;

                    @Getter
                    private final RepairBucketPointer currentRepairBucket = repairCurrentBucketPointer;
                })
                       .status(Response.Status.OK)
                       .build();


    }
}
