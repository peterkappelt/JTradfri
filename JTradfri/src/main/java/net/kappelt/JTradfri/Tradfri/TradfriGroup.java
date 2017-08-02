/**
 * 
 */
package net.kappelt.JTradfri.Tradfri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.json.JSONArray;
import org.json.JSONObject;

import net.kappelt.JTradfri.GWConnection;

/*
 The output of the path PATH_GROUP_ROOT/GROUP_ADDRESS looks like follows (for bulbs, that can not change color)
$VAR1 = {
          '9003' => 193768,						-> id
          '9018' => {								-> "HS_ACCESSORY_LINK"
                      '15002' => {
                                   '9003' => [
                                               65536,
                                               65537,			-> sub-devices, contained in group
                                               65538
                                             ]
                                 }
                    },
          '5851' => 0,								-> dimming value
          '9039' => 199947,						-> mood id
          '5850' => 1,								-> on/off
          '9002' => 1492280898,					-> created at
          '9001' => 'TRADFRI group'				-> name
        };

 Moods are defined per Group
 An array of the mood ids of the group can be accessed under PATH_MOODS_ROOT/GROUP_ADDRESS

 the individual mood info is accessed under PATH_MOODS_ROOT/GROUP_ADDRESS/MOOD_ID
 {
    "9001":"FOCUS",												-> user name
    "9002":1494088485,												-> created at?
    "9003":206399,													-> mood id
    "9057":2,														
    "9068":1,														-> 1 means that mood is predefined by IKEA ?
    "15013":[														-> configs for individual member devices
       {
          "5850":1,												-> on/ off
          "5851":254,												-> dimvalue
          "9003":65537												-> member id
       },
       {
          "5850":1,
          "5851":254,
          "9003":65538
       }
    ]
 }
*/

/**
 * @author peter
 * @todo brightness gets updated even if not subscribed
 */
public class TradfriGroup {
	private GWConnection gateway;
	
	private ArrayList<TradfriGroupEventListener> updateListeners = new ArrayList<TradfriGroupEventListener>();
	
	private int groupID;
	
	private boolean isSubscribed = false;		//set once the method subsribe() is called
	
	private int mood;
	private int createdAt;
	private String name;
	private int onoff;
	private int dimvalue;
	
	Map<Integer, TradfriDevice> memberDevices = new HashMap<Integer, TradfriDevice>();
	
