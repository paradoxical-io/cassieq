package io.paradoxical.cassieq.configurations;

import io.paradoxical.common.valuetypes.ValueTypeWrapper;
import com.godaddy.logging.LoggingConfigs;
import org.joda.time.DateTime;

import java.net.URI;

public class LogMapping {
    public static void register(){
        LoggingConfigs.getCurrent()
                      .addOverride(URI.class, URI::toString)
                      .addOverride(ValueTypeWrapper.class, ValueTypeWrapper::toString)
                      .addOverride(DateTime.class, DateTime::toString)
                      .addOverride(Class.class, Class::toString);
    }
}
