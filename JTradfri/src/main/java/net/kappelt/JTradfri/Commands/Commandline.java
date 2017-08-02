package net.kappelt.JTradfri.Commands;

import java.util.Arrays;

import net.kappelt.JTradfri.GWConnection;
import net.kappelt.JTradfri.Tradfri.TradfriDevice;
import net.kappelt.JTradfri.Tradfri.TradfriGroup;

public class Commandline {
	
	private Commandline() {}
	
	private static GWConnection gateway;
	
	/**
	 * Run the command line parser
	 * @param gw an open GWConnection
	 */
	public static void run(GWConnection gw) {
		System.out.println("JTradfri Commandline");
		System.out.println("Type \"help\" to show available commands");
		
		gateway = gw;
		
		boolean runShell = true;
		while(runShell) {
			String[] input = System.console().readLine().split(" ");
			
			if((input.length < 1) || input[0].equals("")) {
				
			}else if(input[0].equals("help")){
				System.out.printf(	"help									display this help%n" +
									"%n" +	
									"debug:disable							disable debug output%n" +
									"debug:enable							enable debug output%n" +
									"%n" +	
									"device:list							list all available devices%n" +
									"device:update <device-id>				manually read the device's info%n" +
									"device:subscribe <device-id>			subscribe to a device so changes will be recognized automatically%n" +
									"device:info <device-id>				get the buffered device data that is updated by a subscription or a update%n" +
									"device:set <device-id> <attribute>		set a attribute of a device, following attributes are available%n" +
									"										onoff <0/1> : Turn on or off%n" +
									"										color <hex> : 3byte hex string for color%n" +
									"										dimvalue <value> : Dimvalue inbetween 0-254%n" +
									"										name <name> : The name of the device%n" +
									"%n" +	
									"group:list								list all available groups%n" +
									"group:update <group-id>				manually read the group's info%n" +
									"group:subscribe <group-id>				subscribe to a group so changes will be recognized automatically%n" +
									"group:info <group-id>					get the buffered group data that is updated by a subscription or a update%n" +
									"group:set <group-id> <attribute>		set a attribute of a group, following attributes are available%n" +
									"										onoff <0/1> : Turn on or off%n" +
									"										mood <mood-id> : mood%n" +
									"										dimvalue <value> : Dimvalue inbetween 0-254%n" +
									"										name <name> : The name of the device%n" +
									"%n" +
									"exit									quit this program%n");
			}else if(input[0].equals("debug:enable")){
				gateway.debugEnable();
			}else if(input[0].equals("debug:disable")){
				gateway.debugDisable();
			}else if (input[0].equals("device:list")) {
				System.out.println(Arrays.toString(TradfriDevice.getDevices(gateway)));
			}else if (input[0].equals("device:update")) {
				if(input.length < 2) {
					System.out.println("Please give device-id!");
				}else {
					try {
						int deviceID = Integer.parseInt(input[1]);
						if(gateway.device(deviceID).update()) {
							System.out.println("Update of " + Integer.toString(deviceID) + " successful.");
						}else {
							System.err.println("Update of " + Integer.toString(deviceID) + " failed!");
						}
					}catch(Exception e) {
						System.out.println("The given device-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("device:subscribe")) {
				if(input.length < 2) {
					System.out.println("Please give device-id!");
				}else {
					try {
						int deviceID = Integer.parseInt(input[1]);
						gateway.device(deviceID).subscribe();
					}catch(Exception e) {
						System.out.println("The given device-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("device:info")) {
				if(input.length < 2) {
					System.out.println("Please give device-id!");
				}else {
					try {
						int deviceID = Integer.parseInt(input[1]);
						System.out.println(gateway.device(deviceID).jsonInfo());
					}catch(Exception e) {
						System.out.println("The given device-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("device:set")) {
				if(input.length < 4) {
					System.out.println("Please give device-id, attribute and value!");
				}else {
					try {
						int deviceID = Integer.parseInt(input[1]);
						
						if(input[2].equals("onoff")) {
							if(!input[3].equals("1") && !input[3].equals("0")) {
								System.out.println("Value for onoff must be \"1\" or \"0\"");
							}else {
								gateway.device(deviceID).setOnoff(input[3].equals("0") ? 0:1);
							}
						}else if(input[2].equals("dimvalue")) {
							try {
								gateway.device(deviceID).setDimvalue(Integer.parseInt(input[3]));
							}catch(Exception e) {
								System.out.println("Invalid value: " + e.getMessage());
							}
						}else if(input[2].equals("color")) {
							if(input[3].length() != 6) {
								System.out.println("Value for color must be a 6 character hex string");
							}else {
								gateway.device(deviceID).setColor(input[3]);
							}
						}else if(input[2].equals("name")) {
							gateway.device(deviceID).setName(input[3]);
						}else {
							System.out.println("Unknown attribute: " + input[2]);
						}
					}catch(Exception e) {
						System.out.println("The given device-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("group:list")) {
				System.out.println(Arrays.toString(TradfriGroup.getGroups(gateway)));
			}else if (input[0].equals("group:update")) {
				if(input.length < 2) {
					System.out.println("Please give group-id!");
				}else {
					try {
						int groupID = Integer.parseInt(input[1]);
						if(gateway.group(groupID).update()) {
							System.out.println("Update of " + Integer.toString(groupID) + " successful.");
						}else {
							System.err.println("Update of " + Integer.toString(groupID) + " failed!");
						}
					}catch(Exception e) {
						System.out.println("The given group-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("group:subscribe")) {
				if(input.length < 2) {
					System.out.println("Please give group-id!");
				}else {
					try {
						int groupID = Integer.parseInt(input[1]);
						gateway.group(groupID).subscribe();
					}catch(Exception e) {
						System.out.println("The given group-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("group:info")) {
				if(input.length < 2) {
					System.out.println("Please give group-id!");
				}else {
					try {
						int groupID = Integer.parseInt(input[1]);
						System.out.println(gateway.group(groupID).jsonInfo());
					}catch(Exception e) {
						System.out.println("The given group-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("group:set")) {
				if(input.length < 4) {
					System.out.println("Please give group-id, attribute and value!");
				}else {
					try {
						int groupID = Integer.parseInt(input[1]);
						
						if(input[2].equals("onoff")) {
							if(!input[3].equals("1") && !input[3].equals("0")) {
								System.out.println("Value for onoff must be \"1\" or \"0\"");
							}else {
								gateway.group(groupID).setOnoff(input[3].equals("0") ? 0:1);
							}
						}else if(input[2].equals("dimvalue")) {
							try {
								gateway.group(groupID).setDimvalue(Integer.parseInt(input[3]));
							}catch(Exception e) {
								System.out.println("Invalid value: " + e.getMessage());
							}
						}else if(input[2].equals("mood")) {
							try {
								gateway.group(groupID).setMood(Integer.parseInt(input[3]));
							}catch(Exception e) {
								System.out.println("Mood id must be a number: " + e.getMessage());
							}
						}else if(input[2].equals("name")) {
							gateway.group(groupID).setName(input[3]);
						}else {
							System.out.println("Unknown attribute: " + input[2]);
						}
					}catch(Exception e) {
						System.out.println("The given group-id doesn't seem to be valid: " + e.getMessage());
					}
				}
			}else if (input[0].equals("exit")) {
				System.out.println("Bye!");
				System.exit(0);
			}else {
				System.out.println("Unknown command! Type \"help\" to get a list of available commands");
			}
		}
	}
}
