/**
 * Copyright (C) 2012 Google, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.gtfs_realtime.visualizer;

import java.io.FileReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onebusaway.cli.CommandLineInterfaceLibrary;
import org.onebusaway.guice.jsr250.LifecycleService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class VisualizerMain{
	
	private static final String ARG_GTFS_RT_CONFIG_JSON = "config";
	private static final int _DEFAULT_MIN_REFRESH = 10;
	private static final int _DEFAULT_REFRESH = 15;
	private static final String _DEFAULT_AGENCY = "Agency";
	private static final boolean _DEFAULT_DYNAMIC_REFRESH = true;
	private static final int _DEFAULT_HTTP_PORT = 8080;
	
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
		
		if (!msg.equals("") || args.length == 0 || CommandLineInterfaceLibrary.wantsHelp(args) || !cli.hasOption(ARG_GTFS_RT_CONFIG_JSON)) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar demo.jar --config config.json", "Visualize GTFS-realtime vehicle position data.", options, msg, true);
			System.exit(-1);
		}
		
		Set<Module> modules = new HashSet<Module>();
		VisualizerModule.addModuleAndDependencies(modules);
		
		Injector injector = Guice.createInjector(modules);
		injector.injectMembers(this);
		
		VisualizerService service = injector.getInstance(VisualizerService.class);
		VisualizerServer server = injector.getInstance(VisualizerServer.class);
		
		JSONObject jsonConfig = (JSONObject) new JSONTokener(new FileReader(cli.getOptionValue(ARG_GTFS_RT_CONFIG_JSON))).nextValue();
		
		// Set false to disable dynamic refresh interval adjustment and lock to initial value, defaults to _DEFAULT_DYNAMIC_REFRESH
		boolean dynamicRefresh = jsonConfig.optBoolean("dynamicRefresh", _DEFAULT_DYNAMIC_REFRESH);
		service.setDynamicRefresh(dynamicRefresh);
		
		// Sets minimum refresh interval, defaults to _DEFAULT_MIN_REFRESH
		int minRefresh = jsonConfig.optInt("minRefresh", _DEFAULT_MIN_REFRESH);
		service.setMinimumInterval(minRefresh);
		
		// Set TCP port on which to bind the server, defaults to _DEFAULT_HTTP_PORT
		int httpPort = jsonConfig.optInt("httpPort", _DEFAULT_HTTP_PORT);
		server.setPort(httpPort);
		
		JSONArray jsonConfigItems = jsonConfig.getJSONArray("gtfsRtSources");
		for (int i = 0; i < jsonConfigItems.length(); i++) {
			JSONObject item = jsonConfigItems.getJSONObject(i);
			String agency = item.optString("agency", _DEFAULT_AGENCY);
			URL url = new URL(item.getString("url")); // URL is required
			int refreshRate = item.optInt("refreshRate", _DEFAULT_REFRESH);
			double hue = item.optDouble("hue", 0);
			GTFSRTSource source = new GTFSRTSource(agency, url, refreshRate, hue);
			service.addGTFSRTSource(source);
		}
		
		LifecycleService lifecycleService = injector.getInstance(LifecycleService.class);
		lifecycleService.start();
	}
	
	private void buildOptions(Options options) {
		options.addOption(ARG_GTFS_RT_CONFIG_JSON, true, "JSON Configuration File, contains configuration and GTFS-RT URLs");
	}
	
}
