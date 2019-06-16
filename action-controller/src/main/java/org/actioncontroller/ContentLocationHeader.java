package org.actioncontroller;

import org.actioncontroller.meta.HttpReturnMapperFactory;
import org.actioncontroller.meta.HttpReturnMapping;
import org.actioncontroller.meta.HttpReturnValueMapping;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.URL;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
@HttpReturnMapping(ContentLocationHeader.MappingFactory.class)
public @interface ContentLocationHeader {

    class MappingFactory implements HttpReturnMapperFactory<ContentLocationHeader> {
        @Override
        public HttpReturnValueMapping create(ContentLocationHeader annotation, Class<?> returnType) {
            if (returnType == URL.class) {
                return (result, exchange) -> exchange.setResponseHeader("Content-location", result.toString());
            }
            return (result, exchange) ->
                    exchange.setResponseHeader("Content-Location", exchange.getApiURL() + result.toString());
        }

    }
}

