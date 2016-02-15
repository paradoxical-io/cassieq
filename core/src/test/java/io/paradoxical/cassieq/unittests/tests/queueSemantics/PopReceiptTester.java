package io.paradoxical.cassieq.unittests.tests.queueSemantics;

import categories.BuildVerification;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.MonotonicIndex;
import io.paradoxical.cassieq.model.PopReceipt;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class PopReceiptTester {
    @Test
    public void test_round_trip() throws Exception {
        final MonotonicIndex monotonicIndex = MonotonicIndex.valueOf(42);
        final int version = 3;

        Message m = Message.builder().index(monotonicIndex).version(version).build();

        final PopReceipt popReceipt = PopReceipt.from(m);

        System.out.println(popReceipt);

        final PopReceipt components = PopReceipt.valueOf(popReceipt.toString());

        assertThat(components.getMessageIndex()).isEqualTo(monotonicIndex);
        assertThat(components.getMessageVersion()).isEqualTo(version);
    }
}
