package io.paradoxical.cassieq.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import io.paradoxical.cassieq.metrics.QueueTimer;
import io.paradoxical.cassieq.metrics.QueueTimerProvider;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.util.Optional;

public class QueueDelineatedMetricProvider implements ApplicationEventListener {
    private ImmutableMap<Method, QueueTimerProvider> timers = ImmutableMap.of();

    private final MetricRegistry metricRegistry;

    public QueueDelineatedMetricProvider(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onEvent(final ApplicationEvent event) {
        if (event.getType() != ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            return;
        }

        final ImmutableMap.Builder<Method, QueueTimerProvider> timerBuilder = ImmutableMap.builder();

        for (final Resource resource : event.getResourceModel().getResources()) {
            for (final ResourceMethod method : resource.getAllMethods()) {
                registerQueueTimers(timerBuilder, method);
            }

            for (final Resource childResource : resource.getChildResources()) {
                for (final ResourceMethod method : childResource.getAllMethods()) {
                    registerQueueTimers(timerBuilder, method);
                }
            }
        }

        timers = timerBuilder.build();

    }

    private void registerQueueTimers(final ImmutableMap.Builder<Method, QueueTimerProvider> builder, final ResourceMethod method) {
        final Method definitionMethod = method.getInvocable().getDefinitionMethod();
        final QueueTimer annotation = definitionMethod.getAnnotation(QueueTimer.class);

        if (annotation != null) {
            builder.put(definitionMethod, new QueueTimerProvider(annotation, metricRegistry));
        }
    }

    @Override
    public RequestEventListener onRequest(final RequestEvent event) {
        return new RequestEventListener() {

            private Optional<Timer.Context> t = Optional.empty();

            @Override
            public void onEvent(final RequestEvent requestEvent) {
                if (requestEvent.getType() == RequestEvent.Type.RESOURCE_METHOD_START) {

                    final Method definitionMethod = requestEvent.getUriInfo()
                                                                .getMatchedResourceMethod()
                                                                .getInvocable()
                                                                .getDefinitionMethod();

                    t = Optional.ofNullable(timers.get(definitionMethod))
                                .map(timer -> timer.startTimer(requestEvent.getUriInfo()));

                }
                else if (requestEvent.getType() == RequestEvent.Type.FINISHED) {
                    if(requestEvent.getContainerResponse() != null) {
                        final int status = requestEvent.getContainerResponse().getStatus();

                        // only log successes
                        if (status >= 200 || status < 300) {
                            t.ifPresent(Timer.Context::close);
                        }
                    }
                }
            }
        };
    }
}
