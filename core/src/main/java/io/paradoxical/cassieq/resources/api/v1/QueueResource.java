package io.paradoxical.cassieq.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.metrics.QueueTimer;
import io.paradoxical.cassieq.model.*;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.workers.QueueDeleter;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.joda.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/api/v1/accounts/{accountName}/queues")
@Api(value = "/api/v1/accounts/{accountName}/queues", description = "Queue api", tags = "cassieq")
@Produces(MediaType.APPLICATION_JSON)
public class QueueResource extends BaseQueueResource {

    private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);
    private final RepairWorkerManager repairWorkerManager;
    private final QueueDeleter queueDeleter;

    @Inject
    public QueueResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            DataContextFactory dataContextFactory,
            RepairWorkerManager repairWorkerManager,
            QueueDeleter.Factory queueDeleterFactory,
            @PathParam("accountName") AccountName accountName) {
        super(readerFactory, messageRepoFactory, monotonicRepoFactory, dataContextFactory.forAccount(accountName), accountName);
        this.repairWorkerManager = repairWorkerManager;
        this.queueDeleter = queueDeleterFactory.create(accountName);
    }

    @GET
    @Timed
    @ApiOperation(value = "Get all account queue definitions")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getQueueInfo() {

        final List<QueueDefinition> activeQueues = getQueueRepository().getActiveQueues();

        return Response.ok().entity(activeQueues).build();
    }

    @GET
    @Path("/{queueName}")
    @Timed
    @ApiOperation(value = "Get a queue definition")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getQueueInfo(
            @NotNull @PathParam("queueName") QueueName queueName) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final QueueDefinition definition = queueDefinition.get();

        return Response.ok().entity(definition).build();
    }

    @GET
    @Path("/{queueName}/statistics")
    @Timed
    @ApiOperation(value = "Get queue statistics")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getQueueStatistics(
            @NotNull @PathParam("queueName") QueueName queueName) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final QueueDefinition definition = queueDefinition.get();

        final Optional<Long> queueSize = getQueueRepository().getQueueSize(definition);

        Object returnEntity = new Object() {
            @JsonProperty("size")
            public long size = queueSize.orElse(0L);
        };

        return Response.ok().entity(returnEntity).build();
    }

    private boolean active(final Optional<QueueDefinition> queueDefinition) {
        return queueDefinition.isPresent() && queueDefinition.get().getStatus() == QueueStatus.Active;
    }

    @POST
    @Timed
    @ApiOperation(value = "Create Queue")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response createQueue(
            @Valid @NotNull QueueCreateOptions createOptions,
            @QueryParam("errorIfExists") @DefaultValue("false") Boolean errorIfExists) {
        final QueueName queueName = createOptions.getQueueName();

        try {
            final QueueDefinition newQueueDefinition =
                    QueueDefinition.builder()
                                   .bucketSize(BucketSize.valueOf(createOptions.getBucketSize()))
                                   .maxDeliveryCount(createOptions.getMaxDeliveryCount())
                                   .repairWorkerPollFrequencySeconds(createOptions.getRepairWorkerPollSeconds())
                                   .repairWorkerTombstonedBucketTimeoutSeconds(createOptions.getRepairWorkerBucketFinalizeTimeSeconds())
                                   .deleteBucketsAfterFinalization(createOptions.getDeleteBucketsAfterFinalize())
                                   .queueName(createOptions.getQueueName())
                                   .accountName(getAccountName())
                                   .build();

            final boolean wasInserted = getQueueRepository().createQueue(newQueueDefinition).isPresent();

            if (!wasInserted && errorIfExists) {
                return Response.status(Response.Status.CONFLICT).build();
            }

            // try and start a repair worker for the new queue
            repairWorkerManager.notifyChanges();

            if (wasInserted) {
                return Response.status(Response.Status.CREATED).build();
            }
            else {
                return Response.ok().build();
            }
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("CreateQueue", queueName, e);
        }

    }

    @DELETE
    @Path("/{queueName}")
    @Timed
    @QueueTimer(actionName = "delete")
    @ApiOperation(value = "Delete queue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No message"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response deleteQueue(@PathParam("queueName") QueueName queueName) {
        try {
            queueDeleter.delete(queueName);
        }
        catch (QueueAlreadyDeletingException e) {
            logger.error(e, "Error");
            return buildErrorResponse("DeleteQueue", queueName, e);
        }

        return Response.ok().build();
    }

    @GET
    @Path("/{queueName}/messages/next")
    @Timed
    @QueueTimer(actionName = "read")
    @ApiOperation(value = "Get Message")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No message"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getMessage(
            @NotNull @PathParam("queueName") QueueName queueName,
            @NotNull @QueryParam("invisibilityTimeSeconds") @DefaultValue("30") Long invisibilityTimeSeconds) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!active(queueDefinition)) {
            return buildQueueNotFoundResponse(queueName);
        }

        final Optional<Message> messageOptional;

        try {
            messageOptional = getReaderFactory().forQueue(getAccountName(), queueDefinition.get())
                                                .nextMessage(Duration.standardSeconds(invisibilityTimeSeconds));
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("GetMessage", queueName, e);
        }

        if (!messageOptional.isPresent()) {
            return Response.noContent().build();
        }

        final Message messageInstance = messageOptional.get();

        final String popReceipt = PopReceipt.from(messageInstance).toString();

        final String message = messageInstance.getBlob();
        final GetMessageResponse response = new GetMessageResponse(
                popReceipt,
                message,
                messageInstance.getDeliveryCount(),
                messageInstance.getTag()
        );


        return Response.ok(response)
                       .status(Response.Status.OK)
                       .build();
    }


    @PUT
    @Path("/{queueName}/messages")
    @Timed
    @QueueTimer(actionName = "update-message")
    @ApiOperation(value = "Update Message")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = UpdateMessageResponse.class),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 409, message = "CONFLICT: PopReceipt is stale"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response updateMessage(
            @NotNull @PathParam("queueName") QueueName queueName,
            @NotNull @QueryParam("popReceipt") String popReceiptRaw,
            UpdateMessageRequest clientUpdateRequest) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!active(queueDefinition)) {
            return buildQueueNotFoundResponse(queueName);
        }

        final MessageUpdateRequest updateRequest = MessageUpdateRequest.from(clientUpdateRequest, PopReceipt.valueOf(popReceiptRaw));

        final Optional<Message> message = getMessageRepoFactory().forQueue(queueDefinition.get()).updateMessage(updateRequest);

        if (message.isPresent()) {
            final UpdateMessageResponse updateMessageResponse =
                    new UpdateMessageResponse(PopReceipt.from(message.get()).toString(), message.get().getTag());

            return Response.ok().entity(updateMessageResponse).build();
        }

        return buildConflictResponse("Pop reciept is stale");
    }

    @POST
    @Path("/{queueName}/messages")
    @Timed
    @QueueTimer(actionName = "publish")
    @ApiOperation(value = "Put Message")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Message Added"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response putMessage(
            @NotNull @PathParam("queueName") QueueName queueName,
            @QueryParam("initialInvisibilityTime") @DefaultValue("0") Long initialInvisibilityTime,
            String message) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);

        if (!active(queueDefinition)) {
            return buildQueueNotFoundResponse(queueName);
        }

        try {
            final Message messageToInsert = Message.builder()
                                                   .blob(message)
                                                   .index(getMonotonicRepoFactory().forQueue(queueDefinition.get().getId())
                                                                                   .nextMonotonic())
                                                   .build();

            final Duration initialInvisibility = Duration.standardSeconds(initialInvisibilityTime);

            getMessageRepoFactory().forQueue(queueDefinition.get())
                                   .putMessage(messageToInsert,
                                               initialInvisibility);

            logger.with("index", messageToInsert.getIndex())
                  .with("tag", messageToInsert.getTag())
                  .debug("Adding message");
        }
        catch (ExistingMonotonFoundException e) {
            logger.error(e, "Error");

            return buildErrorResponse("PutMessage", queueName, e);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{queueName}/messages")
    @Timed
    @QueueTimer(actionName = "ack")
    @ApiOperation(value = "Ack Message")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 409, message = "CONFLICT: PopReceipt is stale"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response ackMessage(
            @NotNull @PathParam("queueName") QueueName queueName,
            @NotNull @QueryParam("popReceipt") String popReceiptRaw) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final PopReceipt popReceipt = PopReceipt.valueOf(popReceiptRaw);

        boolean messageAcked;

        try {
            messageAcked = getReaderFactory().forQueue(getAccountName(), queueDefinition.get())
                                             .ackMessage(popReceipt);
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("AckMessage", queueName, e);
        }

        if (messageAcked) {
            return Response.noContent().build();
        }

        return buildConflictResponse("The message is already being reprocessed");
    }


}
