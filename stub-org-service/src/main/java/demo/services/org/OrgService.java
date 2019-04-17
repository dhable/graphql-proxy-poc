package demo.services.org;

import demo.services.org.grpc.DetailsRequest;
import demo.services.org.grpc.DetailsResponse;
import demo.services.org.grpc.OrganizationGrpc;
import io.grpc.stub.StreamObserver;

public class OrgService extends OrganizationGrpc.OrganizationImplBase {

    @Override
    public void details(DetailsRequest request, StreamObserver<DetailsResponse> responseObserver) {
        var resp = DetailsResponse.newBuilder()
                                  .setGuid(request.getGuid())
                                  .setCountry("US")
                                  .setName("GitHub")
                                  .build();
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

}
