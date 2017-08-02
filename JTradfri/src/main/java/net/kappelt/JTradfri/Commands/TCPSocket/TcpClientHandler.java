/**
 * 
 */
package net.kappelt.JTradfri.Commands.TCPSocket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import net.kappelt.JTradfri.GWConnection;
import net.kappelt.JTradfri.Tradfri.TradfriDevice;
import net.kappelt.JTradfri.Tradfri.TradfriDeviceEventListener;
import net.kappelt.JTradfri.Tradfri.TradfriGroup;
import net.kappelt.JTradfri.Tradfri.TradfriGroupEventListener;

/**
 * @author peter
 *
 */
public class TcpClientHandler implements Runnable {

	/**
	 * Devices and groups that are subscribed by the client on this connection
	 */
	private ArrayList<TradfriDevice> subscribedDevices = new ArrayList<TradfriDevice>();
	private ArrayList<TradfriGroup> subscribedGroups = new ArrayList<TradfriGroup>();
	
	private Socket clientSocket;
	private GWConnection gateway;
	
	private PrintWriter outData;		//used to write data
	
	public TcpClientHandler(Socket clientSocket, GWConnection gw) {
		this.clientSocket = clientSocket;
		this.gateway = gw;
	}
	
	/**
	 * @return String in format "[TcpClientHandler-<HASH>] "
	 */
	private String threadIdentification() {
		return "[TcpClientHandler-" + clientSocket.hashCode() + "] ";
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * see tcp-protocol.md for description of commands
	 */
	@Override
	public void run() {
		BufferedReader inData;		//used to read commands from the socket
		
		String commandLine = null;	//received line	
		Boolean executeLoop = true; //this is used to stop the thread, once the connection is broken
		
		try{
			// read data and write data
			inData = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));			
			outData = new PrintWriter(clientSocket.getOutputStream(), true);
		}catch(Exception e){
			System.err.println(threadIdentification() + "Error in client socket, ending this socket: " + e.getMessage());
			e.printStackTrace();
			return;
		}
	
