package org.onebusaway.gtfs_realtime.exporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeListener;
import org.onebusway.gtfs_realtime.exporter.GtfsRealtimeMutableProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.Position;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehicleDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

public class OBAFetcher implements GtfsRealtimeMutableProvider {

	private URL _url;
	
	public OBAFetcher() {
		try {
			_url = new URL("http://api.onebusaway.org/api/where/vehicles-for-agency/40.json?key=TEST");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
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
	public void setVehiclePositions(FeedMessage message) {
		JSONArray vehicles = null;
		int inServiceVehicles = 0;

		try {
			vehicles = downloadVehicleDetails();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
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
						System.out.println(o.getString("tripId"));
						inServiceVehicles++;
					}
					
					VehiclePosition.Builder vp = VehiclePosition.newBuilder();
					
					VehicleDescriptor.Builder vd = VehicleDescriptor.newBuilder();
					vd.setId(o.getString("vehicleId"));
					vp.setVehicle(vd);

					vp.setTimestamp(System.currentTimeMillis());

					Position.Builder position = Position.newBuilder();
					position.setLatitude((float) o.getJSONObject("location").getDouble("lat"));
					position.setLongitude((float) o.getJSONObject("location").getDouble("lon"));
					vp.setPosition(position);

					FeedEntity.Builder entity = FeedEntity.newBuilder();
					entity.setId("entityId");
					entity.setVehicle(vp);
					feedMessageBuilder.addEntity(entity);

					FeedMessage message = feedMessageBuilder.build();
					message.addEntity(entity);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			


			FeedMessage message = feedMessageBuilder.build();
		}
		
		/*if (vehicles != null) {
			for (int i = 0; i < vehicles.length(); i++) {
				JSONObject vehicle = null;
				try {
					vehicle = vehicles.getJSONObject(i);
				} catch (JSONException e) {
				}
				
				
				if (null != e.getElementsByTagName("activeTripId").item(0)) {
					TripDescriptor.Builder tripDescriptor = TripDescriptor
							.newBuilder();
					tripDescriptor.setRouteId(e
							.getElementsByTagName("activeTripId").item(0)
							.getTextContent());
					inServiceVehicles++;
				}

				VehicleDescriptor.Builder vehicleDescriptor = VehicleDescriptor
						.newBuilder();
				vehicleDescriptor.setId(e.getElementsByTagName("vehicleId")
						.item(0).getTextContent());
				System.out.println(vehicle);
			}
		}*/
		System.out.println(vehicles.length() + " vehicles processed, of which " + inServiceVehicles + " are active");

	}

	private static String getValue(String tag, Element element) {
		NodeList nodes = element.getElementsByTagName(tag).item(0)
				.getChildNodes();
		Node node = (Node) nodes.item(0);
		return node.getNodeValue();
	}
	

	@Override
	public void setVehiclePositions(FeedMessage arg0, boolean arg1) {
		// TODO Auto-generated method stub
		
	}
	
	/*private NodeList downloadVehicleDetails() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(_url.openStream());//(reader);
		doc.getDocumentElement().normalize();

		return doc.getElementsByTagName("vehicleStatus");
	}*/

   /**
	* @return a JSON array parsed from the data pulled from the SEPTA vehicle
	* data API.
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
