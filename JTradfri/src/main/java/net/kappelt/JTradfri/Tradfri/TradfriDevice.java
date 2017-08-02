/**
 * 
 */
package net.kappelt.JTradfri.Tradfri;

import java.util.ArrayList;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.json.JSONArray;
import org.json.JSONObject;

import net.kappelt.JTradfri.GWConnection;

/**
 * @author peter
 *
 */

/* device json:
$VAR1 = { 
          '9019' => 1, 											-> reachability state
          '3' => { 
                   '6' => 1, 
                   '0' => 'IKEA of Sweden', 						-> manufacturer
                   '3' => '1.1.1.1-5.7.2.0', 						-> software version
                   '1' => 'TRADFRI bulb E14 WS opal 400lm', 		-> product name
                   '2' => '' 
                 }, 
          '5750' => 2, 											-> type: bulb?, but no information about the type		
          '3311' => [ 												-> light information
                      { 
                        '5850' => 1, 								-> on/ off
                        '5710' => 24694, 							-> color_y (CIE1931 model, max 65535)
                        '5707' => 0, 								
                        '5851' => 7, 								-> dim value (brightness)
                        '5711' => 0, 								
                        '5709' => 24930, 							-> color_x (CIE1931 model, max 65535)
                        '9003' => 0, 								-> instance id?
                        '5708' => 0, 
                        '5706' => 'f5faf6' 						-> rgb color code
                      } 
                    ], 
          '9001' => 'TRADFRI bulb E14 WS opal 400lm', 				-> user defined name
          '9002' => 1492802359, 									-> paired/ created at
          '9020' => 1492863561, 									-> last seen				
          '9003' => 65539, 										-> device id
          '9054' => 0 												-> OTA update state
        }; 
*/

public class TradfriDevice{
	private GWConnection gateway;
	
	private ArrayList<TradfriDeviceEventListener> updateListeners = new ArrayList<TradfriDeviceEventListener>();
	
	private int deviceID;

	//true if it is a bulb or a light panel
	private Boolean isLightingDevice = false;
	
	private boolean isSubscribed = false;		//set once the method subsribe() is called
	
	private String manufacturer;
	private String type;
	private int dimvalue;
	private int onoff;
	private String name;
	private int reachabilityState;
	private int createdAt;
	private int lastSeenAt;
	private String color;
	private String version;
	
	public TradfriDevice(GWConnection gateway, int deviceID) {
		this.gateway = gateway;
		this.deviceID = deviceID;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + deviceID;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TradfriDevice other = (TradfriDevice) obj;
		if (deviceID != other.deviceID)
			return false;
		return true;
	}
	
