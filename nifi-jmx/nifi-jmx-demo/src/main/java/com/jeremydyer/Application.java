package com.jeremydyer;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.jeremydyer.cli.DummyCommand;
import com.jeremydyer.managed.DummyManaged;
import com.jeremydyer.resource.DummyResource;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Created by jeremydyer on 11/19/14.
 */
public class Application
        extends io.dropwizard.Application<ApplicationConfiguration> {

    @Override
    public void initialize(Bootstrap<ApplicationConfiguration> bootstrap) {
        bootstrap.addCommand(new DummyCommand());

        //Creates an Asset bundle to serve up static content. Served from http://localhost:8080/assets/
        bootstrap.addBundle(new AssetsBundle());
    }

    @Override
    public void run(ApplicationConfiguration configuration, Environment environment) throws Exception {

        final MetricRegistry metricsRegistry = new MetricRegistry();

        //Add managed instances.
        environment.lifecycle().manage(new DummyManaged());

        //Register your Web Resources like below.
        final DummyResource dummyResource = new DummyResource();
        environment.jersey().register(dummyResource);

        final JmxReporter reporter = JmxReporter.forRegistry(metricsRegistry).build();
        reporter.start();
    }

    public static void main(String[] args) throws Exception {
        new Application().run(args);
    }
}