	public TradfriGroup(GWConnection gateway, int groupID) {
		this.gateway = gateway;
		this.groupID = groupID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gateway == null) ? 0 : gateway.hashCode());
		result = prime * result + groupID;
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
		TradfriGroup other = (TradfriGroup) obj;
		if (gateway == null) {
			if (other.gateway != null)
				return false;
		} else if (!gateway.equals(other.gateway))
			return false;
		if (groupID != other.groupID)
			return false;
		return true;
	}
	
	public static String getGroups(GWConnection gateway) {
		CoapResponse resp = gateway.get("/15004");
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Get groups failed!");
			return "";
		}else {
			try {
				JSONArray groupIDs = new JSONArray(resp.getResponseText());
				JSONArray returnJSON = new JSONArray();
				
				for(int i = 0; i < groupIDs.length(); i++) {
					int currentID = groupIDs.getInt(i);

					gateway.group(currentID).update();
					
					JSONObject currentKey = new JSONObject();
					currentKey.put("groupid", gateway.group(currentID).getGroupID());
					currentKey.put("name", gateway.group(currentID).getName());
					
					returnJSON.put(currentKey);
				}
				
				return returnJSON.toString();
			}catch(Exception e) {
				System.out.println("Unexpected response: " + e.getMessage());
				return "";
			}
		}
	}
	
	/**
	 * Call this if a new info from path /15004/group-id is available
	 * This could either be the case by a get or a new observed state
	 * @param jsonInfo the new json info
	 * @return true if the parsing was successful, false if not
	 */
	private Boolean newInfoAvailable(String jsonInfo) {
		try {
			JSONObject json = new JSONObject(jsonInfo);
			
			//name
			if(json.has("9001")) {
				this.name = json.getString("9001");
			}
			
			//mood
			if(json.has("9039")) {
				this.mood = json.getInt("9039");
			}
			
			//createdAt
			if(json.has("9002")) {
				this.createdAt = json.getInt("9002");
			}
			
			if(json.has("9018") && json.getJSONObject("9018").has("15002") && json.getJSONObject("9018").getJSONObject("15002").has("9003")) {
				JSONArray rawMemberDevices = json.getJSONObject("9018").getJSONObject("15002").getJSONArray("9003");
				
				List<Integer> newDevices = new ArrayList<Integer>();
				
				for(int i = 0; i < rawMemberDevices.length(); i++) {
					int deviceID = rawMemberDevices.getInt(i);
					
					newDevices.add(deviceID);
					
					if(!memberDevices.containsKey(deviceID)) {					
						memberDevices.put(deviceID, gateway.device(deviceID));
						
						gateway.device(deviceID).addEventListener(memberDeviceListener);
						gateway.device(deviceID).subscribe();
					}
				}
				
				//check for every known device if it is a new device
				//delete it if it isn't known in the updated data
				for(Map.Entry<Integer, TradfriDevice> entry : memberDevices.entrySet()) {
				    int deviceID = entry.getKey();
				    TradfriDevice device = entry.getValue();
				    //the device was deleted from this group
				    if(!newDevices.contains(deviceID)) {
				    	device.removeEventListener(memberDeviceListener);
				    	memberDevices.remove(deviceID);
				    }
				}
			}
			
			//invoke all registered event listeners
			for (TradfriGroupEventListener eventListener : updateListeners)
	            eventListener.onUpdate(this);
			
			return true;
		}catch(Exception e) {
			System.out.println("Unexpected response: " + e.getMessage()); 
			return false;
		}
	}
	
	public void addEventListener(TradfriGroupEventListener eventListener) {
		updateListeners.add(eventListener);
	}
	
	public void removeEventListener(TradfriGroupEventListener eventListener) {
		updateListeners.remove(eventListener);
	}
	
	private void memberDeviceUpdated() {
		/**
		 * a member of this group was updated
		 * now, follow those rules to calculate Onoff and dimvalue for the group:
		 *  - if every member is off the group is off and the group brightness is the average of the member brightnesses
		 *  - if one ore more device is on the group is on and the group brightness is the average of the on-device's brightnesses 
		 */
		int onDeviceCount = 0;		//number of devices that are on
		int deviceCount = 0;		//number of devices that are members
		int brightnessSum = 0;		//sum of the brightnesses of all members
		int onBrightnessSum = 0;	//sum of the brightnesses of members that are turned on
		
		//check for every known device if it is a new device
		//delete it if it isn't known in the updated data
		for(Map.Entry<Integer, TradfriDevice> entry : memberDevices.entrySet()) {
		    TradfriDevice device = entry.getValue();

		    if(device.isLightingDevice()) {
		    	deviceCount++;
		    	brightnessSum += device.getDimvalue();
		    	
		    	if(device.getOnoff() > 0) {
		    		onDeviceCount++;
		    		onBrightnessSum += device.getDimvalue();
		    	}
		    }
		}
		
		if(onDeviceCount > 0) {
			this.onoff = 1;
			this.dimvalue = onBrightnessSum / onDeviceCount;
		}else {
			this.onoff = 0;
			if(deviceCount > 0) {
				this.dimvalue = brightnessSum / deviceCount;
			}else {
				this.dimvalue = 0;
			}
		}
		
		//invoke all registered event listeners
		for (TradfriGroupEventListener eventListener : updateListeners)
            eventListener.onUpdate(this);
	}
	
	/**
	 * a member device calles this if it was updated
	 * invoke the brightness calculation algorithm
	 */
	private TradfriDeviceEventListener memberDeviceListener = new TradfriDeviceEventListener() {
		@Override
		public void onUpdate(TradfriDevice updatedDevice) {
			memberDeviceUpdated();
		}
	};
	
	/**
	 * force a update by a blocking get from the group
	 * @return true if it was successful, false if not
	 */
	public Boolean update() {
		CoapResponse resp = gateway.get("/15004/" + Integer.toString(this.groupID));
		
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Group update for " + this.groupID + "failed!");
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
		
		gateway.observe("/15004/" + Integer.toString(this.groupID), new CoapHandler() {
			@Override
			public void onLoad(CoapResponse response) {
				if(ResponseCode.isSuccess(response.getCode())) {
					newInfoAvailable(response.getResponseText());
				}else {
					System.err.println("Subscription for group " + groupID + " failed with code " + response.getCode());
				}
			}
			
			@Override
			public void onError() {
				isSubscribed = false;
				System.err.println("Subscription for group " + Integer.toString(groupID) + " failed!");
			}
		});
		
		return true;
	}
	
	/**
	 * @return the mood
	 */
	public int getMood() {
		return mood;
	}

	/**
	 * @param mood the mood to set
	 */
	public void setMood(int mood) {		
		JSONObject json = new JSONObject();
		json.put("9039", mood);
		
		gateway.putJSON("/15004/" + Integer.toString(this.groupID), json.toString());
		
		this.mood = mood;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		JSONObject json = new JSONObject();
		json.put("9001", name);
		
		gateway.putJSON("/15004/" + Integer.toString(this.groupID), json.toString());
		
		this.name = name;
	}

	/**
	 * @return the onoff
	 */
	public int getOnoff() {
		return onoff;
	}

	/**
	 * @param onoff the onoff to set
	 */
	public void setOnoff(int onoff) {
		if(onoff > 1)
			onoff = 1;
		if(onoff < 0)
			onoff = 0;
		
		JSONObject json = new JSONObject();
		json.put("5850", onoff);
		gateway.putJSON("/15004/" + Integer.toString(this.groupID), json.toString());
		
		this.onoff = onoff;
	}

	/**
	 * @return the dimvalue
	 */
	public int getDimvalue() {
		return dimvalue;
	}

	/**
	 * @param dimvalue the dimvalue to set
	 */
	public void setDimvalue(int dimvalue) {
		if(dimvalue < 0)
			dimvalue = 0;
		if(dimvalue > 254)
			dimvalue = 254;
		
		JSONObject json = new JSONObject();
		json.put("5851", dimvalue);
		
		gateway.putJSON("/15004/" + Integer.toString(this.groupID), json.toString());
		
		this.dimvalue = dimvalue;
	}

	/**
	 * @return the groupID
	 */
	public int getGroupID() {
		return groupID;
	}

	/**
	 * @return the createdAt
	 */
	public int getCreatedAt() {
		return createdAt;
	}

	public String jsonInfo() {
		JSONObject json = new JSONObject();
		
		json.put("groupid", this.groupID);
		json.put("name", this.name);
		json.put("createdAt", this.createdAt);
		json.put("mood", this.mood);
		json.put("onoff", this.onoff);
		json.put("dimvalue", this.dimvalue);
		
		JSONArray devices = new JSONArray();
		for(Map.Entry<Integer, TradfriDevice> entry : memberDevices.entrySet()) {
		    TradfriDevice memberDevice = entry.getValue();
		    
		    JSONObject deviceJSON = new JSONObject();
		    
		    deviceJSON.put("deviceid", memberDevice.getDeviceID());
		    deviceJSON.put("name", memberDevice.getName());
		    
		    devices.put(deviceJSON);
		}
		json.put("members", devices);
		
		return json.toString();
	}
}
