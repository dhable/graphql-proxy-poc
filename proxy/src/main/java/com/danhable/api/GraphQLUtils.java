package com.danhable.api;

import graphql.language.ScalarTypeDefinition;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

import static graphql.Scalars.GraphQLBigDecimal;
import static graphql.Scalars.GraphQLBigInteger;
import static graphql.Scalars.GraphQLBoolean;
import static graphql.Scalars.GraphQLByte;
import static graphql.Scalars.GraphQLChar;
import static graphql.Scalars.GraphQLFloat;
import static graphql.Scalars.GraphQLID;
import static graphql.Scalars.GraphQLInt;
import static graphql.Scalars.GraphQLLong;
import static graphql.Scalars.GraphQLShort;
import static graphql.Scalars.GraphQLString;

import static java.util.Map.entry;
import static graphql.schema.GraphQLObjectType.newObject;



public class GraphQLUtils {

    private static Logger LOG = LoggerFactory.getLogger(GraphQLUtils.class);

    private static Map<String, GraphQLScalarType> BUILT_IN_SCALARS = Map.ofEntries(entry(GraphQLInt.getName(), GraphQLInt),
                                                                                   entry(GraphQLFloat.getName(), GraphQLFloat),
                                                                                   entry(GraphQLString.getName(), GraphQLString),
                                                                                   entry(GraphQLBoolean.getName(), GraphQLBoolean),
                                                                                   entry(GraphQLID.getName(), GraphQLID),
                                                                                   entry(GraphQLLong.getName(), GraphQLLong),
                                                                                   entry(GraphQLShort.getName(), GraphQLShort),
                                                                                   entry(GraphQLByte.getName(), GraphQLByte),
                                                                                   entry(GraphQLBigInteger.getName(), GraphQLBigInteger),
                                                                                   entry(GraphQLBigDecimal.getName(), GraphQLBigDecimal),
                                                                                   entry(GraphQLChar.getName(), GraphQLChar));


    public static boolean isBuiltInType(final ScalarTypeDefinition typeDef) {
        return BUILT_IN_SCALARS.containsKey(typeDef.getName());
    }


    public static TypeDefinitionRegistry loadTypeDefinitions(File...files) {
        var typeDefRegistry = new TypeDefinitionRegistry();

        for (var schemaFile: files) {
            var schemaParser = new SchemaParser();
            var fileTypeDef = schemaParser.parse(schemaFile);

            if (LOG.isDebugEnabled()) {
                LOG.debug("GraphQL type definitions loaded from {} => {}",
                          schemaFile.getAbsolutePath(),
                          fileTypeDef.types().entrySet()
                                     .stream()
                                     .map(Map.Entry::getKey)
                                     .collect(Collectors.joining(", ")));
            }

            typeDefRegistry.merge(fileTypeDef);
        }

        return typeDefRegistry;
    }


    public static RuntimeWiring newProxyRuntimeWiring(TypeDefinitionRegistry typeRegistery) {
        return RuntimeWiring.newRuntimeWiring()
                            .wiringFactory(new ProxyWiringFactory(typeRegistery))
                            .build();
    }


    public static GraphQLSchema stitch(GraphQLSchema s1, GraphQLSchema s2) {
        var newSchemaBuilder = GraphQLSchema.newSchema(s1)
                                            .additionalTypes(s2.getAdditionalTypes());

        // generlized merge of the query type - must be present on all schema instances
        var newQueryType = newObject(s1.getQueryType());
        for (var s2Field: s2.getQueryType().getFieldDefinitions()) {
            newQueryType.field(s2Field);
        }
        newSchemaBuilder.query(newQueryType);

        // generalized merge of the mutation types - either or both objects could have a null value
        if (s1.isSupportingMutations() && s2.isSupportingMutations()) {
            var newMutationType = newObject(s1.getMutationType());
            for (var s2Field: s2.getMutationType().getFieldDefinitions()) {
                newMutationType.field(s2Field);
            }
            newSchemaBuilder.mutation(newMutationType);
        } else if (s1.isSupportingMutations() || s2.isSupportingMutations()) {
            newSchemaBuilder.mutation(s1.isSupportingMutations() ? s1.getMutationType()
                                                                 : s2.getMutationType());
        }

        return newSchemaBuilder.build();
    }


}
