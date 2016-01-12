package io.paradoxical.cassieq.clustering.eventing;

import com.godaddy.logging.Logger;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class HazelcastEventBus implements EventBus {
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
    public <T extends Event> void register(Class<T> eventType, final EventListener<T> listener) {
        try {
            hazelcastInstance.getTopic(topicName).addMessageListener(new MessageListener<Object>() {
                private final String id = eventType.newInstance().getId();

                @Override
                public void onMessage(final Message<Object> message) {
                    if (id == null) {
                        logger.with(message.getMessageObject()).warn("Got message but id is null");

                        return;
                    }

                    listener.onMessage(((T) message.getMessageObject()));
                }
            });
        }
        catch (InstantiationException | IllegalAccessException e) {
            logger.with(eventType.getName()).error(e, "Error creating event type");
        }
    }
}
