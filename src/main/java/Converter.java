import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.jmx.Agent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onebusaway.gtfs_realtime.exporter.OBAFetcher;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedHeader;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.google.transit.realtime.GtfsRealtime.FeedHeader.Incrementality;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtimeConstants;

public class Converter {

	private URL _url;

	private String[] agencyIds = { "1", "3", "40" };

	public Converter() {

	}

	public static void main(String[] args) {
		OBAFetcher oba = new OBAFetcher();

		Converter c = new Converter();
		c.buildAll();
	}

	public void buildAll() {
		for (String s : agencyIds) {
			try {
				_url = new URL(
						"http://api.onebusaway.org/api/where/vehicles-for-agency/"
								+ s + ".json?key=TEST");
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

					if (o.getString("tripId").length() > 0) {
						TripDescriptor.Builder tripDescriptor = TripDescriptor
								.newBuilder();
						tripDescriptor.setRouteId(o.getString("tripId"));
						inServiceVehicles++;
					}

					VehiclePosition.Builder vp = VehiclePosition.newBuilder();

					VehicleDescriptor.Builder vd = VehicleDescriptor
							.newBuilder();
					vd.setId(o.getString("vehicleId"));
					vp.setVehicle(vd);

					vp.setTimestamp(System.currentTimeMillis());

					try {
						o.getJSONObject("location");
					} catch (JSONException e) {
						break;
					}
					Position.Builder position = Position.newBuilder();
					position.setLatitude((float) o.getJSONObject("location")
							.getDouble("lat"));
					position.setLongitude((float) o.getJSONObject("location")
							.getDouble("lon"));
					vp.setPosition(position);

					FeedEntity.Builder entity = FeedEntity.newBuilder();
					entity.setId(o.getString("vehicleId"));
					entity.setVehicle(vp);
					feedMessageBuilder.addEntity(entity);

					feedMessageBuilder.addEntity(entity);
				} catch (JSONException e) {
					e.printStackTrace();
					return null;
				}

			}
		}
		System.out.println(vehicles.length() + " vehicles processed, of which "
				+ inServiceVehicles + " are active");
		
		if (inServiceVehicles == 0) {
			System.err.println("Agency GTFS-realtime feed down.");
			return null;
		}
		
		return feedMessageBuilder;
	}

	/**
	 * @return a JSON array parsed from the data pulled from the SEPTA vehicle
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
