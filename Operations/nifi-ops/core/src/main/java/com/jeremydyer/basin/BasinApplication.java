package com.jeremydyer.basin;

import static com.jeremydyer.basin.VersionedApplication.versionedApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

@EnableAutoConfiguration
@ComponentScan(basePackages = "com.jeremydyer.basin")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class BasinApplication {

    public static void main(String[] args) {
        System.out.println("Starting Basin Application");
        if (!versionedApplication().showVersionInfo(args)) {
            ConfigurableApplicationContext context = null;
            if (args.length == 0) {
                context = SpringApplication.run(BasinApplication.class);
            } else {
                context = SpringApplication.run(BasinApplication.class, args);
            }
            resetStackAndClusterStates(context);
        }
    }

    private static void resetStackAndClusterStates(ConfigurableApplicationContext context) {
        context.getBean(BasinApplication.class).resetStates();
    }

}