package com.danhable;

import com.danhable.api.GraphQLBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;

import javax.servlet.DispatcherType;
import java.util.EnumSet;


public class ProxyApplication extends Application<ProxyConfiguration> {

    public static void main(final String[] args) throws Exception {
        new ProxyApplication().run(args);
    }

    @Override
    public String getName() {
        return "Proxy";
    }

    @Override
    public void initialize(final Bootstrap<ProxyConfiguration> bootstrap) {
        bootstrap.addBundle(new GraphQLBundle());
    }

    @Override
    public void run(final ProxyConfiguration configuration,
                    final Environment environment) {

        var cors = environment.servlets().addFilter("cors", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
    }

}
