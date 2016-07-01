package com.jeremydyer.managed;

import io.dropwizard.lifecycle.Managed;

/**
 * Created by jeremydyer on 11/19/14.
 */
public class DummyManaged
    implements Managed {

    @Override
    public void start() throws Exception {
        //Invoked when the Jetty web server has started. Start up your managed instance here.
    }

    @Override
    public void stop() throws Exception {
        //Stop your managed instance. Called when Jetty is shut down.
    }
}