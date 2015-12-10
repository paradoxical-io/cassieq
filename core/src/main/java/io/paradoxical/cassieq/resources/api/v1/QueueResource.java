package io.paradoxical.cassieq.resources.api.v1;

import io.paradoxical.cassieq.dataAccess.exceptions.ExistingMonotonFoundException;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.MessageRepoFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.GetMessageResponse;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueCreateOptions;
import io.paradoxical.cassieq.model.QueueDefinition;
import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import io.paradoxical.cassieq.model.QueueName;
import com.google.inject.Inject;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import io.paradoxical.cassieq.workers.repair.RepairWorkerManager;
import org.joda.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;

@Path("/api/v1/queues")
@Api(value = "/api/v1/queues", description = "Queue api")
@Produces(MediaType.APPLICATION_JSON)
public class QueueResource extends BaseQueueResource {

    private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);
    private final RepairWorkerManager repairWorkerManager;

    @Inject
    public QueueResource(
            ReaderFactory readerFactory,
            MessageRepoFactory messageRepoFactory,
            MonotonicRepoFactory monotonicRepoFactory,
            QueueRepository queueRepository,
            RepairWorkerManager repairWorkerManager) {
        super(readerFactory, messageRepoFactory, monotonicRepoFactory, queueRepository);
        this.repairWorkerManager = repairWorkerManager;
    }

    @POST
    @Path("/")
    @ApiOperation(value = "Create Queue")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response createQueue(@Valid @NotNull QueueCreateOptions createOptions) {
        final QueueName queueName = createOptions.getQueueName();

        try {
            final QueueDefinition newQueueDefinition = QueueDefinition.builder()
                                                         .bucketSize(BucketSize.valueOf(createOptions.getBucketSize()))
                                                         .maxDeliveryCount(createOptions.getMaxDeliveryCount())
                                                         .queueName(createOptions.getQueueName())
                                                         .build();

            getQueueRepository().createQueue(newQueueDefinition);

            // try and start a repair worker for the new queue
            repairWorkerManager.refresh();
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("CreateQueue", queueName, e);
        }

        return Response.ok().status(Response.Status.CREATED).build();
    }

    @GET
    @Path("/{queueName}/messages/next")
    @ApiOperation(value = "Get Message")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 204, message = "No message"),
            @ApiResponse(code = 404, message = "Queue doesn't exist"),
            @ApiResponse(code = 500, message = "Server Error")
    })
    public Response getMessage(
            @PathParam("queueName") QueueName queueName,
            @QueryParam("invisibilityTime") @DefaultValue("30") Long invisibilityTime) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        final Optional<Message> messageOptional;

        try {
            messageOptional = getReaderFactory().forQueue(queueDefinition.get())
                                                .nextMessage(Duration.standardSeconds(invisibilityTime));
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

    @POST
    @Path("/{queueName}/messages")
    @ApiOperation(value = "Put Message")
    @ApiResponses(value = { @ApiResponse(code = 201, message = "Message Added"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response putMessage(
            @PathParam("queueName") QueueName queueName,
            @QueryParam("initialInvisibilityTime") @DefaultValue("0") Long initialInvisibilityTime,
            String message) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        try {
            final Message messageToInsert = Message.builder()
                                                   .blob(message)
                                                   .index(getMonotonicRepoFactory().forQueue(queueName)
                                                                                   .nextMonotonic())
                                                   .build();

            final Duration initialInvisibility = Duration.standardSeconds(initialInvisibilityTime);

            getMessageRepoFactory().forQueue(queueDefinition.get())
                                   .putMessage(messageToInsert,
                                               initialInvisibility);
        }
        catch (ExistingMonotonFoundException e) {
            logger.error(e, "Error");
            return buildErrorResponse("PutMessage", queueName, e);
        }

        return Response.status(Response.Status.CREATED).build();

    }

    @DELETE
    @Path("/{queueName}/messages")
    @ApiOperation(value = "Ack Message")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
                            @ApiResponse(code = 404, message = "Queue doesn't exist"),
                            @ApiResponse(code = 409, message = "CONFLICT: PopReceipt is stale"),
                            @ApiResponse(code = 500, message = "Server Error") })
    public Response ackMessage(
            @PathParam("queueName") QueueName queueName,
            @QueryParam("popReceipt") String popReceipt) {

        final Optional<QueueDefinition> queueDefinition = getQueueDefinition(queueName);
        if (!queueDefinition.isPresent()) {
            return buildQueueNotFoundResponse(queueName);
        }

        boolean messageAcked;

        try {
            messageAcked = getReaderFactory().forQueue(queueDefinition.get())
                                             .ackMessage(PopReceipt.valueOf(popReceipt));
        }
        catch (Exception e) {
            logger.error(e, "Error");
            return buildErrorResponse("AckMessage", queueName, e);
        }

        if (messageAcked) {
            return Response.noContent().build();
        }

        return Response.status(Response.Status.CONFLICT).entity("The message is already being reprocessed").build();
    }
}
