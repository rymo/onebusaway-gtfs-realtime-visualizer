/**
 * Copyright (C) 2012 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs_realtime.visualizer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.commons.cli.*;
import org.onebusaway.cli.CommandLineInterfaceLibrary;
import org.onebusaway.guice.jsr250.LifecycleService;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class VisualizerMain {

  private static final String ARG_GTFS_RT_LIST = "gtfsRtList";
  private static final String ARG_VEHICLE_POSITIONS_URL = "vehiclePositionsUrl";
  private static final String ARG_HTTP_PORT = "httpPort";
  private static final String ARG_INIT_REFRESH = "refresh";
  private static final String ARG_MIN_REFRESH = "minRefresh";
  private static final String ARG_DYNAMIC_REFRESH = "lockRefresh";
  private static final String _DEFAULT_HTTP_PORT = "8080";

  public static void main(String[] args) throws Exception {
    VisualizerMain m = new VisualizerMain();
    m.run(args);
  }

  private void run(String[] args) throws Exception {

    Options options = new Options();
    buildOptions(options);
    Parser parser = new GnuParser();
    CommandLine cli = null;
    String msg = "";
    try {
      cli = parser.parse(options, args);
    } catch (ParseException e) {
      msg = e.getMessage();
    }
    
    if (!msg.equals("") || args.length == 0 || CommandLineInterfaceLibrary.wantsHelp(args)
            || !(cli.hasOption(ARG_VEHICLE_POSITIONS_URL) || cli.hasOption(ARG_GTFS_RT_LIST))) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp( "java -jar demo.jar", "Visualize GTFS-realtime vehicle position data.", options, msg, true );
      System.exit(-1);
    }

    Set<Module> modules = new HashSet<Module>();
    VisualizerModule.addModuleAndDependencies(modules);

    Injector injector = Guice.createInjector(modules);
    injector.injectMembers(this);

    VisualizerService service = injector.getInstance(VisualizerService.class);
    if (cli.hasOption(ARG_VEHICLE_POSITIONS_URL)) {
      service.addVehiclePositionsUrl(new URL(
              cli.getOptionValue(ARG_VEHICLE_POSITIONS_URL)));
    } else {
      BufferedReader in = new BufferedReader(new FileReader(cli.getOptionValue(ARG_GTFS_RT_LIST)));
      while (in.ready()) {
        String s = in.readLine();
        System.out.println(s);
        service.addVehiclePositionsUrl(new URL(s));
      }
      in.close();
    }
    if (cli.hasOption(ARG_INIT_REFRESH)) {
      service.setRefreshInterval(Integer.parseInt(cli.getOptionValue(ARG_INIT_REFRESH)));
    }
    if (cli.hasOption(ARG_MIN_REFRESH)) {
      service.setMinimumInterval(Integer.parseInt(cli.getOptionValue(ARG_MIN_REFRESH)));
    }
    if (cli.hasOption(ARG_DYNAMIC_REFRESH)) {
      service.setDynamicRefresh(false);
    }
    
    VisualizerServer server = injector.getInstance(VisualizerServer.class);
    if (cli.hasOption(ARG_HTTP_PORT)) {
      server.setPort(Integer.parseInt(cli.getOptionValue(ARG_HTTP_PORT, _DEFAULT_HTTP_PORT)));
    }

    LifecycleService lifecycleService = injector.getInstance(LifecycleService.class);
    lifecycleService.start();
  }

  private void buildOptions(Options options) {
    options.addOption(ARG_VEHICLE_POSITIONS_URL, true, "GTFS-realtime vehicle positions url");
    options.addOption(ARG_GTFS_RT_LIST, true, "File containing list of GTFS-realtime urls");
    options.addOption(ARG_HTTP_PORT, true, "TCP port on which to bind the server, defaults to " + _DEFAULT_HTTP_PORT);
    options.addOption(ARG_INIT_REFRESH, true, "Initial refresh interval, defaults to 20s");
    options.addOption(ARG_MIN_REFRESH, true, "Minimum refresh interval, defaults to 10s");
    options.addOption(ARG_DYNAMIC_REFRESH, false, "Set to disable dynamic refresh interval adjustment and lock to Initial value");
  }

}