	public static int[] getDevices(GWConnection gateway) {
		CoapResponse resp = gateway.get("/15001");
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Get devices failed!");
			return new int[0];
		}else {
			try {
				JSONArray json = new JSONArray(resp.getResponseText());
				
				int[] ids = new int[json.length()];
				for(int i = 0; i < json.length(); i++) {
					ids[i] = json.getInt(i);
				}
				
				return ids;
			}catch(Exception e) {
				System.out.println("Unexpected response: " + e.getMessage());
				return new int[0];
			}
		}
	}
	
	/**
	 * Call this if a new info from path /15001/device-id is available
	 * This could either be the case by a get or a new observed state
	 * @param jsonInfo the new json info
	 * @return true if the parsing was successful, false if not
	 */
	private Boolean newInfoAvailable(String jsonInfo) {
		try {
			JSONObject json = new JSONObject(jsonInfo);
			
			//reachability state
			if(json.has("9019")) {
				this.reachabilityState = json.getInt("9019");
			}
			
			//manufacturer
			if(json.has("3") && json.getJSONObject("3").has("0")) {
				this.manufacturer = json.getJSONObject("3").getString("0");
			}
			
			//software version
			if(json.has("3") && json.getJSONObject("3").has("3")) {
				this.version = json.getJSONObject("3").getString("3");
			}
			
			//product type
			if(json.has("3") && json.getJSONObject("3").has("1")) {
				this.type = json.getJSONObject("3").getString("1");
			}
			
			//onoff
			if(json.has("3311") && json.getJSONArray("3311").length() > 0 && json.getJSONArray("3311").getJSONObject(0).has("5850")) {
				this.onoff = json.getJSONArray("3311").getJSONObject(0).getInt("5850");
				this.isLightingDevice = true;
			}
			
			//dimvalue
			if(json.has("3311") && json.getJSONArray("3311").length() > 0 && json.getJSONArray("3311").getJSONObject(0).has("5851")) {
				this.dimvalue = json.getJSONArray("3311").getJSONObject(0).getInt("5851");
				this.isLightingDevice = true;
			}
			
			//color
			if(json.has("3311") && json.getJSONArray("3311").length() > 0 && json.getJSONArray("3311").getJSONObject(0).has("5706")) {
				this.color = json.getJSONArray("3311").getJSONObject(0).getString("5706");
				this.isLightingDevice = true;
			}
			
			//name
			if(json.has("9001")) {
				this.name = json.getString("9001");
			}
			
			//lastSeenAt
			if(json.has("9020")) {
				this.lastSeenAt = json.getInt("9020");
			}
			
			//createdAt
			if(json.has("9002")) {
				this.createdAt = json.getInt("9002");
			}
			
			//invoke all registered event listeners
			for (TradfriDeviceEventListener eventListener : updateListeners)
	            eventListener.onUpdate(this);
			
			return true;
		}catch(Exception e) {
			System.out.println("Unexpected response: " + e.getMessage()); 
			return false;
		}
	}
	
	public void addEventListener(TradfriDeviceEventListener eventListener) {
		updateListeners.add(eventListener);
	}
	
	public void removeEventListener(TradfriDeviceEventListener eventListener) {
		updateListeners.remove(eventListener);
	}
	
	/**
	 * force a update by a blocking get from the device
	 * @return true if it was successfull, false if not
	 */
	public Boolean update() {
		CoapResponse resp = gateway.get("/15001/" + Integer.toString(this.deviceID));
		System.out.println("Code: " + resp.getCode());
		
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Device update failed!");
			return false;
		}else {
			return newInfoAvailable(resp.getResponseText());
		}
	}

	/**
	 * subscribe to changes of this device
	 * @return true if subscription was successful
	 */
	public Boolean subscribe() {
		if(isSubscribed) {
			return false;
		}
		
		isSubscribed = true;
		
		gateway.observe("/15001/" + Integer.toString(this.deviceID), new CoapHandler() {
			@Override
			public void onLoad(CoapResponse response) {
				if(ResponseCode.isSuccess(response.getCode())) {
					newInfoAvailable(response.getResponseText());
				}else {
					System.err.println("Subscription for device " + deviceID + " failed with code " + response.getCode());
				}
			}
			
			@Override
			public void onError() {
				isSubscribed = false;
				System.err.println("Subscription for device " + Integer.toString(deviceID) + " failed!");
			}
		});
		
		return true;
	}
	
	public Boolean isLightingDevice() {
		return isLightingDevice;
	}
	
	public int getDeviceID() {
		return deviceID;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getType() {
		return type;
	}

	public int getDimvalue() {
		return dimvalue;
	}

	public void setDimvalue(int dimvalue) {
		if(dimvalue < 0)
			dimvalue = 0;
		if(dimvalue > 254)
			dimvalue = 254;
		
		JSONObject json = new JSONObject();
		json.put("3311", new JSONArray().put(0, new JSONObject().put("5851", dimvalue)));
		
		gateway.putJSON("/15001/" + Integer.toString(this.deviceID), json.toString());
		
		this.dimvalue = dimvalue;
	}

	public int getOnoff() {
		return onoff;
	}

	public void setOnoff(int onoff) {
		if(onoff > 1)
			onoff = 1;
		if(onoff < 0)
			onoff = 0;
		
		JSONObject json = new JSONObject();
		json.put("3311", new JSONArray().put(0, new JSONObject().put("5850", onoff)));
		gateway.putJSON("/15001/" + Integer.toString(this.deviceID), json.toString());
		
		this.onoff = onoff;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		JSONObject json = new JSONObject();
		json.put("9001", name);
		
		gateway.putJSON("/15001/" + Integer.toString(this.deviceID), json.toString());
		
		this.name = name;
	}

	public int getReachabilityState() {
		return reachabilityState;
	}

	public int getCreatedAt() {
		return createdAt;
	}

	public int getLastSeenAt() {
		return lastSeenAt;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		if(color.length() != 6) {
			return;
		}
		
		color = color.toLowerCase();
		
		JSONObject json = new JSONObject();
		json.put("3311", new JSONArray().put(0, new JSONObject().put("5706", color)));
		
		gateway.putJSON("/15001/" + Integer.toString(this.deviceID), json.toString());
		
		this.color = color;
	}

	public String getVersion() {
		return version;
	}
	
	public String jsonInfo() {
		JSONObject json = new JSONObject();
		
		json.put("deviceid", this.deviceID);
		json.put("manufacturer", this.manufacturer);
		json.put("type", this.type);
		json.put("dimvalue", this.dimvalue);
		json.put("onoff", this.onoff);
		json.put("name", this.name);
		json.put("reachabilityState", this.reachabilityState);
		json.put("createdAt", this.createdAt);
		json.put("lastSeenAt", this.lastSeenAt);
		json.put("color", this.color);
		json.put("version", this.version);
		
		return json.toString();
	}
}
