package com.danhable.db;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SidecarClient implements Client {



    private static String formatArgument(Map.Entry<String, Object> entry) {
        var key = entry.getKey();
        var val = entry.getValue();

        return key + ":"
                   + (val instanceof String ? "\"" + val + "\""
                                            : val.toString());
    }

    private static String formatRequestElement(Request graphQLRequest) {
        var req = new StringBuilder();
        req.append(graphQLRequest.type);

        if (!graphQLRequest.arguments.isEmpty()) {
            req.append("(")
                      .append(graphQLRequest.arguments.entrySet()
                                                      .stream()
                                                      .map(SidecarClient::formatArgument)
                                                      .collect(Collectors.joining(",")))
                      .append(")");
        }

        req.append("{")
           .append(String.join(",", graphQLRequest.fields))
           .append("}");

        return req.toString();
    }


    @Override
    public Object execute(Request... requests) throws Exception {

        var request = Arrays.stream(requests)
                            .map(SidecarClient::formatRequestElement)
                            .collect(Collectors.joining("\n"));

        System.out.println("outbound graphql query =>\n" + request + "\n-------");

        // TODO: make http request
        return Map.of("databases", List.of(Map.of("guid", "1", "owner", "dan", "region", "US East"),
                                           Map.of("guid", "2", "owner", "dan", "region", "US Central"),
                                           Map.of("guid", "3", "owner", "tom", "region", "US East")));
    }

}
