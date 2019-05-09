package com.danhable.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.language.AstPrinter;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.Node;
import graphql.language.OperationDefinition;

import graphql.language.Selection;
import graphql.language.SelectionSet;
import graphql.parser.Parser;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLQueryInvoker;
import graphql.servlet.GraphQLSingleInvocationInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static graphql.language.OperationDefinition.Operation.*;
import static java.util.Objects.nonNull;
import static org.asynchttpclient.Dsl.*;


public class CustomQueryInvoker extends GraphQLQueryInvoker {
    private static final Logger LOG = LoggerFactory.getLogger(CustomQueryInvoker.class);
    private final GraphQLSchema schemaToForward;
    private final GraphQLQueryInvoker forwardingQueryInvoker;
    private final ObjectMapper jsonMapper;


    public CustomQueryInvoker(final GraphQLSchema schemaToForward, final GraphQLQueryInvoker forwardingQueryInvoker) {
        super(null, null, null, null, null);
        this.schemaToForward = schemaToForward;
        this.forwardingQueryInvoker = forwardingQueryInvoker;
        jsonMapper = new ObjectMapper();
    }


    private List<Selection> fieldsToProxy(final SelectionSet selections, final GraphQLObjectType forwardOperationType) {
        // compare only top level fields on query/mutation/subscription
        return selections.getSelections()
                         .parallelStream()
                         .filter(val -> {
                             var field = (Field) val;
                             return nonNull(forwardOperationType.getFieldDefinition(field.getName()));
                         })
                         .collect(Collectors.toList());
    }


    private List<String> findReferencedFragments(final Collection<? extends Node> nodes) {
        var allNodes = new LinkedList<>(nodes);

        for (int i = 0; i < allNodes.size(); i++) {
            var current = allNodes.get(i);
            allNodes.addAll(current.getChildren());
        }

        return allNodes.parallelStream()
                       .filter(n -> n instanceof FragmentSpread)
                       .map(n -> ((FragmentSpread) n).getName())
                       .collect(Collectors.toList());
    }

    private static GraphQLError newGraphQLError(final Object errorObj) {
        return null;
    }


    private Future<ExecutionResult> executeGraphQLQuery(final String query) {
        var result = new CompletableFuture<ExecutionResult>();

        try (var httpClient = asyncHttpClient()) {
            httpClient.preparePost("http://localhost:9091/graphql")
                      .setBody(query)
                      .setHeader("Content-Type", "application/json")
                      .setHeader("Accept", "application/json")
                      .execute()
                      .toCompletableFuture()
                      .whenComplete((response, throwable) -> {
                          if (nonNull(throwable)) {
                              result.completeExceptionally(throwable);
                          } else {
                              try {
                                  var execResultBuilder = ExecutionResultImpl.newExecutionResult();
                                  var respContent = jsonMapper.readValue(response.getResponseBody(), HashMap.class);

                                  var errors = respContent.get("errors");
                                  if (nonNull(errors)) {
                                      List<GraphQLError> errors2 = ((List<Object>)errors).stream()
                                                                                         .map(CustomQueryInvoker::newGraphQLError)
                                                                                         .collect(Collectors.toList());
                                      execResultBuilder.addErrors(errors2);
                                  }

                                  execResultBuilder.data(respContent.get("data"));
                                  result.complete(execResultBuilder.build());
                              } catch (Exception ex) {
                                  result.completeExceptionally(ex);
                              }
                          }
                      });
        } catch (IOException ex) {
            result.completeExceptionally(ex);
        }

        return result;
    }


    @Override
    public ExecutionResult query(GraphQLSingleInvocationInput singleInvocationInput) {

        // Step 1: Find the OperationDefinition in the parsed stream of definitions
        var queryDoc = new Parser().parseDocument(singleInvocationInput.getExecutionInput().getQuery());
        var queryDefs = queryDoc.getDefinitions();
        var operationDef = (OperationDefinition) queryDefs.parallelStream()
                                                          .filter(d -> d instanceof OperationDefinition)
                                                          .findFirst() // currently the spec doesn't allow for multiple values
                                                          .orElseThrow();

        // Step 2: Pick apart the queryDef objects and pull out fields and fragments definitions that need to be
        //         forwarded to the next graphql server.
        var fieldsToForward = fieldsToProxy(operationDef.getSelectionSet(),
                                            operationDef.getOperation() == QUERY ? schemaToForward.getQueryType()
                                                                                 : schemaToForward.getMutationType());
        var referencedFragmentNames = findReferencedFragments(fieldsToForward);
        var fragmentDefToProxy = queryDefs.stream()
                                          .filter(d -> {
                                              if (d instanceof FragmentDefinition) {
                                                  var fragmentDef = (FragmentDefinition) d;
                                                  return referencedFragmentNames.contains(fragmentDef.getName());
                                              }
                                              return false;
                                          })
                                          .collect(Collectors.toList());

        //  Step 3: Build a new query String that only contains the fields and fragment pieces that should
        //          be sent to the second graphql server. This should not include any elements for code that
        //          is handled in this server's data fetchers.
        var newProxyQuery = operationDef.deepCopy();
        newProxyQuery.setSelectionSet(SelectionSet.newSelectionSet()
                                                  .selections(fieldsToForward)
                                                  .build());

        List<Definition> proxyQueryComponents = new ArrayList<>();
        proxyQueryComponents.add(newProxyQuery);
        proxyQueryComponents.addAll(fragmentDefToProxy);

        var proxyQueryString = AstPrinter.printAst(new Document(proxyQueryComponents));
        LOG.debug("query to proxy = {}", proxyQueryString);

        // Step 4: Execute forward query async
        var proxyExecResultFuture = executeGraphQLQuery(proxyQueryString);

        // Step 5: Execute local query through wrapped invoker, fields that should be forwarded
        //         will end up being returned in the result as null values.
        var localResults = forwardingQueryInvoker.query(singleInvocationInput);

        // Step 6: Merge results of the forwarded query into the current execution results
        try {
            var proxyResults = proxyExecResultFuture.get();

            return ExecutionResultImpl.newExecutionResult()
                    .addErrors(localResults.getErrors())
                    .addErrors(proxyResults.getErrors())
                    // TODO: generic way to merge data objects? convert to maps and then merge?
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}
