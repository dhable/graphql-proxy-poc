package demo.services.org;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import static java.util.Objects.nonNull;

public class OrgServer {
    private final int port;
    private final Server server;


    OrgServer(int port) {
        this.port = port;

        var serverBuilder = ServerBuilder.forPort(port);
        this.server = serverBuilder.addService(new OrgService())
                                   .build();
    }


    void start() throws IOException {
        server.start();
        System.out.println(String.format("server started, listening on port %s", port));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("*** shutting dow gRPC server since JVM is shutting down ***");
            OrgServer.this.stop();
            System.err.println("*** server shut down ***");
        }));
    }


    void stop() {
        if (nonNull(server)) {
            server.shutdown();
        }
    }


    private void blockUntilShutdown() throws InterruptedException {
        if (nonNull(server)) {
            server.awaitTermination();
        }
    }


    public static void main(String[] args) throws Exception {
        var server = new OrgServer(9001);
        server.start();
        server.blockUntilShutdown();
    }


}
