package org.onebusaway.gtfs_realtime.visualizer;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GTFSRTSource{
	private URL myURL;
	private String myAgency;
	private int myRefreshRate;
	private long myMostRecentRefresh = -1;
	private Map<String, Vehicle> vehicles = new HashMap<String, Vehicle>();
	private double myHue;
	
	public GTFSRTSource(String agency, URL url, int refreshRate) {
		myAgency = agency;
		myURL = url;
		myRefreshRate = refreshRate;
		myHue = Math.random();
	}
	
	public GTFSRTSource(String agency, URL url, int refreshRate, double hue) {
		myAgency = agency;
		myURL = url;
		myRefreshRate = refreshRate;
		if ((hue % 1) <= 0) {
			myHue = Math.random();
		} else {
			myHue = hue;
		}
	}
	
	public URL getURL() {
		return myURL;
	}
	
	public String getAgency() {
		return myAgency;
	}
	
	public int getRefreshRate() {
		return myRefreshRate;
	}
	
	public long getMostRecentRefresh() {
		return myMostRecentRefresh;
	}
	
	public double getHue() {
		return myHue;
	}
	
	public void setRefreshRate(int refreshRate) {
		myRefreshRate = refreshRate;
	}
	
	public void setMostRecentRefresh(long mostRecentRefresh) {
		myMostRecentRefresh = mostRecentRefresh;
	}
	
	public void setHue(double hue) {
		myHue = hue % 1;
	}
	
	public void putVehicle(String k, Vehicle v) {
		vehicles.put(k, v);
	}
	
	public Collection<Vehicle> getVehicles() {
		return vehicles.values();
	}
	
	public Vehicle getVehicle(String k) {
		return vehicles.get(k);
	}
	
	@Override
	public String toString() {
		int updateRecency = -1;
		if (this.getMostRecentRefresh() != -1) {
			updateRecency = (int) ((System.currentTimeMillis() - this.getMostRecentRefresh()) / (2 * 1000));
		}
		String result = "\n\nAgency: " + this.getAgency() + ",\n";
		result += "URL: " + this.getURL().toString() + ",\n";
		result += "Refresh Rate: " + this.getRefreshRate() + " seconds,\n";
		if (updateRecency != -1) {
			result += "Last updated " + updateRecency + "ms ago.";
		} else {
			result += "Not yet updated.";
		}
		result += "\n";
		return result;
	}
	
}
