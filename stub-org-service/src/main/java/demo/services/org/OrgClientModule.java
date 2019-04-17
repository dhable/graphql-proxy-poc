package demo.services.org;

import com.google.inject.AbstractModule;
import demo.services.org.grpc.OrganizationGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class OrgClientModule extends AbstractModule {

    @Override
    protected void configure() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9001)
                                                      .usePlaintext(true)
                                                      .build();
        bind(OrganizationGrpc.OrganizationBlockingStub.class).toInstance(OrganizationGrpc.newBlockingStub(channel));
    }

}
