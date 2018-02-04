/**
 * 
 */
package net.kappelt.JTradfri.Commands.TCPSocket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import net.kappelt.JTradfri.GWConnection;

/**
 * @author peter
 *
 */
public class TcpServer implements Runnable{
	private int port;
	private GWConnection gateway;
	private InetAddress listenAddress;
	
	public TcpServer(int port, GWConnection gw) {
		this(port, gw, InetAddress.getLoopbackAddress());
	}
	
	public TcpServer(int port, GWConnection gw, InetAddress listenAddress) {
		this.port = port;
		this.gateway = gw;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			//listen to localhost only
			@SuppressWarnings("resource")
			ServerSocket socket = new ServerSocket(port, 0, listenAddress);
			//ServerSocket socket = new ServerSocket(port);
			System.out.println("[TcpServer] Binding of socket @ port " + port	+ " successfull.");
			
			//accept new connections in an endless loop
			//@todo I don't think this is a good idea -> what to do, if someone opens a lot of connections to attack?
			while (true){
				try{
				    //block, until there's a connection request
					Socket clientSocket = socket.accept();
					System.out.println("[TcpServer] Connection at port " + port + " opened (Hash " + clientSocket.hashCode() + ")");
					
					Thread threadHandler = new Thread(new TcpClientHandler(clientSocket, gateway));
					threadHandler.start();
				}catch (IOException e){
					System.out.println("[TcpServer] New connection request failed");
				}
		      
		    }
		} catch (IOException e) {
			System.out.println("[TcpServer] Error while binding socket: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			System.out.println("[TcpServer] Caught exception: " + e.getMessage());
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
