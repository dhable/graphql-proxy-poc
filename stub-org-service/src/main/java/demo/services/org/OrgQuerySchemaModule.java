package demo.services.org;

import com.google.api.graphql.rejoiner.Namespace;
import com.google.api.graphql.rejoiner.Query;
import com.google.api.graphql.rejoiner.SchemaModule;
import demo.services.org.grpc.DetailsRequest;
import demo.services.org.grpc.DetailsResponse;
import demo.services.org.grpc.OrganizationGrpc;


@Namespace("org")
public class OrgQuerySchemaModule extends SchemaModule {

    @Query("details")
    DetailsResponse listDetails(DetailsRequest request, OrganizationGrpc.OrganizationBlockingStub client) {
        return client.details(request);
    }

}
