/**
 * 
 */

package net.kappelt.JTradfri;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import net.kappelt.JTradfri.Commands.Commandline;
import net.kappelt.JTradfri.Commands.TCPSocket.TcpServer;

/**
 * @author peter
 *
 */
public class JTradfri {	
	@Parameter(names = {"-s", "--secret"}, description = "PSK that is used to connect to the gateway", required=true, help=true)
	private String coapPSK;
	
	@Parameter(names = {"-g", "--gateway"}, description = "IP or DNS name of the gateway to connect to, without leading or trailing slash", required=true, help=true)
	private String coapAddress;
	
	@Parameter(names = {"-p", "--port"}, description = "Port for the TCP server, default 1505", help=true)
	private int tcpPort = 1505;
	
	@Parameter(names = {"-d", "--debug"}, description = "Enable debug output", help = true)
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
		
		System.out.println("JTradfri 0.0.3-snapshot");
		System.out.println();
		
		main.startThreadHandler();
	}
	
	public void startThreadHandler(){
		if(coapPSK.length() != 16){
			System.out.println("Warning: Your set PSK \"" + coapPSK + "\" doesn't seem to be a correct Tradfri-PSK. This could be a false alarm.");
			System.out.println();
		}
		
		final GWConnection gateway = new GWConnection(coapAddress, coapPSK);

		Thread tcpServer = new Thread(new TcpServer(tcpPort, gateway));
		tcpServer.start();
		
		Commandline.run(gateway);
	}

}
