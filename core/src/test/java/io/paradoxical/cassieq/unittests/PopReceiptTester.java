package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import io.paradoxical.cassieq.model.QueueId;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PopReceiptTester {
    @Test
    public void test_round_trip() throws Exception {
        final MonotonicIndex monotonicIndex = MonotonicIndex.valueOf(42);
        final int version = 3;

        Message m = Message.builder().index(monotonicIndex).version(version).build();

        final QueueId queueId = QueueId.valueOf("test");

        final PopReceipt popReceipt = PopReceipt.from(m, queueId);

        System.out.println(popReceipt);

        final PopReceipt components = PopReceipt.valueOf(popReceipt.toString());

        assertThat(components.getMessageIndex()).isEqualTo(monotonicIndex);
        assertThat(components.getMessageVersion()).isEqualTo(version);
        assertThat(components.getQueueId()).isEqualTo(queueId);
    }
}
