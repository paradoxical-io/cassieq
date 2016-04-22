package io.paradoxical.cassieq.discoverable.resources.api.v1;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.exceptions.QueueAlreadyDeletingException;
import io.paradoxical.cassieq.discoverable.auth.AccountAuth;
import io.paradoxical.cassieq.discoverable.auth.AuthLevelRequired;
import io.paradoxical.cassieq.exceptions.ConflictException;
import io.paradoxical.cassieq.exceptions.ErrorEntity;
import io.paradoxical.cassieq.exceptions.QueryParamWithDeprecationDetectedError;
import io.paradoxical.cassieq.exceptions.QueueInternalServerError;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.metrics.QueueTimer;
import io.paradoxical.cassieq.model.*;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.validators.StringTypeValid;
import io.paradoxical.cassieq.resources.api.BaseQueueResource;
import io.paradoxical.cassieq.workers.MessagePublisher;
import io.paradoxical.cassieq.workers.QueueDeleter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.joda.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@AccountAuth
@Path("/api/v1/accounts/{accountName}/queues")
@Api(value = "/api/v1/accounts/{accountName}/queues", description = "Queue api", tags = "cassieq")
@Produces(MediaType.APPLICATION_JSON)
public class QueueResource extends BaseQueueResource {

    private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);
    private final QueueDeleter queueDeleter;
    private final MessagePublisher messagePublisher;

    @Inject
    public QueueResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            DataContextFactory dataContextFactory,
            MessagePublisher messagePublisher,
            QueueDeleter.Factory queueDeleterFactory,
            @StringTypeValid @PathParam("accountName") AccountName accountName) {
        super(readerFactory, messageRepoFactory, monotonicRepoFactory, dataContextFactory.forAccount(accountName), accountName);
        this.messagePublisher = messagePublisher;
        this.queueDeleter = queueDeleterFactory.create(accountName);
    }

    @GET
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.GetQueueInformation)
    @ApiOperation(value = "Get all account queue definitions")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getAccountQueues() {

        final List<QueueDefinition> activeQueues = getQueueRepository().getActiveQueues();

        return Response.ok().entity(activeQueues).build();
    }

    @GET
    @Path("/{queueName}")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.GetQueueInformation)
    @ApiOperation(value = "Get a queue definition")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getQueueDefinition(@StringTypeValid @PathParam("queueName") QueueName queueName) {

        final QueueDefinition queueDefinition = lookupQueueDefinition(queueName);

        return Response.ok().entity(queueDefinition).build();
    }

    @GET
    @Path("/{queueName}/statistics")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.GetQueueInformation)
    @ApiOperation(value = "Get queue statistics")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response getQueueStatistics(
            @StringTypeValid @PathParam("queueName") QueueName queueName) {

        final QueueDefinition definition = lookupQueueDefinition(queueName);

        final Optional<Long> queueSize = getQueueRepository().getQueueSize(definition);

        Object returnEntity = new Object() {
            @JsonProperty("size")
            public long size = queueSize.orElse(0L);
        };

        return Response.ok().entity(returnEntity).build();
    }

    @POST
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.CreateQueue)
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

            if (wasInserted) {
                return Response.status(Response.Status.CREATED).build();
            }

        }
        catch (WebApplicationException e) { throw e; }
        catch (Exception e) {
            logger.error(e, "Error");
            throw new QueueInternalServerError("CreateQueue", queueName, e);
        }


        if (errorIfExists) {
            throw new ConflictException("CreateQueue",
                                        "A queue named '%s' already exists.", queueName);
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("/{queueName}")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.DeleteQueue)
    @QueueTimer(actionName = "delete")
    @ApiOperation(value = "Delete queue")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No message"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response deleteQueue(@StringTypeValid @PathParam("queueName") QueueName queueName) {
        try {
            queueDeleter.delete(queueName);
        }
        catch (QueueAlreadyDeletingException e) {
            logger.error(e, "Error");
            throw new QueueInternalServerError("DeleteQueue", queueName, e);
        }

        return Response.ok().build();
    }

    @GET
    @Path("/{queueName}/messages/next")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.ReadMessage)
    @QueueTimer(actionName = "read")
    @ApiOperation(value = "Get Message")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No message"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getMessage(
            @StringTypeValid @PathParam("queueName") QueueName queueName,
            @NotNull @Min(0) @QueryParam("invisibilityTimeSeconds") @DefaultValue("30") Long invisibilityTimeSeconds) {

        final QueueDefinition definition = lookupQueueDefinition(queueName);

        final Optional<Message> messageOptional;

        try {
            messageOptional = getReaderFactory().forQueue(getAccountName(), definition)
                                                .nextMessage(Duration.standardSeconds(invisibilityTimeSeconds));
        }
        catch (Exception e) {
            logger.error(e, "Error reading next message");
            throw new QueueInternalServerError("GetMessage", queueName, e);
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
    @AuthLevelRequired(level = AuthorizationLevel.UpdateMessage)
    @QueueTimer(actionName = "update-message")
    @ApiOperation(value = "Update Message")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK", response = UpdateMessageResponse.class),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 409, message = "CONFLICT: PopReceipt is stale"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response updateMessage(
            @StringTypeValid @PathParam("queueName") QueueName queueName,
            @NotNull @QueryParam("popReceipt") String popReceiptRaw,
            UpdateMessageRequest clientUpdateRequest) {

        final QueueDefinition definition = lookupQueueDefinition(queueName);

        final MessageUpdateRequest updateRequest = MessageUpdateRequest.from(clientUpdateRequest, PopReceipt.valueOf(popReceiptRaw));

        final Optional<Message> message = getMessageRepoFactory().forQueue(definition).updateMessage(updateRequest);

        if (message.isPresent()) {
            final UpdateMessageResponse updateMessageResponse =
                    new UpdateMessageResponse(PopReceipt.from(message.get()).toString(), message.get().getTag());

            return Response.ok().entity(updateMessageResponse).build();
        }

        throw new ConflictException("UpdateMessage", "Pop receipt is stale.");

    }

    @POST
    @Path("/{queueName}/messages")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.PutMessage)
    @QueueTimer(actionName = "publish")
    @ApiOperation(value = "Put Message")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Message Added"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response putMessage(
            final @StringTypeValid @PathParam("queueName") QueueName queueName,
            final @ApiParam(hidden = true) @QueryParam("initialInvisiblityTime") Long initialInvisibilityTimeDeprecated,
            @QueryParam("initialInvisibilitySeconds") @DefaultValue("0") Long initialInvisibilityTime,
            final String message) {

        if (initialInvisibilityTimeDeprecated != null && initialInvisibilityTime == null) {
            initialInvisibilityTime = initialInvisibilityTimeDeprecated;
        }

        if (initialInvisibilityTime != null && initialInvisibilityTimeDeprecated != null) {
            throw new QueryParamWithDeprecationDetectedError(
                    new ErrorEntity("putMessage",
                                    "initialInvisiblityTime query param is deprecated but " +
                                    "used with the initialInvisibilityTime parameter. " +
                                    "Only one may be used at a time (initialInvisibilityTime preferred)"));
        }

        final QueueDefinition definition = lookupQueueDefinition(queueName);

        try {
            messagePublisher.put(definition, message, initialInvisibilityTime);
        }
        catch (ExistingMonotonFoundException e) {
            logger.error(e, "Error");

            throw new QueueInternalServerError("PutMessage", queueName, e);
        }

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{queueName}/messages")
    @Timed
    @AuthLevelRequired(level = AuthorizationLevel.AckMessage)
    @QueueTimer(actionName = "ack")
    @ApiOperation(value = "Ack Message")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 409, message = "CONFLICT: PopReceipt is stale"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response ackMessage(
            @StringTypeValid @PathParam("queueName") QueueName queueName,
            @NotNull @QueryParam("popReceipt") String popReceiptRaw) {

        final QueueDefinition definition = lookupQueueDefinition(queueName);

        final PopReceipt popReceipt = PopReceipt.valueOf(popReceiptRaw);

        boolean messageAcked;

        try {
            messageAcked = getReaderFactory().forQueue(getAccountName(), definition)
                                             .ackMessage(popReceipt);
        }
        catch (Exception e) {
            logger.error(e, "Error");
            throw new QueueInternalServerError("AckMessage", queueName, e);
        }

        if (messageAcked) {
            return Response.noContent().build();
        }

        throw new ConflictException("AckMessage", "The message is already being reprocessed.");
    }
}
