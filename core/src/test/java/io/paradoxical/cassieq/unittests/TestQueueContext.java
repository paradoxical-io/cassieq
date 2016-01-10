package io.paradoxical.cassieq.unittests;

import com.google.inject.Injector;
import io.paradoxical.cassieq.dataAccess.interfaces.QueueRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.factories.QueueDataContext;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.workers.reader.Reader;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import org.joda.time.Duration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Data
public class TestQueueContext {

    @NonNull
    private final QueueName queueName;

    @NonNull
    @Getter
    private final QueueDefinition queueDefinition;

    @NonNull
    private final Reader reader;
    private static final AccountName testAccountName = AccountName.valueOf("test");

    @Getter
    private QueueDataContext context;

    @Getter
    private QueueRepository queueRepository;

    public TestQueueContext(QueueName name, Injector injector) {
        this(QueueDefinition.builder()
                            .queueName(name)
                            .build(), injector);
    }

    public TestQueueContext(QueueDefinition queueDefinition, Injector injector) {
        this.reader = injector.getInstance(ReaderFactory.class).forQueue(testAccountName, queueDefinition);

        this.queueDefinition = queueDefinition;

        this.queueName = queueDefinition.getQueueName();

        final DataContextFactory factory = injector.getInstance(DataContextFactory.class);

        factory.getAccountRepository().createAccount(testAccountName);

        queueRepository = factory.forAccount(testAccountName);

        this.queueRepository.createQueue(queueDefinition);


        context = factory.forQueue(queueDefinition);
    }

    public Optional<Message> readNextMessage(Duration invisiblity) {
        return reader.nextMessage(invisiblity);
    }

    public Optional<Message> readNextMessage(int invisiblitySeconds) {
        return reader.nextMessage(Duration.standardSeconds(invisiblitySeconds));
    }

    public void putMessage(String blob) throws Exception {
        putMessage(0, blob);
    }

    public MonotonicIndex ghostMessage() {
        return context.getMonotonicRepository().nextMonotonic();
    }

    public void putMessage(int seconds, String blob) throws Exception {

        final MonotonicIndex monoton = context.getMonotonicRepository().nextMonotonic();

        context.getMessageRepository().putMessage(
                Message.builder()
                       .blob(blob)
                       .index(monoton)
                       .build(), Duration.standardSeconds(seconds));
    }

    public boolean readAndAckMessage(String blob, Long invisDuration) {
        Optional<Message> message = getReader().nextMessage(Duration.standardSeconds(invisDuration));

        assertThat(message.get().getBlob().equals(blob));

        final PopReceipt popReceipt = PopReceipt.from(message.get());

        return reader.ackMessage(popReceipt);
    }

    public boolean readAndAckMessage(String blob) {
        return readAndAckMessage(blob, 10L);
    }

    private void tombstone(int bucket) {
        context.getMessageRepository().tombstone(ReaderBucketPointer.valueOf(bucket));
    }

    public void finalize(int bucket) {
        context.getMessageRepository().finalize(RepairBucketPointer.valueOf(bucket));
    }
}
