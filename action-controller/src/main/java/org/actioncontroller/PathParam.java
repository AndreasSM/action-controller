package org.actioncontroller;

import org.actioncontroller.meta.HttpParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMapping;
import org.actioncontroller.meta.HttpRequestParameterMappingFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Parameter;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@HttpParameterMapping(PathParam.MappingFactory.class)
public @interface PathParam {

    String value();

    public class MappingFactory implements HttpRequestParameterMappingFactory<PathParam> {
        @Override
        public HttpRequestParameterMapping create(PathParam annotation, Parameter parameter) {
            String name = annotation.value();
            return (exchange) -> exchange.pathParam(name, parameter);
        }
    }
}
