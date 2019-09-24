package org.actioncontroller.meta;

import org.actioncontroller.ApiControllerAction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface ApiControllerActionFactory<ANNOTATION extends Annotation> {

    ApiControllerAction create(ANNOTATION annotation, Object controller, Method action);

}