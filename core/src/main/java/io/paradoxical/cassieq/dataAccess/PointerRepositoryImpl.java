package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Pointer;
import io.paradoxical.cassieq.model.PointerType;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.model.QueueId;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import lombok.NonNull;

import java.util.function.Function;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

public class PointerRepositoryImpl extends RepositoryBase implements PointerRepository {
    private final Session session;
    private final QueueId queueId;

    @Inject
    public PointerRepositoryImpl(
            @NonNull Provider<Session> session,
            @NonNull @Assisted QueueDefinition definition) {
        this.session = session.get();
        this.queueId = definition.getId();
    }

    @Override
    public ReaderBucketPointer advanceMessageBucketPointer(
            @NonNull final ReaderBucketPointer original,
            @NonNull final ReaderBucketPointer next) {
        if (tryMovePointer(PointerType.BUCKET_POINTER, next, pointerEqualsClause(original))) {
            return next;
        }

        // someone else moved it, get that value
        return getReaderCurrentBucket();
    }

    @Override
    public InvisibilityMessagePointer tryMoveInvisiblityPointerTo(
            @NonNull final InvisibilityMessagePointer original,
            @NonNull final InvisibilityMessagePointer destination) {

        //If the destination is less than the current pointer value, move the pointer.
        if (tryMovePointer(PointerType.INVISIBILITY_POINTER, destination, pointerGreaterThanClause(destination))) {
            return destination;
        }

        //If the pointer was not moved, attempt to move the pointer to the destination if the original pointer value equals the current pointer value.
        if (tryMovePointer(PointerType.INVISIBILITY_POINTER, destination, pointerEqualsClause(original))) {
            return destination;
        }

        // someone else moved it, get that value
        return getCurrentInvisPointer();
    }

    @Override
    public RepairBucketPointer advanceRepairBucketPointer(@NonNull final RepairBucketPointer original, @NonNull final RepairBucketPointer next) {
        if (tryMovePointer(PointerType.REPAIR_BUCKET, next, pointerEqualsClause(original))) {
            return next;
        }

        // someone else moved it, get that value
        return getRepairCurrentBucketPointer();
    }

    @Override
    public InvisibilityMessagePointer getCurrentInvisPointer() {
        final InvisibilityMessagePointer pointer = getPointer(PointerType.INVISIBILITY_POINTER, InvisibilityMessagePointer::map);

        return pointer == null ? InvisibilityMessagePointer.valueOf(0) : pointer;
    }

    @Override
    public ReaderBucketPointer getReaderCurrentBucket() {
        final ReaderBucketPointer pointer = getPointer(PointerType.BUCKET_POINTER, ReaderBucketPointer::map);

        return pointer == null ? ReaderBucketPointer.valueOf(0) : pointer;
    }

    @Override
    public RepairBucketPointer getRepairCurrentBucketPointer() {
        final RepairBucketPointer pointer = getPointer(PointerType.REPAIR_BUCKET, RepairBucketPointer::map);

        return pointer == null ? RepairBucketPointer.valueOf(0) : pointer;
    }

    @Override
    public void deleteAll() {
        final Statement delete = QueryBuilder.delete().all()
                                             .from(Tables.Pointer.TABLE_NAME)
                                             .where(eq(Tables.Pointer.QUEUE_ID, queueId.get()));

        session.execute(delete);
    }

    private <T extends Pointer> T getPointer(PointerType pointerType, Function<Row, T> mapper) {
        Statement query = QueryBuilder.select()
                                      .all()
                                      .from(Tables.Pointer.TABLE_NAME)
                                      .where(eq(Tables.Pointer.QUEUE_ID, queueId.get()))
                                      .and(eq(Tables.Pointer.POINTER_TYPE, pointerType.toString()));

        return getOne(session.execute(query), mapper);
    }

    private <T extends Pointer> boolean tryMovePointer(PointerType pointerType, T destination, Clause clause) {

        Statement statement = QueryBuilder.update(Tables.Pointer.TABLE_NAME)
                                          .with(set(Tables.Pointer.VALUE, destination.get()))
                                          .where(eq(Tables.Pointer.QUEUE_ID, queueId.get()))
                                          .and(eq(Tables.Pointer.POINTER_TYPE, pointerType.toString()))
                                          .onlyIf(clause);

        return session.execute(statement).wasApplied();
    }


    private Clause pointerEqualsClause(Pointer pointer) {
        return eq(Tables.Pointer.VALUE, pointer.get());
    }

    private Clause pointerGreaterThanClause(Pointer pointer) {
        return gt(Tables.Pointer.VALUE, pointer.get());
    }
}
