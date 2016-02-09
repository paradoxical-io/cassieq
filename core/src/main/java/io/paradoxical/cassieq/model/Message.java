package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import io.paradoxical.cassieq.dataAccess.SpecialIndex;
import io.paradoxical.cassieq.dataAccess.Tables;
import io.paradoxical.cassieq.model.time.Clock;
import lombok.Builder;
import lombok.Data;
import org.joda.time.DateTime;

@Data
@Builder
public class Message {
    private final MonotonicIndex index;

    @LoggingScope(scope = Scope.HASH)
    private final String blob;

    private DateTime nextVisiblityAt;

    private DateTime createdDate;

    private boolean isAcked;

    private int version = 0;

    private int deliveryCount = 0;

    private MessageTag tag;

    private final QueueId queueId;

    public boolean isVisible(Clock clock) {
        return nextVisiblityAt == null || nextVisiblityAt.isBefore(clock.now()) || nextVisiblityAt.isEqual(clock.now());
    }

    public boolean isTombstone() { return getIndex().equals(SpecialIndex.Tombstone.getIndex()); }

    public boolean isFinalizer() { return getIndex().equals(SpecialIndex.Finalizer.getIndex()); }

    @JsonIgnore
    public boolean isNotAcked() {
        return !isAcked;
    }

    @JsonIgnore
    public boolean isRevived(Clock clock) {
        return isNotAcked() && isVisible(clock) && getDeliveryCount() > 0;
    }

    @JsonIgnore
    public boolean isNotVisible(Clock clock) {
        return !isVisible(clock);
    }

    @JsonIgnore
    public boolean isNotSpecial() { return !isTombstone() && !isFinalizer(); }

    public PopReceipt getPopReceipt() {
        return PopReceipt.from(this);
    }

    public Message createNewWithIndex(MonotonicIndex index) {
        return Message.builder()
                      .blob(blob)
                      .index(index)
                      .version(0)
                      .isAcked(false)
                      .nextVisiblityAt(nextVisiblityAt)
                      .deliveryCount(deliveryCount)
                      .createdDate(createdDate)
                      .tag(tag)
                      .build();
    }

    public Message withNewVersion(int version) {
        return Message.builder()
                      .blob(blob)
                      .index(index)
                      .version(version)
                      .isAcked(isAcked)
                      .nextVisiblityAt(nextVisiblityAt)
                      .deliveryCount(deliveryCount)
                      .createdDate(createdDate)
                      .tag(tag)
                      .build();
    }

    public static Message fromRow(final Row row) {

        return Message.builder()
                      .blob(row.getString(Tables.Message.MESSAGE))
                      .index(MonotonicIndex.valueOf(row.getLong(Tables.Message.MONOTON)))
                      .isAcked(row.getBool(Tables.Message.ACKED))
                      .version(row.getInt(Tables.Message.VERSION))
                      .deliveryCount(row.getInt(Tables.Message.DELIVERY_COUNT))
                      .nextVisiblityAt(new DateTime(row.getTimestamp(Tables.Message.NEXT_VISIBLE_ON)))
                      .createdDate(new DateTime(row.getTimestamp(Tables.Message.CREATED_DATE)))
                      .tag(MessageTag.valueOf(row.getString(Tables.Message.TAG)))
                      .build();
    }
}
