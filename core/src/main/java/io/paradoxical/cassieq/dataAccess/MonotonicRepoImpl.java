package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import io.paradoxical.cassieq.dataAccess.interfaces.MonotonicRepository;
import io.paradoxical.cassieq.model.MonotonicIndex;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.paradoxical.cassieq.model.QueueName;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;

public class MonotonicRepoImpl extends RepositoryBase implements MonotonicRepository {
    private final Session session;
    private final QueueName queueName;

    @Inject
    public MonotonicRepoImpl(Session session, @Assisted QueueName queueName) {
        this.session = session;
        this.queueName = queueName;
    }

    @Override public MonotonicIndex nextMonotonic() {
        MonotonicIndex nextMonotonic = null;

        while(nextMonotonic == null) {
            nextMonotonic = incrementMonotonicValue();
        }

        return nextMonotonic;
    }

    @Override public MonotonicIndex getCurrent() {
        Statement statement = QueryBuilder.select()
                                          .all()
                                          .from(Tables.Monoton.TABLE_NAME)
                                          .where(eq(Tables.Monoton.QUEUENAME, queueName.get()));

        MonotonicIndex current = getOne(session.execute(statement), MonotonicIndex::map);

        return current == null ? MonotonicIndex.valueOf(0) : current;
    }

    @Override
    public void deleteAll() {
        final Statement delete = QueryBuilder.delete().all()
                                             .from(Tables.Monoton.TABLE_NAME)
                                             .where(eq(Tables.Monoton.QUEUENAME, queueName.get()));

        session.execute(delete);
    }

    private MonotonicIndex incrementMonotonicValue() {
        Long current = getCurrent().get();

        final long next = current + 1;

        Statement stat = QueryBuilder.update(Tables.Monoton.TABLE_NAME)
                                     .with(set(Tables.Monoton.VALUE, next))
                                     .where(eq(Tables.Monoton.QUEUENAME, queueName.get()))
                                     .onlyIf(eq(Tables.Monoton.VALUE, current));

        if(session.execute(stat).wasApplied()) {
            return MonotonicIndex.valueOf(current);
        }

        return null;
    }
}
