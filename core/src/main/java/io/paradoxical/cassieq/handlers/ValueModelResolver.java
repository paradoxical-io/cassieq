package io.paradoxical.cassieq.handlers;

import com.fasterxml.jackson.databind.JavaType;
import io.paradoxical.cassieq.model.mappers.Mappers;
import io.paradoxical.common.valuetypes.LongValue;
import io.paradoxical.common.valuetypes.StringValue;
import io.paradoxical.common.valuetypes.UuidValue;
import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.jackson.ModelResolver;
import io.swagger.models.Model;
import io.swagger.models.properties.LongProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.models.properties.UUIDProperty;

import java.lang.annotation.Annotation;
import java.util.Iterator;

public class ValueModelResolver extends ModelResolver {
    public ValueModelResolver() {
        super(Mappers.getJson());
    }

    @Override
    public Model resolve(
            final JavaType type,
            final ModelConverterContext context,
            final Iterator<ModelConverter> next) {

        return super.resolve(type, context, next);
    }

    @Override
    public Property resolveProperty(
            final JavaType propType,
            final ModelConverterContext context,
            final Annotation[] annotations,
            final Iterator<ModelConverter> next) {

        if (propType.isTypeOrSubTypeOf(StringValue.class)) {
            return new StringProperty();
        }

        if (propType.isTypeOrSubTypeOf(LongValue.class)) {
            return new LongProperty();
        }

        if (propType.isTypeOrSubTypeOf(UuidValue.class)) {
            return new UUIDProperty();
        }

        return super.resolveProperty(propType, context, annotations, next);
    }

    @Override
    protected JavaType getInnerType(final String innerType) {
        return super.getInnerType(innerType);
    }
}
