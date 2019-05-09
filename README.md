# graphql-proxy-poc

The purpose of the code in this repository was to explore a means of accelerating the aggregation
of  gRPC services written in Java into a front facing GraphQL proxy for use within a React UI. 
Complicating the work was an existing GraphQL server written in go that needed to continue resolving
the existing types. The solution also needed to allow for new data resolvers to be written directly in
Java. 


## Contents

* `stub-org-service` is a made up gRPC service written in Java and only used for the purposes of
showing queries being forwarded and handled by an actual gRPC service.

* `proxy` is the actual GraphQL server implementation that attempts to solve the goals stated above.


## Approach

The gRPC integration was already part of a Google project, rejoiner. 

The first approaches created a generic `PassthruDataFetcher` and then registered that on different
GraphQL types. While easy to understand, this approach would result in each instance of `PassthruDataFetcher`
calling the go GraphQL service since types could be loaded in parallel. 

Pulling apart the query needed to happen at a lower level in the communication stack, namely before the
`graphql-java` library had mapped the query types into DataFetcher instances. Building a custom `QueryInvoker`
that leveraged the `Parser` and `AstPrinter` allowed me to determine which elements of the query were
handled by the go GraphQL server and which needed to be resolved locally. With that information, I was then
able to issue a new HTTP request to the go server async and allow the `graphql-java` server to resolve all
the internal bits.

Difficulty also occurs in merging the results back, specifically the `data` element. The best approach seems
to be turning the `data` Object into two Maps and then merging the maps into a single result.


## Findings

This approach does work but had a few downsides. First, it depends on the `Parser` class which is flagged
as an internal API. Each upgrade to `graphql-java` may require revisiting and fixing the custom `QueryInvoker`.
Additionally, the inbound request is parsed twice. Being able to provide filters to the query resolution process
or at least having callbacks would be a welcome design change to enable this kind of use case.

GraphQL fragments are also a pain to deal with. This highlights the need to maintain the custom `QueryInvoker` as
the GraphQL syntax is evolved and changed. Restricting the query syntax to basic types and avoiding constructs
like fragments and variables may also be used to avoid needing to maintain parity with the spec.


## Recommendation

Given that the go GraphQL server does not contain a large amount of data handling logic at this time, I would avoid
using a proxy in front of the server. Any GraphQL proxy is going to be aware of the schemas involved and have an
implementation aware of the language specification. It makes more sense to port the go code over to Java is one wishes
to leverage rejoiner for gluing the gRPC services together.