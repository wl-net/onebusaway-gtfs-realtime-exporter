import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeConstants;

public class Converter {

	private URL _url;

	private String[] agencyIds = { "1", "3", "40" };

	private List<Integer> late_times;
	
	public Converter() {

	}

	public static void main(String[] args) {
		Converter c = new Converter();
		c.buildAll();
	}

	public void buildAll() {
		for (String s : agencyIds) {
			late_times = new ArrayList<Integer>();
			try {
				_url = new URL(
						"http://api.pugetsound.onebusaway.org/api/where/vehicles-for-agency/"
								+ s + ".json?version=2&key=TEST");
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			FeedMessage.Builder feedMessageBuilder = FeedMessage.newBuilder();

			FeedHeader.Builder header = FeedHeader.newBuilder();
			header.setTimestamp(System.currentTimeMillis());
			header.setIncrementality(Incrementality.FULL_DATASET);
			header.setGtfsRealtimeVersion(GtfsRealtimeConstants.VERSION);
			feedMessageBuilder.setHeader(header);
			feedMessageBuilder = this.build(feedMessageBuilder);
			if (feedMessageBuilder != null) {
				FeedMessage message = feedMessageBuilder.build();
				try {
					BufferedOutputStream out = new BufferedOutputStream(
							new FileOutputStream("/home/user/gtfs.agency" + s + ".buffer"));
					message.writeTo(out);
					out.close();

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public FeedMessage.Builder build(FeedMessage.Builder feedMessageBuilder) {
		JSONArray vehicles = null;
		int inServiceVehicles = 0;
		int processedVehicles = 0;

		try {
			vehicles = this.downloadVehicleDetails();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			return null;
		}

		if (vehicles != null) {
			for (int i = 0; i < vehicles.length(); i++) {
				JSONObject o;
				try {
					o = vehicles.getJSONObject(i);
					TripDescriptor.Builder tripDescriptor = null;

					if (o.getString("tripId").length() > 0) { // vehicles on a trip are in service, not deadheaded or at a base
						tripDescriptor = TripDescriptor
								.newBuilder();
//						tripDescriptor.setRouteId(o.getString("routeId"));
						tripDescriptor.setTripId(o.getString("tripId").replaceAll(".*_", ""));
						tripDescriptor.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
						inServiceVehicles++;
					} else {
						continue;
					}

					VehiclePosition.Builder vp = VehiclePosition.newBuilder();

					VehicleDescriptor.Builder vd = VehicleDescriptor
							.newBuilder();
					vd.setId(o.getString("vehicleId"));
					vp.setVehicle(vd);

					vp.setTimestamp(Long.parseLong(o.getString("lastLocationUpdateTime")));

					try {
						o.getJSONObject("location");
					} catch (JSONException e) {
						continue;
					}
					
					try {
						o.getJSONObject("tripStatus");
					} catch (JSONException e) {
						continue;
					}
					if (o.getJSONObject("tripStatus").getInt("lastUpdateTime") > System.currentTimeMillis() - 5 * 60 * 1000) {
						continue;
					}
					
				    StopTimeEvent.Builder arrival = StopTimeEvent.newBuilder();
				    arrival.setDelay(o.getJSONObject("tripStatus").getInt("scheduleDeviation"));

				    StopTimeUpdate.Builder stopTimeUpdate = StopTimeUpdate.newBuilder();
				    stopTimeUpdate.setArrival(arrival);
				    stopTimeUpdate.setStopId(o.getJSONObject("tripStatus").getString("nextStop").replaceAll(".*_", ""));

				    TripUpdate.Builder tripUpdate = TripUpdate.newBuilder();
				    tripUpdate.addStopTimeUpdate(stopTimeUpdate);
				    tripUpdate.setTrip(tripDescriptor);
					
					Position.Builder position = Position.newBuilder();
					position.setLatitude((float) o.getJSONObject("location")
							.getDouble("lat"));
					position.setLongitude((float) o.getJSONObject("location")
							.getDouble("lon"));
					vp.setPosition(position);

					FeedEntity.Builder entity = FeedEntity.newBuilder();
					entity.setId(o.getString("vehicleId").replaceAll(".*_", ""));
					//entity.setVehicle(vp);
					entity.setTripUpdate(tripUpdate);

					late_times.add(arrival.getDelay());
					//System.out.println("vehicleId=" + vd.getId() + " on tripId " + tripDescriptor.getTripId() + " is " + arrival.getDelay()/60 + " minutes late");

					feedMessageBuilder.addEntity(entity);
					processedVehicles++;
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}

			}
		}
		
		System.out.println(processedVehicles + " / " + vehicles.length() + " vehicles processed, of which "
				+ inServiceVehicles + " are active." + getStats());
		
		if (inServiceVehicles == 0) {
			System.err.println("Agency GTFS-realtime feed down.");
			return null;
		}
		
		return feedMessageBuilder;
	}
	
	public String getStats() {
		int early = 0, earlyCount = 0, earliest = 0, late = 0, lateCount = 0, latest = 0;
		for (Integer i: late_times) {
			if (i > 300) {
				lateCount++;
				late += i;
				if (i > latest)
					latest = i;
			} else if (i < -120) {
				earlyCount++;
				early += i;
				
				if (i < earliest)
					earliest = i;
			}
		}
		
		if (earlyCount == 0)
			earlyCount = 1;
		
		
		return " Extended details: late count: " + lateCount + " early count: " + earlyCount + ", " 
		+ (late / lateCount)/60	+ " minutes and " + late / earlyCount % 60 + " seconds, latest: " 
		+ latest/60  + " minutes, " + latest%60 + " seconds"
				;
	}

	/**
	 * @return a JSON array parsed from the data pulled from the OBA vehicle
	 *         data API.
	 */
	private JSONArray downloadVehicleDetails() throws IOException,
			JSONException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				_url.openStream()));
		JSONTokener tokener = new JSONTokener(reader);
		JSONObject object = new JSONObject(tokener);

		return object.getJSONObject("data").getJSONArray("list");
	}
}
