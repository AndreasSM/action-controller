package org.actioncontroller;

import org.actioncontroller.meta.ApiHttpExchange;
import org.actioncontroller.meta.HttpParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMappingFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;
import java.net.URL;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@HttpParameterMapping(ContextUrl.MappingFactory.class)
public @interface ContextUrl {

    public class MappingFactory implements HttpRequestParameterMappingFactory<ContextUrl> {
        @Override
        public HttpRequestParameterMapping create(ContextUrl annotation, Parameter parameter) {
            if (parameter.getType() == URL.class) {
                return ApiHttpExchange::getContextURL;
            } else if (parameter.getType() == String.class) {
                return exchange -> exchange.getContextURL().toString();
            }
            throw new IllegalArgumentException("Can't map " + parameter);
        }
    }
}