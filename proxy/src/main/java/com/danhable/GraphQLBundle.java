package com.danhable;

import com.danhable.api.MultiplexingQueryInvoker;
import com.danhable.api.GraphQLUtils;
import com.google.api.graphql.rejoiner.Schema;
import com.google.api.graphql.rejoiner.SchemaProviderModule;
import com.google.inject.Guice;
import com.google.inject.Key;
import demo.services.org.OrgClientModule;
import demo.services.org.OrgQuerySchemaModule;
import graphql.schema.GraphQLSchema;

import graphql.schema.idl.SchemaGenerator;
import graphql.servlet.GraphQLHttpServlet;
import graphql.servlet.GraphQLQueryInvoker;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class GraphQLBundle implements ConfiguredBundle<ProxyConfiguration> {

    private GraphQLSchema loadLegacyGraphQLSchema() throws IOException {
        var schemaFiles = Files.find(Paths.get("/Users/danhable/go/src/graphql/schema"),
                                     Integer.MAX_VALUE,
                                     (path, attributes) -> attributes.isRegularFile() && path.toString().endsWith(".graphql"))
                               .map(Path::toFile)
                               .toArray(File[]::new);

        var typeDef = GraphQLUtils.loadTypeDefinitions(schemaFiles);
        var wiring = GraphQLUtils.newProxyRuntimeWiring(typeDef);

        return new SchemaGenerator().makeExecutableSchema(typeDef, wiring);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }

    @Override
    public void run(ProxyConfiguration configuration, Environment environment) throws Exception {
        try {
            var legacyServerSchema = loadLegacyGraphQLSchema();

            var rejoinerSchema = Guice.createInjector(new SchemaProviderModule(),
                                                     new OrgClientModule(),
                                                     new OrgQuerySchemaModule())
                                     .getInstance(Key.get(GraphQLSchema.class, Schema.class));

            var graphQLSchema = GraphQLUtils.stitch(legacyServerSchema, rejoinerSchema);

            final GraphQLQueryInvoker queryInvoker
                    = new MultiplexingQueryInvoker(legacyServerSchema,
                                                   GraphQLQueryInvoker.newBuilder()
                                                                .build());

            final graphql.servlet.GraphQLConfiguration config =
                    graphql.servlet.GraphQLConfiguration.with(graphQLSchema).with(queryInvoker).build();

            var servlet = GraphQLHttpServlet.with(config);

            environment.servlets().addServlet("graphql", servlet).addMapping("/graphql", "/schema.json");

        } catch(IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
