package io.paradoxical.cassieq.dataAccess;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.godaddy.logging.Logger;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.MessageDeleterJobProcessor;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.MonotonicRepoFactory;
import io.paradoxical.cassieq.factories.PointerRepoFactory;
import io.paradoxical.cassieq.model.BucketSize;
import io.paradoxical.cassieq.model.GenericMessagePointer;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.MessagePointer;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.QueueStatus;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static com.codahale.metrics.MetricRegistry.name;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.godaddy.logging.LoggerFactory.getLogger;

public class MessageDeletorJobProcessorImpl implements MessageDeleterJobProcessor {
    private final Session session;
    private final MetricRegistry metricRegistry;
    private final DeletionJob job;
    private final PointerRepository pointerRepository;
    private final MonotonicRepository monotonicRepository;

    private static final Logger logger = getLogger(MessageDeletorJobProcessorImpl.class);
    private final QueueRepository queueRepository;

    @Inject
    public MessageDeletorJobProcessorImpl(
            Session session,
            DataContextFactory dataContextFactory,
            MetricRegistry metricRegistry,
            @Assisted DeletionJob job) {
        this.session = session;
        this.metricRegistry = metricRegistry;
        this.job = job;

        QueueId queueId = job.getQueueIdentifier();

        queueRepository = dataContextFactory.forAccount(job.getAccountName());
        pointerRepository = dataContextFactory.getPointerRepository(queueId);
        monotonicRepository = dataContextFactory.getMonotonicRepository(queueId);
    }

    /**
     * Handles deleting messages and marking the job as complete
     */
    @Override
    public void start() {
        try (Timer.Context ignored = getMetricTimer()) {
            final MessagePointer startPointer = getMinStartPointer(pointerRepository, job.getBucketSize());

            final MessagePointer endPointer = monotonicRepository.getCurrent();

            delete(startPointer, endPointer);

            monotonicRepository.deleteAll();

            pointerRepository.deleteAll();

            queueRepository.deleteQueueStats(job.getQueueSizeCounterId());

            complete();
        }
    }

    private void complete() {
        // queue definition is now totally inactive
        queueRepository.tryAdvanceQueueStatus(job.getQueueName(), QueueStatus.Inactive);

        queueRepository.deleteCompletionJob(job);
    }

    private MessagePointer getMinStartPointer(PointerRepository pointerRepository, BucketSize bucketSize) {
        final MonotonicIndex repairPointer = pointerRepository.getRepairCurrentBucketPointer().startOf(bucketSize);

        final InvisibilityMessagePointer currentInvisPointer = pointerRepository.getCurrentInvisPointer();

        return GenericMessagePointer.valueOf(Math.min(repairPointer.get(), currentInvisPointer.get()));
    }

    private void delete(MessagePointer from, MessagePointer to) {

        final int lastBucketNumber = to.toBucketPointer(job.getBucketSize()).get().intValue();
        final int firstBucketNumber = from.toBucketPointer(job.getBucketSize()).get().intValue();

        List<Integer> batchBucketsToDelete = new ArrayList<>();

        final Iterator<Integer> bucketRangeIterator = IntStream.range(firstBucketNumber, lastBucketNumber + 1)
                                                               .boxed()
                                                               .iterator();

        while (bucketRangeIterator.hasNext()) {
            batchBucketsToDelete.add(bucketRangeIterator.next());

            if (batchBucketsToDelete.size() == getDeleteBatchSize()) {
                deleteAllMessagesInBuckets(ImmutableList.copyOf(batchBucketsToDelete));

                batchBucketsToDelete.clear();
            }
        }

        if (!CollectionUtils.isEmpty(batchBucketsToDelete)) {
            deleteAllMessagesInBuckets(ImmutableList.copyOf(batchBucketsToDelete));
        }
    }

    private int getDeleteBatchSize() {
        return 100;
    }

    private void deleteAllMessagesInBuckets(final List<Integer> deletableBuckets) {
        final Statement delete = QueryBuilder.delete()
                                             .all()
                                             .from(Tables.Message.TABLE_NAME)
                                             .where(eq(Tables.Message.QUEUE_ID, job.getQueueIdentifier().get()))
                                             .and(in(Tables.Message.BUCKET_NUM, deletableBuckets));

        session.execute(delete);
    }

    private Timer.Context getMetricTimer() {

        try {
            return metricRegistry.timer(name("deletion", job.getQueueName().get())).time();
        }
        catch (Exception ex) {
            logger.warn(ex, "Error getting deletion metric");

            return new Timer().time();
        }
    }
}
