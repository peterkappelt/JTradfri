/**
 * 
 */

package net.kappelt.JTradfri;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import net.kappelt.JTradfri.Commands.Commandline;
import net.kappelt.JTradfri.Commands.TCPSocket.TcpServer;

/**
 * @author peter
 *
 */
public class JTradfri {	
	@Parameter(names = {"-h", "--help"}, description = "Show help information", help = true)
	private boolean help;

	@Parameter(names = {"-s", "--secret"}, description = "PSK that is used to connect to the gateway. Required, if no configfile is specified")
	private String coapPSK;
	
	@Parameter(names = {"-g", "--gateway"}, description = "IP or DNS name of the gateway to connect to. Required, if no configfile is specified")
	private String coapAddress;
	
	@Parameter(names = {"-p", "--port"}, description = "Port for the TCP server, default 1505")
	private int tcpPort = 1505;
	
	@Parameter(names = {"-l", "--listen-address"}, description = "IP of the Interface that shall be listened to. Necessary, if you're running the TCP-Client (e.g. FHEM) on another machine")
	private String listenAddress = "127.0.0.1";
	
	@Parameter(names = {"-u", "--udpport"}, description = "Port for the UDP server, default to random port")
	private int udpPort = 0;
	
	@Parameter(names = {"-n", "--no-commandline"}, description = "Disable interactive input from command line, e.g. for SystemD-Service")
	private Boolean noCmdLine = false;
	
	@Parameter(names = {"-c", "--config-file"}, description = "Path to a config file")
	private String configFilePath;
	
	@Parameter(names = {"-d", "--debug"}, description = "Enable debug output")
	private Boolean debug = false;
	

	public static void main(String[] args) {
		JTradfri main = new JTradfri();
		
		JCommander cmdLine = JCommander.newBuilder().addObject(main).build();	
		try{
			cmdLine.parse(args);
		}catch(Exception e){
			System.err.println("Parameter error: " + e.getMessage());
			cmdLine.usage();
			System.exit(-1);
		}
		
		System.out.println("JTradfri 0.0.6-snapshot");
		System.out.println();
		
		main.start(cmdLine);
	}
	
	public void start(JCommander cmdLine){
		//user has requested help to show
		if(help == true) {
			cmdLine.usage();
			System.exit(-1);
		}
				
		//a config file was specified
		if(configFilePath != null) {
			InputStream input;
			Properties prop = new Properties();
			try {
				input = new FileInputStream(configFilePath);
				prop.load(input);
			}catch(Exception e) {
				System.out.println("Error while reading config file: " + e.getMessage());
				System.out.print("Aborting...");
				System.exit(-1);
			}
			
			if(prop.containsKey("gateway")) {
				this.coapAddress = prop.getProperty("gateway");
			}
			if(prop.containsKey("secret")) {
				this.coapPSK = prop.getProperty("secret");
			}
			if(prop.containsKey("port")) {
				try {
					this.tcpPort = Integer.parseInt(prop.getProperty("port"));
				}catch(Exception e) {
					System.out.println("Your value of the parameter \"port\" in the config is invalid: " + e.getMessage());
					System.out.print("Aborting...");
					System.exit(-1);
				}
			}
			if(prop.containsKey("udpport")) {
				try {
					this.udpPort = Integer.parseInt(prop.getProperty("udpport"));
				}catch(Exception e) {
					System.out.println("Your value of the parameter \"udpport\" in the config is invalid: " + e.getMessage());
					System.out.print("Aborting...");
					System.exit(-1);
				}
			}
			if(prop.containsKey("listenaddress")) {
				this.listenAddress = prop.getProperty("listenaddress");
			}
			if(prop.containsKey("nocmdline")) {
				this.noCmdLine = prop.getProperty("nocmdline").equals("true") ? true:false;
			}
		}
		
		if(coapPSK == null) {
			System.out.println("Please specify the Gateway PSK, either with the command line parameter -s or in the config file");
			System.out.print("Aborting...");
			System.exit(-1);
		}
		if(coapAddress == null) {
			System.out.println("Please specify the Gateway Address, either with the command line parameter -g or in the config file");
			System.out.print("Aborting...");
			System.exit(-1);
		}
		
		//notify the user if the PSK seems to be invalid
		if(coapPSK.length() != 16){
			System.out.println("Warning: Your set PSK \"" + coapPSK + "\" doesn't seem to be a correct Tradfri-PSK. This could be a false alarm.");
			System.out.println();
		}
		
		final GWConnection gateway = new GWConnection(coapAddress, coapPSK, udpPort);

		//Parse the InetAddress from the parameter -l
		InetAddress listenInet = InetAddress.getLoopbackAddress();
		try {
			listenInet = InetAddress.getByName(listenAddress);
		}catch(Exception e) {
			System.out.println("Faulty listen-address for parameter -l: " + e.getMessage());
			System.exit(-1);
		}
		System.out.println("TCP-Listener on " + listenInet.toString());
		
		Thread tcpServer = new Thread(new TcpServer(tcpPort, gateway, listenInet));
		tcpServer.start();
		
		if(!noCmdLine) {
			Commandline.run(gateway);
		}
	}

}
