package io.paradoxical.cassieq.clustering.eventing;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class HazelcastEventBus implements EventBus {
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final Logger logger = getLogger(HazelcastEventBus.class);

    private final HazelcastInstance hazelcastInstance;

    private final String topicName = "events";

    @Inject
    public HazelcastEventBus(HazelcastInstance instance) {
        hazelcastInstance = instance;
    }

    @Override
    public <T extends Event> void publish(final T event) {
        hazelcastInstance.getTopic(topicName).publish(event);
    }

    @Override
    public <T extends Event> String register(Class<T> eventType, final EventListener<T> listener) {
        try {
            return hazelcastInstance.getTopic(topicName).addMessageListener(new MessageListener<Object>() {
                private final String eventId = eventType.newInstance().getEventId();

                @Override
                public void onMessage(final Message<Object> message) {
                    if (eventId == null) {
                        logger.with(message.getMessageObject()).warn("Got message but id is null");

                        return;
                    }

                    if(!Objects.equals(eventId, ((Event) message.getMessageObject()).getEventId())){
                        return;
                    }

                    executorService.submit(() -> {
                        try {
                            listener.onMessage(((T) message.getMessageObject()));
                        }
                        catch (Throwable ex) {
                            logger.with("event-type", eventType.getName())
                                  .error(ex, "Error handling event!");
                        }
                    });
                }
            });
        }
        catch (InstantiationException | IllegalAccessException e) {
            logger.with(eventType.getName()).error(e, "Error creating event type");
        }

        return null;
    }

    @Override
    public void unregister(final String registrationId) {
        hazelcastInstance.getTopic(topicName).removeMessageListener(registrationId);
    }
}
