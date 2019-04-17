package com.danhable.db;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface Client {

    class Request {
        final String type;
        final Map<String, Object> arguments;
        final List<String> fields;

        Request(final String type, final Map<String, Object> arguments, final List<String> fields) {
            this.type = type;
            this.arguments = arguments;
            this.fields = fields;
        }

        @Override
        public String toString() {
            return String.format("Request: %s(%s) {%s}",
                                 type,
                                 arguments.entrySet()
                                          .stream()
                                          .map(e -> String.format("%s: %s", e.getKey(), e.getValue().toString()))
                                          .collect(Collectors.joining(", ")),
                                 fields.stream().collect(Collectors.joining(", ")));
        }
    }

    Object execute(Request...requests) throws Exception;

}
