/**
 * 
 */
package net.kappelt.JTradfri.Tradfri;

import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.json.JSONArray;
import org.json.JSONObject;

import net.kappelt.JTradfri.GWConnection;

/*
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
 *
 */
public class TradfriMood {
	private GWConnection gateway;

	private int moodID;
	private TradfriGroup group;
	private int createdAt;
	private String name;
	
	public TradfriMood(GWConnection gateway, TradfriGroup group, int moodID) {
		this.gateway = gateway;
		this.group = group;
		this.moodID = moodID;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + moodID;
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
		TradfriMood other = (TradfriMood) obj;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (moodID != other.moodID)
			return false;
		return true;
	}



	public static String getMoods(GWConnection gateway, TradfriGroup group) {
		CoapResponse resp = gateway.get("/15005/" + group.getGroupID());
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Get moods failed!");
			return "";
		}else {
			try {
				JSONArray moodIDs = new JSONArray(resp.getResponseText());
				JSONArray returnJSON = new JSONArray();
				
				for(int i = 0; i < moodIDs.length(); i++) {
					int currentID = moodIDs.getInt(i);

					group.mood(currentID).update();
					
					JSONObject currentKey = new JSONObject();
					currentKey.put("groupid", group.mood(currentID).getGroupID());
					currentKey.put("moodid", group.mood(currentID).getMoodID());
					currentKey.put("name", group.mood(currentID).getName());
					
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
	 * force a update by a blocking get
	 * @return true if it was successful, false if not
	 */
	public Boolean update() {
		CoapResponse resp = gateway.get("/15005/" + this.group.getGroupID() + "/" + this.moodID);
		
		if(!ResponseCode.isSuccess(resp.getCode())) {
			System.out.println("Mood update for " + this.moodID + "failed!");
			return false;
		}else {
			try {
				JSONObject json = new JSONObject(resp.getResponseText());
				
				//name
				if(json.has("9001")) {
					this.name = json.getString("9001");
				}
				
				//createdAt
				if(json.has("9002")) {
					this.createdAt = json.getInt("9002");
				}
				
				return true;
			}catch(Exception e) {
				System.out.println("Unexpected response: " + e.getMessage()); 
				return false;
			}
		}
	}
	
	/**
	 * @return the moodID
	 */
	public int getMoodID() {
		return moodID;
	}

	/**
	 * @return the groupID
	 */
	public int getGroupID() {
		return group.getGroupID();
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the createdAt
	 */
	public int getCreatedAt() {
		return createdAt;
	}
	
	/**
	 * Update the device and return the JSON info
	 * @return JSON info
	 */
	public String jsonInfo() {
		update();
		
		JSONObject json = new JSONObject();
		
		json.put("groupid", this.group.getGroupID());
		json.put("moodid", this.moodID);
		json.put("name", this.name);
		json.put("createdAt", this.createdAt);
		
		return json.toString();
	}
}
