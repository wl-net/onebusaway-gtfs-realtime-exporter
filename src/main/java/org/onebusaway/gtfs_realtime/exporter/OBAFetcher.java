package org.onebusaway.gtfs_realtime.exporter;

import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeListener;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class OBAFetcher implements GtfsRealtimeMutableProvider {

	@Override
	public void addGtfsRealtimeListener(GtfsRealtimeListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public FeedMessage getAlerts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedMessage getTripUpdates() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FeedMessage getVehiclePositions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeGtfsRealtimeListener(GtfsRealtimeListener arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fireUpdate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAlerts(FeedMessage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAlerts(FeedMessage arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTripUpdates(FeedMessage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTripUpdates(FeedMessage arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVehiclePositions(FeedMessage arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVehiclePositions(FeedMessage arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}

}
