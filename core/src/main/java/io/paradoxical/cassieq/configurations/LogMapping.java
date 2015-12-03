package io.paradoxical.cassieq.configurations;

import io.paradoxical.common.valuetypes.ValueTypeWrapper;
import com.godaddy.logging.LoggingConfigs;
import org.joda.time.DateTime;

import java.net.URI;

public class LogMapping {
    public static void register(){
        LoggingConfigs.getCurrent()
                      .withOverride(URI.class, URI::toString)
                      .withOverride(ValueTypeWrapper.class, ValueTypeWrapper::toString)
                      .withOverride(DateTime.class, DateTime::toString)
                      .withOverride(Class.class, Class::toString);
    }
}
