package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Provider;
import io.paradoxical.cassieq.dataAccess.interfaces.PointerRepository;
import io.paradoxical.cassieq.model.InvisibilityMessagePointer;
import io.paradoxical.cassieq.model.Pointer;
import io.paradoxical.cassieq.model.PointerType;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.ReaderBucketPointer;
import io.paradoxical.cassieq.model.RepairBucketPointer;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.function.Function;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.gt;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

public class PointerRepositoryImpl extends RepositoryBase implements PointerRepository {
    private final Session session;
    private final QueueName queueName;

    @Inject
    public PointerRepositoryImpl(Provider<Session> session, @Assisted QueueName queueName) {
        this.session = session.get();
        this.queueName = queueName;
    }

    @Override public ReaderBucketPointer advanceMessageBucketPointer(final ReaderBucketPointer original, final ReaderBucketPointer next) {
        return movePointer(PointerType.BUCKET_POINTER, original, next, pointerEqualsClause(original));
    }

    @Override public InvisibilityMessagePointer tryMoveInvisiblityPointerTo(
            final InvisibilityMessagePointer original, final InvisibilityMessagePointer destination) {

        //If the destination is less than the current pointer value, move the pointer.
        InvisibilityMessagePointer pointer = movePointer(PointerType.INVISIBILITY_POINTER, original, destination, pointerGreaterThanClause(destination));

        //If the pointer was not moved, attempt to move the pointer to the destination if the original pointer value equals the current pointer value.
        return pointer.get().equals(original.get()) ?
               movePointer(PointerType.INVISIBILITY_POINTER, original, destination, pointerEqualsClause(original)) :
               pointer;
    }

    @Override public RepairBucketPointer advanceRepairBucketPointer(final RepairBucketPointer original, final RepairBucketPointer next) {
        return movePointer(PointerType.REPAIR_BUCKET, original, next, pointerEqualsClause(original));
    }

    @Override public InvisibilityMessagePointer getCurrentInvisPointer() {
        final InvisibilityMessagePointer pointer = getPointer(PointerType.INVISIBILITY_POINTER, InvisibilityMessagePointer::map);

        return pointer == null ? InvisibilityMessagePointer.valueOf(0) : pointer;
    }

    @Override public ReaderBucketPointer getReaderCurrentBucket() {
        final ReaderBucketPointer pointer = getPointer(PointerType.BUCKET_POINTER, ReaderBucketPointer::map);

        return pointer == null ? ReaderBucketPointer.valueOf(0) : pointer;
    }

    @Override public RepairBucketPointer getRepairCurrentBucketPointer() {
        final RepairBucketPointer pointer = getPointer(PointerType.REPAIR_BUCKET, RepairBucketPointer::map);

        return pointer == null ? RepairBucketPointer.valueOf(0) : pointer;
    }

    private <T extends Pointer> T getPointer(PointerType pointerType, Function<Row, T> mapper) {
        Statement query = QueryBuilder.select()
                                      .all()
                                      .from(Tables.Pointer.TABLE_NAME)
                                      .where(eq(Tables.Pointer.QUEUENAME, queueName.get()))
                                      .and(eq(Tables.Pointer.POINTER_TYPE, pointerType.toString()));

        return getOne(session.execute(query), mapper);
    }

    private <T extends Pointer> T movePointer(PointerType pointerType, T original, T destination, Clause clause) {

        Statement statement = QueryBuilder.update(Tables.Pointer.TABLE_NAME)
                                          .with(set(Tables.Pointer.VALUE, destination.get()))
                                          .where(eq(Tables.Pointer.QUEUENAME, queueName.get()))
                                          .and(eq(Tables.Pointer.POINTER_TYPE, pointerType.toString()))
                                          .onlyIf(clause);

        return session.execute(statement)
                      .wasApplied() ? destination : original;
    }


    private Clause pointerEqualsClause(Pointer pointer) {
        return eq(Tables.Pointer.VALUE, pointer.get());
    }

    private Clause pointerGreaterThanClause(Pointer pointer) {
        return gt(Tables.Pointer.VALUE, pointer.get());
    }
}
