package io.paradoxical.cassieq.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import io.paradoxical.cassieq.factories.InvisStrategyFactory;
import io.paradoxical.cassieq.factories.ReaderFactory;
import io.paradoxical.cassieq.model.Message;
import io.paradoxical.cassieq.model.QueueDefinition;
import io.paradoxical.cassieq.workers.DefaultMessageConsumer;
import io.paradoxical.cassieq.workers.MessageConsumer;
import io.paradoxical.cassieq.workers.reader.InvisConsumerAdapter;
import io.paradoxical.cassieq.workers.reader.InvisStrategy;
import io.paradoxical.cassieq.workers.reader.OnMessageConsumed;
import io.paradoxical.cassieq.workers.reader.PointerBasedInvisManager;
import io.paradoxical.cassieq.workers.reader.Reader;
import io.paradoxical.cassieq.workers.reader.ReaderImpl;
import org.joda.time.Duration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ReaderModule extends AbstractModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                        .implement(Reader.class, ReaderImpl.class)
                        .build(ReaderFactory.class));


        install(new FactoryModuleBuilder()
                        .implement(MessageConsumer.class, DefaultMessageConsumer.class)
                        .build(DefaultMessageConsumer.Factory.class));
    }

    @Provides
    public InvisStrategyFactory invisStrategyFactory(Injector injector) {
        return queueDefinition -> {
            final Injector childInjector = getForDefinition(injector, queueDefinition);

            final PointerBasedInvisManager pointerBasedInvisManager = childInjector.getInstance(PointerBasedInvisManager.class);

            List<InvisStrategy> strategyList = Collections.singletonList(pointerBasedInvisManager);

            return new InvisStrategy() {
                @Override
                public Optional<Message> tryConsumeNextVisibleMessage(final Duration invisiblity) {
                    return strategyList.stream()
                                       .map(strategy -> strategy.tryConsumeNextVisibleMessage(invisiblity))
                                       .findFirst()
                                       .get();
                }

                @Override
                public void onMessageConsumed(final Message message, final Duration invisiblity) {
                    strategyList.forEach(strategy -> strategy.onMessageConsumed(message, invisiblity));
                }
            };
        };
    }

    @Provides
    public OnMessageConsumed.OnMessageConsumedFactory onMessageConsumedFactory(Injector injector) {
        return queueDefinition -> {
            final Injector childInjector = getForDefinition(injector, queueDefinition);

            final List<OnMessageConsumed> consumers =
                    Collections.singletonList(new InvisConsumerAdapter(childInjector.getInstance(PointerBasedInvisManager.class)));

            return (message, invis) -> consumers.forEach(consumer -> consumer.markConsumed(message, invis));
        };
    }

    private Injector getForDefinition(Injector injector, final QueueDefinition queueDefinition) {
        return injector.createChildInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(QueueDefinition.class).annotatedWith(Assisted.class).toInstance(queueDefinition);
            }
        });
    }
}