		while(executeLoop){
			try {
				commandLine = inData.readLine();
			}catch(Exception e) {
				System.err.println(threadIdentification() + "Exception while receiving command, ending this socket: " + e.getMessage());
				e.printStackTrace();
				return;
			}
			
			if((commandLine == null) || clientSocket.isClosed()){
				//the received dataline was "null" -> probably the remote side closed the socket
				//additionally, the socket could be closed on the server side, though that isn't implemented here
				executeLoop = false;
				System.out.println(threadIdentification() + "Client socket was probably closed by remote!");
				
				//de-register all event listeners 
				for(TradfriDevice device: subscribedDevices) {
					device.removeEventListener(deviceEventListener);
				}
				
				for(TradfriGroup group: subscribedGroups) {
					group.removeEventListener(groupEventListener);
				}
				
				//end this thread
				return;
			}
			
			if(commandLine != null){
				commandLine = commandLine.replace("\n", "").replace("\r", "");		//remove newlines, like at the end
				
				System.out.println(threadIdentification() + "Received command: " + commandLine);
				
				String[] command = commandLine.split("::");						//split the command, separated by "::"
				
				if(command[0].equals("device")) {
					handleDeviceCommand(command);
				}else if(command[0].equals("group")) {
					handleGroupCommand(command);
				}else {
					System.out.println(threadIdentification() + "Unknown command!");
				}
				
				System.out.println();
			}

		}
	}

	//event listener for changes
	TradfriDeviceEventListener deviceEventListener = new TradfriDeviceEventListener() {
		
		@Override
		public void onUpdate(TradfriDevice updatedDevice) {
			outData.println("subscribedDeviceUpdate::" + updatedDevice.getDeviceID() + "::" + updatedDevice.jsonInfo());
		}
	};
	
	private void handleDeviceCommand(String[] command) {
		if(command.length < 2) {
			System.out.println(threadIdentification() + "No action specified!");
			return;
		}
		
		if(command[1].equals("list")) {
			//command "device::list" -> return JSON of all device ids and their name
			String deviceJSON = TradfriDevice.getDevices(gateway);
			
			outData.println("deviceList::" + deviceJSON);
			System.out.println(threadIdentification() + "Send device-list: " + deviceJSON);
		}else if(command[1].equals("update")) {
			//command "device::update::<device-id>" -> perform a device update
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No device-id specified!");
				return;
			}
			
			int deviceID;
			try {
				deviceID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The device-id is not a number: " + e.getMessage());
				return;
			}
			
			gateway.device(deviceID).update();
			System.out.println(threadIdentification() + "Performed update for device " + deviceID);
		}else if(command[1].equals("subscribe")) {
			//command "device::subscribe::<device-id>" -> subscribe to device updates
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No device-id specified!");
				return;
			}
			
			int deviceID;
			try {
				deviceID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The device-id is not a number: " + e.getMessage());
				return;
			}
			
			gateway.device(deviceID).subscribe();
			gateway.device(deviceID).addEventListener(deviceEventListener);
			subscribedDevices.add(gateway.device(deviceID));
			
			System.out.println(threadIdentification() + "Subscribed to changes for device " + deviceID);
		}else if(command[1].equals("info")) {
			//command "device::info::<device-id>" -> get the buffered device info
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No device-id specified!");
				return;
			}
			
			int deviceID;
			try {
				deviceID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The device-id is not a number: " + e.getMessage());
				return;
			}
			
			outData.println("deviceInfo::" + deviceID + "::" + gateway.device(deviceID).jsonInfo());
			System.out.println(threadIdentification() + "Sent info for " + deviceID);
		}else if(command[1].equals("set")) {
			//command "device::set::<device-id>::..." -> set something for the device
			if(command.length < 5) {
				System.out.println(threadIdentification() + "device-id, attribute or value not specified!");
				return;
			}
			
			int deviceID;
			try {
				deviceID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The device-id is not a number: " + e.getMessage());
				return;
			}
			
			if(command[3].equals("onoff")) {
				int onoff;
				if(command[4].equals("0")) {
					onoff = 0;
				}else if(command[4].equals("1")) {
					onoff = 1;
				}else {
					System.out.println(threadIdentification() + "Value must be one or zero!");
					return;
				}
				
				gateway.device(deviceID).setOnoff(onoff);
			}else if(command[3].equals("dimvalue")) {
				int dimvalue;
				try {
					dimvalue = Integer.parseInt(command[4]);
				}catch(Exception e) {
					System.out.println(threadIdentification() + "dimvalue is not a number: " + e.getMessage());
					return;
				}
				
				gateway.device(deviceID).setDimvalue(dimvalue);
			}else if(command[3].equals("color")) {
				if(command[4].length() != 6) {
					System.out.println("Color must be a 6 digit hex string");
					return;
				}
				
				gateway.device(deviceID).setColor(command[4]);
			}else if(command[3].equals("name")) {
				gateway.device(deviceID).setName(command[4]);
			}else {
				System.out.println(threadIdentification() + "Unknown attribute " + command[3]);
				return;
			}
			
			System.out.println(threadIdentification() + "Successful set for " + deviceID);
		}else {
			System.out.println(threadIdentification() + "Action unknown!");
		}
	}

	//event listener for changes
	TradfriGroupEventListener groupEventListener = new TradfriGroupEventListener() {
		
		@Override
		public void onUpdate(TradfriGroup updatedGroup) {
			outData.println("subscribedGroupUpdate::" + updatedGroup.getGroupID() + "::" + updatedGroup.jsonInfo());
		}
	};
	
	private void handleGroupCommand(String[] command) {
		if(command.length < 2) {
			System.out.println(threadIdentification() + "No action specified!");
			return;
		}
		
		if(command[1].equals("list")) {
			//command "group::list" -> return an array of all groups
			String groupJSON = TradfriGroup.getGroups(gateway);
			
			outData.println("groupList::" + groupJSON);
			System.out.println(threadIdentification() + "Send group-list:  " + groupJSON);
		}else if(command[1].equals("update")) {
			//command "group::update::<group-id>" -> perform a group update
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No group-id specified!");
				return;
			}
			
			int groupID;
			try {
				groupID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The group-id is not a number: " + e.getMessage());
				return;
			}
			
			gateway.group(groupID).update();
			System.out.println(threadIdentification() + "Performed update for group " + groupID);
		}else if(command[1].equals("subscribe")) {
			//command "group::subscribe::<group-id>" -> subscribe to group updates
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No group-id specified!");
				return;
			}
			
			int groupID;
			try {
				groupID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The group-id is not a number: " + e.getMessage());
				return;
			}
			
			gateway.group(groupID).subscribe();
			gateway.group(groupID).addEventListener(groupEventListener);
			subscribedGroups.add(gateway.group(groupID));
			
			System.out.println(threadIdentification() + "Subscribed to changes for group " + groupID);
		}else if(command[1].equals("info")) {
			//command "group::info::<group-id>" -> get the buffered group info
			if(command.length < 3) {
				System.out.println(threadIdentification() + "No group-id specified!");
				return;
			}
			
			int groupID;
			try {
				groupID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The group-id is not a number: " + e.getMessage());
				return;
			}
			
			outData.println("groupInfo::" + groupID + "::" + gateway.group(groupID).jsonInfo());
			System.out.println(threadIdentification() + "Sent info for " + groupID);
		}else if(command[1].equals("set")) {
			//command "group::set::<group-id>::..." -> set something for the group
			if(command.length < 5) {
				System.out.println(threadIdentification() + "group-id, attribute or value not specified!");
				return;
			}
			
			int groupID;
			try {
				groupID = Integer.parseInt(command[2]);
			}catch(Exception e) {
				System.out.println(threadIdentification() + "The group-id is not a number: " + e.getMessage());
				return;
			}
			
			if(command[3].equals("onoff")) {
				int onoff;
				if(command[4].equals("0")) {
					onoff = 0;
				}else if(command[4].equals("1")) {
					onoff = 1;
				}else {
					System.out.println(threadIdentification() + "Value must be one or zero!");
					return;
				}
				
				gateway.group(groupID).setOnoff(onoff);
			}else if(command[3].equals("dimvalue")) {
				int dimvalue;
				try {
					dimvalue = Integer.parseInt(command[4]);
				}catch(Exception e) {
					System.out.println(threadIdentification() + "dimvalue is not a number: " + e.getMessage());
					return;
				}
				
				gateway.group(groupID).setDimvalue(dimvalue);
			}else if(command[3].equals("mood")) {
				int moodID;
				try {
					moodID = Integer.parseInt(command[4]);
				}catch(Exception e) {
					System.out.println(threadIdentification() + "moodID is not a number: " + e.getMessage());
					return;
				}
				
				gateway.group(groupID).setMood(moodID);
			}else if(command[3].equals("name")) {
				gateway.group(groupID).setName(command[4]);
			}else {
				System.out.println(threadIdentification() + "Unknown attribute " + command[3]);
				return;
			}
			
			System.out.println(threadIdentification() + "Successful set for " + groupID);
		}else {
			System.out.println(threadIdentification() + "Action unknown!");
		}
	}
}
