package org.actioncontroller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApiServletCompositeException extends ApiServletException  {

    private List<ApiControllerCompositeException> exceptions = new ArrayList<>();

    public ApiServletCompositeException() {
        super(null);
    }

    public void addControllerException(ApiControllerCompositeException exception) {
        exceptions.add(exception);
    }

    @Override
    public String getMessage() {
        return "Failed to start because of problems with controllers: "
                + exceptions.stream().map(e -> e.getController().getClass().getName()).collect(Collectors.joining(", ")) + ":\n"
                + exceptions.stream().map(ApiControllerCompositeException::getMessage).collect(Collectors.joining("\t\n"));
    }

    public boolean isEmpty() {
        return exceptions.isEmpty();
    }
}
