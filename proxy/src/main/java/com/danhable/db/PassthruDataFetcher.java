package com.danhable.db;

import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;

import java.util.List;
import java.util.stream.Collectors;

public class PassthruDataFetcher implements DataFetcher {

    private Client client;

    public PassthruDataFetcher(Client client) {
        this.client = client;
    }

    @Override
    public Object get(DataFetchingEnvironment environment) throws Exception {

        // Obtain the important bits about the request that was just made
        var nsTypeName = environment.getField().getName();
        var queryRequests = environment.getSelectionSet().getFields()
                                       .stream()
                                       .filter(queryField -> !queryField.getQualifiedName().contains("/"))
                                       .map(selectedField -> {
                                           var name = selectedField.getName();
                                           var args = selectedField.getArguments();
                                           var fields = selectedField.getSelectionSet().getFields()
                                                                     .stream()
                                                                     .map(SelectedField::getName)
                                                                     .collect(Collectors.toList());
                                           return new Client.Request(name, args, fields);
                                       })
                                       .toArray(Client.Request[]::new);

                    // new Client.Request[x]


        // Dump parsed information out to log files
        System.out.println("\n=========================");
        System.out.println(String.format("For subsystem %s: ", nsTypeName));
        for (var req: queryRequests) {
            System.out.println(req.toString());
        }
        System.out.println("=========================\n");


        // Tie into the client that will make the new graphql request
        return new DataFetcherResult(client.execute(queryRequests), List.of());
    }

}
