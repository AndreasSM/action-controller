package org.actioncontroller;

import org.actioncontroller.meta.HttpReturnMapperFactory;
import org.actioncontroller.meta.HttpReturnMapping;
import org.actioncontroller.meta.HttpReturnValueMapping;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
@HttpReturnMapping(ContentBody.MappingFactory.class)
public @interface ContentBody {

    String contentType() default "text/plain";

    class MappingFactory implements HttpReturnMapperFactory<ContentBody> {
        @Override
        public HttpReturnValueMapping create(ContentBody annotation, Class<?> returnType) {
            return (result, exchange) ->
                    exchange.write(annotation.contentType(), writer -> writer.write(String.valueOf(result)));
        }
    }
}