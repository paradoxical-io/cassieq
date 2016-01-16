package io.paradoxical.cassieq.clustering.allocation;

import com.google.inject.Inject;
import io.paradoxical.cassieq.clustering.eventing.EventBus;
import io.paradoxical.cassieq.clustering.eventing.EventListener;
import io.paradoxical.cassieq.model.events.QueueAddedEvent;
import io.paradoxical.cassieq.model.events.QueueDeletingEvent;

import java.util.HashSet;
import java.util.Set;

public class EventableDispatchAllocater implements ResourceAllocator {
    private final Set<String> eventBusListenerIds = new HashSet<>();
    private final EventBus eventBus;
    private final ResourceAllocator allocator;

    @Inject
    public EventableDispatchAllocater(
            EventBus eventBus,
            ResourceAllocator allocator) {
        this.eventBus = eventBus;
        this.allocator = allocator;

        eventBusListenerIds.add(eventBus.register(QueueAddedEvent.class, new EventListener<QueueAddedEvent>() {
            @Override
            public void onMessage(final QueueAddedEvent item) {
                claim();
            }
        }));

        eventBusListenerIds.add(eventBus.register(QueueDeletingEvent.class, new EventListener<QueueDeletingEvent>() {
            @Override
            public void onMessage(final QueueDeletingEvent item) {
                claim();
            }
        }));
    }

    @Override
    public void claim() {
        allocator.claim();
    }

    @Override
    public void close() throws Exception {
        eventBusListenerIds.forEach(eventBus::unregister);

        allocator.close();
    }
}
