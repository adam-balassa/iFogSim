package org.fog.utils;

import fi.aalto.cs.extensions.Monitor;

public class NetworkUsageMonitor implements Monitor {

	private static double networkUsage = 0.0;
	
	public static void sendingTuple(double latency, double tupleNwSize){
		networkUsage += latency*tupleNwSize;
	}
	
	public static void sendingModule(double latency, long moduleSize){
		networkUsage += latency*moduleSize;
	}
	
	public static double getNetworkUsage(){
		return networkUsage;
	}

	@Override
	public void clear() {
		networkUsage = 0.0;
	}
}
