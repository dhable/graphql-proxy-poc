package com.danhable.api;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import graphql.language.InterfaceTypeDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetcherFactory;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.TypeResolver;
import graphql.schema.idl.FieldWiringEnvironment;
import graphql.schema.idl.InterfaceWiringEnvironment;
import graphql.schema.idl.ScalarWiringEnvironment;
import graphql.schema.idl.SchemaDirectiveWiring;
import graphql.schema.idl.SchemaDirectiveWiringEnvironment;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.UnionWiringEnvironment;
import graphql.schema.idl.WiringFactory;


public final class ProxyWiringFactory implements WiringFactory
{
    private static final Coercing PASSTHRU_COERCING = new Coercing()
    {
        @Override
        public Object serialize(Object dataFetcherResult) throws CoercingSerializeException
        {
            return dataFetcherResult;
        }

        @Override
        public Object parseValue(Object input) throws CoercingParseValueException
        {
            return input;
        }

        @Override
        public Object parseLiteral(Object input) throws CoercingParseLiteralException
        {
            return input;
        }
    };

    private final TypeDefinitionRegistry typeDefinitionRegistry;
    private final LoadingCache<String, GraphQLScalarType> scalarsCache =
            CacheBuilder.newBuilder()
                        .build(CacheLoader.from(this::defineScalarOnDemand));


    public ProxyWiringFactory(final TypeDefinitionRegistry typeDefinitionRegistry) {
        this.typeDefinitionRegistry = typeDefinitionRegistry;
    }

    @Override
    public boolean providesScalar(final ScalarWiringEnvironment environment)
    {
        var isBuiltInType = GraphQLUtils.isBuiltInType(environment.getScalarTypeDefinition());
        return !isBuiltInType && typeDefinitionRegistry.getType(environment.getScalarTypeDefinition().getName())
                                                       .filter(definition -> definition instanceof ScalarTypeDefinition)
                                                       .isPresent();
    }

    @Override
    public GraphQLScalarType getScalar(final ScalarWiringEnvironment environment)
    {
        return scalarsCache.getUnchecked(environment.getScalarTypeDefinition().getName());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private GraphQLScalarType defineScalarOnDemand(final String scalarTypeName)
    {
        final TypeDefinition typeDefinition = typeDefinitionRegistry.getType(scalarTypeName).get();
        return GraphQLScalarType.newScalar()
                                .name(scalarTypeName)
                                .definition((ScalarTypeDefinition) typeDefinition)
                                .coercing(PASSTHRU_COERCING)
                                .build();
    }

    @Override
    public boolean providesTypeResolver(final InterfaceWiringEnvironment environment)
    {
        return typeDefinitionRegistry.getType(environment.getInterfaceTypeDefinition().getName())
                                     .filter(typeDefinition -> typeDefinition instanceof InterfaceTypeDefinition)
                                     .isPresent();
    }

    @Override
    public TypeResolver getTypeResolver(final InterfaceWiringEnvironment environment)
    {
        final String interfaceTypeName = environment.getInterfaceTypeDefinition().getName();
        final TypeDefinition typeDefinition = typeDefinitionRegistry.getType(interfaceTypeName).get();
        GraphQLInterfaceType.newInterface()
                            .name(interfaceTypeName)
                            .definition(((InterfaceTypeDefinition) typeDefinition));
        ObjectTypeDefinition.newObjectTypeDefinition()
                            .comments(typeDefinition.getComments())
                            .description(((InterfaceTypeDefinition) typeDefinition).getDescription());
        return env ->
                       GraphQLObjectType.newObject()
                                        .name(interfaceTypeName)
                                        .definition((ObjectTypeDefinition) typeDefinition)
                                        .build();
    }

    @Override
    public boolean providesTypeResolver(UnionWiringEnvironment environment)
    {
        return false;
    }

    @Override
    public TypeResolver getTypeResolver(UnionWiringEnvironment environment)
    {
        return null;
    }

    @Override
    public boolean providesDataFetcherFactory(FieldWiringEnvironment environment)
    {
        return false;
    }

    @Override
    public <T> DataFetcherFactory<T> getDataFetcherFactory(FieldWiringEnvironment environment)
    {
        return null;
    }

    @Override
    public boolean providesSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment)
    {
        return false;
    }

    @Override
    public SchemaDirectiveWiring getSchemaDirectiveWiring(SchemaDirectiveWiringEnvironment environment)
    {
        return null;
    }

    @Override
    public boolean providesDataFetcher(FieldWiringEnvironment environment)
    {
        return false;
    }

    @Override
    public DataFetcher getDataFetcher(FieldWiringEnvironment environment)
    {
        return null;
    }

    @Override
    public DataFetcher getDefaultDataFetcher(FieldWiringEnvironment environment)
    {
        return null;
    }
}