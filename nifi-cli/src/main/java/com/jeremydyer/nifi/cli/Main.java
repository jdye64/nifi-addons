package com.jeremydyer.nifi.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jeremydyer.nifi.cli.configuration.Environment;
import com.jeremydyer.nifi.cli.configuration.NiFiCLIConfiguration;
import com.jeremydyer.nifi.cli.service.ControllerService;
import com.jeremydyer.nifi.cli.service.ControllerServiceImplementation;
import com.jeremydyer.nifi.cli.service.EnvironmentService;
import com.jeremydyer.nifi.cli.service.EnvironmentServiceImpl;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.web.api.entity.ControllerStatusEntity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * Created by jdyer on 4/8/16.
 */
public class Main {

    public static final String NIFI_CLI_CONF_FILE_PATH = System.getProperty("user.home") + "/.nificli/conf.json";

    public static void main(String[] args) {

        Main m = new Main();

        try {
            if (m.run(args) != 0) {
                System.out.println("Error: Problem occured while exiting NiFi-CLI");
            } else {
                System.out.println("Good-byte ;)");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    //Hardcoded options values .... eyeroll
    private static final String QUIT = "q";
    private static final String HELP = "help";
    private static final String LIST_ENVS = "list-envs";
    private static final String SET_ENV = "set-env";
    private static final String STATUS = "status";


    private String CLI_DISPLAY = "NiFi-CLI";
    private Environment CURRENT_ENV = null;
    private  NiFiCLIConfiguration conf = null;

    private ControllerService controllerService = null;
    private EnvironmentService environmentService = null;

    //Main method to be run on startup
    public int run(String[] args) throws Exception {
        conf = loadNiFiCLIConfiguration();

        controllerService = new ControllerServiceImplementation("localhost", "8080");

        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            //Build the NiFi-CLI options
            Options options = new Options();
            options.addOption(QUIT, false, "Quit NiFi-CLI");
            options.addOption(HELP, false, "Print Help");
            options.addOption("le", LIST_ENVS, false, "List NiFi cluster environments");

            options.addOption("se", SET_ENV, true, "Sets the current environment that should be used for all operations");
            options.addOption(STATUS, false, "Gets the status of the current NiFi environment");

            // automatically generate the help statement
            HelpFormatter formatter = new HelpFormatter();

            while (true) {
                System.out.print(CLI_DISPLAY);
                if (CURRENT_ENV != null) {
                    System.out.print(" (" + CURRENT_ENV.getEnvironmentName() + ")");
                }
                System.out.print("> ");

                InputStreamReader in = new InputStreamReader(System.in);
                BufferedReader br = new BufferedReader(in);
                String input = br.readLine();
                //System.out.println("DEBUG: Input -> '" + input + "'");

                //Parse the command.
                CommandLine cmd = parse(parser, options, StringUtils.split(input));
                if (cmd.hasOption(QUIT)) {
                    break;
                } else {
                    execute(formatter, options, cmd);
                }
            }
        }
        catch( ParseException exp ) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
        }

        return 0;   //TODO: return actual exit code
    }

    private CommandLine parse(CommandLineParser parser, Options options, String[] args) throws ParseException {
        CommandLine line = parser.parse( options, args );
        return line;
    }

    private int execute(HelpFormatter formatter, Options options, CommandLine cmd) throws Exception {

        if (cmd != null) {

            if (cmd.hasOption(HELP)) {
                formatter.printHelp("nificli", options);
            } else if (cmd.hasOption(LIST_ENVS) || cmd.hasOption("le")) {
                for (Environment env : conf.getEnvironments()) {
                    System.out.println("\t'" + env.getEnvironmentName() + "'");
                }
            } else if (cmd.hasOption(SET_ENV) || cmd.hasOption("se")) {
                String setEnv = cmd.getOptionValue(SET_ENV);

                //Make sure the Env actually exists.
                if (conf.doesEnvironmentExist(setEnv)) {
                    CURRENT_ENV = conf.getEnvironmentByName(cmd.getOptionValue(SET_ENV));
                    environmentService = new EnvironmentServiceImpl(CURRENT_ENV);
                } else {
                    System.out.println("NiFi environmont '" + setEnv + "' does not exist");
                }
            } else if (cmd.hasOption(STATUS)) {
                //TODO: pass in client id
                ControllerStatusEntity status = environmentService.getEnvironmentControllerStatus(CURRENT_ENV);
                System.out.println("Status: " + controllerService.getControllerStatus(""));
            }

            return 0;
        } else {
            return -1;
        }
    }


    /**
     * Loads the NiFiCLIConfiguration file. If the file or directory does not exist they will be created.
     *
     * @return
     * @throws Exception
     */
    public NiFiCLIConfiguration loadNiFiCLIConfiguration() throws Exception {
        File confFile = new File(NIFI_CLI_CONF_FILE_PATH);
        ObjectMapper mapper = new ObjectMapper();

        //Make sure that the file exists
        if (!confFile.exists()) {
            System.out.println("Unable to find NiFi CLI configuration file: '" + NIFI_CLI_CONF_FILE_PATH
                    + "' creating empty one now" );

            if (!confFile.getParentFile().exists()) {
                if (!confFile.getParentFile().mkdirs()) {
                    System.out.println("Failed to create NiFi-CLI configuration directory at: '" + confFile.getParent()
                            + "' unable to continue");
                    throw new Exception("Unable to create non-existant NiFi-CLI configuration directory at '" + confFile.getParent() + "'");
                }
            }

            //Create an empty NiFiCLIConfiguration file and write it to disk
            NiFiCLIConfiguration emptyConf = new NiFiCLIConfiguration();
            mapper.writeValue(confFile, emptyConf);
        }

        //Load the NiFiCLIConfiguration file.
        return mapper.readValue(confFile, NiFiCLIConfiguration.class);
    }

}
