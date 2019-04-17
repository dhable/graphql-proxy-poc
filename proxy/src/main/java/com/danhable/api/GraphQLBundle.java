package com.danhable.api;

import com.danhable.db.SidecarClient;
import com.danhable.db.PassthruDataFetcher;
import com.google.api.graphql.rejoiner.Schema;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import demo.services.org.OrgClientModule;
import demo.services.org.OrgQuerySchemaModule;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLSchema;

import graphql.servlet.GraphQLHttpServlet;
import graphql.servlet.GraphQLQueryInvoker;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static graphql.Scalars.GraphQLString;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLTypeReference.typeRef;


public class GraphQLBundle implements Bundle {
    private static final Logger LOG = LoggerFactory.getLogger(GraphQLBundle.class);

    private static BufferedReader getResourceAsReader(final String name) {
        LOG.info("Loading GraphQL schema file: {}", name);

        var loader = Thread.currentThread().getContextClassLoader();
        var in = loader.getResourceAsStream(name);

        Objects.requireNonNull(in, String.format("resource not found: %s", name));

        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(Environment environment) {
//        var schemaParser = new SchemaParser();
//        var typeDefinitionRegistry = schemaParser.parse(getResourceAsReader("proxy.graphql"));
//        var wiring = buildRuntimeWiring();
//
//        var schemaGenerator = new SchemaGenerator();
//        var graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, wiring);

        /////////////////////////////////////////////////////////////////////////////

        var graphQLSchema = Guice.createInjector(new SchemaProviderModule(),
                                                 new OrgClientModule(),
                                                 new OrgQuerySchemaModule())
                                 .getInstance(Key.get(GraphQLSchema.class, Schema.class));


        graphQLSchema = GraphQLSchema.newSchema(graphQLSchema)
                                     .additionalType(newObject().name("DatabaseType")
                                                                .fields(
                                                                    List.of(
                                                                            newFieldDefinition().name("guid").type(GraphQLString).build(),
                                                                            newFieldDefinition().name("owner").type(GraphQLString).build(),
                                                                            newFieldDefinition().name("region").type(GraphQLString).build()))
                                                                .build())
                                     .additionalType(newObject().name("CaaSType")
                                                                .field(newFieldDefinition().name("databases")
                                                                                           .type(GraphQLList.list(typeRef("DatabaseType"))))
                                                                .build())
                                     .query(newObject(graphQLSchema.getQueryType()).field(newFieldDefinition().name("caas")
                                                                                                              .type(typeRef("CaaSType"))
                                                                                                              .dataFetcher(new PassthruDataFetcher(new SidecarClient()))))
                                     .build();


        /////////////////////////////////////////////////////////////////////////////
        final GraphQLQueryInvoker queryInvoker =
                GraphQLQueryInvoker.newBuilder()
//                    .withPreparsedDocumentProvider(provider)
//                    .withInstrumentation(factory.getInstrumentations())
                                   .build();

        final graphql.servlet.GraphQLConfiguration config =
                graphql.servlet.GraphQLConfiguration.with(graphQLSchema).with(queryInvoker).build();

        var servlet = GraphQLHttpServlet.with(config);

        environment.servlets().addServlet("graphql", servlet).addMapping("/graphql", "/schema.json");

    }
}
