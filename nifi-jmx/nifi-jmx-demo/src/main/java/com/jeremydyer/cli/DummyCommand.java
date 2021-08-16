package com.jeremydyer.cli;

import com.jeremydyer.ApplicationConfiguration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;

/**
 * Created by jeremydyer on 11/19/14.
 */
public class DummyCommand
    extends ConfiguredCommand<ApplicationConfiguration> {

    public DummyCommand() {
        super("dummy", "Dummy dropwizard CLI command instance");
    }

    @Override
    protected void run(Bootstrap<ApplicationConfiguration> applicationConfigurationBootstrap,
                       Namespace namespace,
                       ApplicationConfiguration applicationConfiguration) throws Exception {
        //Run your code
    }
}