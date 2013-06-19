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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

@Singleton
public class VisualizerService{
	
	private static final Logger _log = LoggerFactory.getLogger(VisualizerService.class);
	
	private List<GTFSRTSource> _sources = new ArrayList<GTFSRTSource>();
	
	private ScheduledExecutorService _executor;
	
	private List<VehicleListener> _listeners = new CopyOnWriteArrayList<VehicleListener>();
	
	private List<RefreshTask> _refreshTasks = new ArrayList<RefreshTask>();
	
	private int _minimumInterval;
	
	private boolean _dynamicRefreshInterval;
	
	public void addGTFSRTSource(GTFSRTSource source) {
		_sources.add(source);
	}
	
	@PostConstruct
	public void start() {
		_executor = Executors.newScheduledThreadPool(_sources.size());
		for (int id = 0; id < _sources.size(); id++) {
			_refreshTasks.add(new RefreshTask(id));
			GTFSRTSource _source = _sources.get(id);
			_executor.schedule(_refreshTasks.get(id), _source.getRefreshRate(), TimeUnit.SECONDS);
		}
	}
	
	@PreDestroy
	public void stop() {
		_executor.shutdownNow();
	}
	
	public void addListener(VehicleListener listener) {
		_listeners.add(listener);
	}
	
	public void removeListener(VehicleListener listener) {
		_listeners.remove(listener);
	}
	
	public void setMinimumInterval(int erval) {
		_minimumInterval = erval;
	}
	
	public void setDynamicRefresh(boolean dynamic) {
		_dynamicRefreshInterval = dynamic;
	}
	
	private void refresh(int id) throws IOException {
		GTFSRTSource _source = _sources.get(id);
		_log.info("refreshing vehicle positions for source " + id);
		_log.info(_source.toString());
		
		List<Vehicle> vehicles = new ArrayList<Vehicle>();
		boolean update = false;
		FeedMessage feed = FeedMessage.parseFrom(_source.getURL().openStream());
		
		for (FeedEntity entity : feed.getEntityList()) {
			if (!entity.hasVehicle()) {
				continue;
			}
			VehiclePosition vehicle = entity.getVehicle();
			if (!vehicle.hasPosition()) {
				continue;
			}
			Position position = vehicle.getPosition();
			Vehicle v = new Vehicle();
			v.setId(entity.getId());
			v.setLat(position.getLatitude());
			v.setLon(position.getLongitude());
			v.setLastUpdate(System.currentTimeMillis());
			
			Vehicle existing = _source.getVehicle(v.getId());
			if ((existing == null) || (existing.getLat() != v.getLat()) || (existing.getLon() != v.getLon())) {
				_source.putVehicle(v.getId(), v);
				update = true;
			} else {
				v.setLastUpdate(existing.getLastUpdate());
			}
			
			vehicles.add(v);
			
		}
		
		if (update) {
			_log.info("vehicles updated: " + vehicles.size());
			if (_dynamicRefreshInterval) {
				updateRefreshInterval(id);
			}
		}
		
		for (VehicleListener listener : _listeners) {
			listener.handleVehicles(_sources);
		}
		
		_executor.schedule(_refreshTasks.get(id), _source.getRefreshRate(), TimeUnit.SECONDS);
	}
	
	private void updateRefreshInterval(int id) {
		long currentTime = System.currentTimeMillis();
		GTFSRTSource _source = _sources.get(id);
		
		if (_source.getMostRecentRefresh() != -1) {
			int refreshInterval = (int) ((currentTime - _source.getMostRecentRefresh()) / (2 * 1000));
			_source.setRefreshRate(Math.max(_minimumInterval, refreshInterval));
			_log.info("refresh interval: " + refreshInterval);
		}
		_source.setMostRecentRefresh(currentTime);
		_sources.set(id, _source);
	}
	
	private class RefreshTask implements Runnable{
		
		private int myId;
		
		public RefreshTask(int id) {
			myId = id;
		}
		
		@Override
		public void run() {
			try {
				refresh(myId);
			} catch (Exception ex) {
				_log.error("error refreshing GTFS-realtime data for source id " + myId, ex);
			}
		}
	}
	
}
