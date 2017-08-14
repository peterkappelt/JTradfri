/**
 * 
 */
package net.kappelt.JTradfri;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.californium.core.CaliforniumLogger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.ScandiumLogger;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.pskstore.StaticPskStore;

import net.kappelt.JTradfri.Tradfri.TradfriDevice;
import net.kappelt.JTradfri.Tradfri.TradfriGroup;

/**
 * @author peter
 *
 */
public class GWConnection {	
	
	/**
	 * devices that are user-added
	 */
	Map<Integer, TradfriDevice> devices = new HashMap<Integer, TradfriDevice>();
	/**
	 * groups that are user-added
	 */
	Map<Integer, TradfriGroup> groups = new HashMap<Integer, TradfriGroup>();
	
	/**
	 * Fixed variables for trust key stores
	 */
	private static final String TRUST_STORE_PASSWORD = "rootPass";
	private static final String KEY_STORE_PASSWORD = "endPass";
	private static final String KEY_STORE_LOCATION = "certs/keyStore.jks";
	private static final String TRUST_STORE_LOCATION = "certs/trustStore.jks";
	
	/**
	 * The IP or DNS name of the gateway
	 */
	private String gatewayIP = "";
	/**
	 * The secret on the label of the gateway
	 */
	private String gatewaySecret = "";
	
	/**
	 * The UDP port for notifies
	 */
	private Integer udpPort = 0;
	
	/**
	 * DTLS connector, in order to provide security features to CoAP-Classes
	 */
	private DTLSConnector dtlsConnector = null;
	
	/**
	 * The Californium CoAP-Client-Instance itself
	 */
	private CoapClient client;
	
	/**
	 * Creates a new instance of the gateway connection
	 */
	public GWConnection() {
		
	}
	
	/**
	 * creates a new instance of the gateway connection and opens it 
	 * a fail while opening leads to abortion of the program
	 * @param gatewayIP	IP or DNS of the gateway
	 * @param gatewaySecret Secret, is on the bottom label on the gateway
	 * @param udpPort the port for the UDP notifys
	 */
	public GWConnection(String gatewayIP, String gatewaySecret, Integer udpPort) {		
		connectionOpen(gatewayIP, gatewaySecret, udpPort);
	}
	
	/**
	 * init the connection to the gateway
	 * @param gatewayIP IP or DNS of the gateway
	 * @param gatewaySecret Secret, is on the bottom label on the gateway
	 * @param configFile path to a Properties-File that is writeable, can be null (than a new file in the current directory will be created)
	 */
	public void connectionOpen(String gatewayIP, String gatewaySecret, Integer udpPort) {
		this.gatewayIP = gatewayIP;
		this.gatewaySecret = gatewaySecret;
		this.udpPort = udpPort;
		
		try {
			// load key store
			KeyStore keyStore = KeyStore.getInstance("JKS");
			InputStream in = getClass().getClassLoader().getResourceAsStream(KEY_STORE_LOCATION);
			keyStore.load(in, KEY_STORE_PASSWORD.toCharArray());
			in.close();

			// load trust store
			KeyStore trustStore = KeyStore.getInstance("JKS");
			in = getClass().getClassLoader().getResourceAsStream(TRUST_STORE_LOCATION);
			trustStore.load(in, TRUST_STORE_PASSWORD.toCharArray());
			in.close();

			// You can load multiple certificates if needed
			Certificate[] trustedCertificates = new Certificate[1];
			trustedCertificates[0] = trustStore.getCertificate("root");

			DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder();
			builder.setAddress(new InetSocketAddress(this.udpPort));
			builder.setPskStore(new StaticPskStore("Client_identity", this.gatewaySecret.getBytes()));
			builder.setIdentity((PrivateKey)keyStore.getKey("client", KEY_STORE_PASSWORD.toCharArray()),
					keyStore.getCertificateChain("client"), true);
			builder.setTrustStore(trustedCertificates);
			
			//try to fix timeouts at user
			builder.setRetransmissionTimeout(50000);
			
			dtlsConnector = new DTLSConnector(builder.build());
			
		} catch (Exception e) {
			System.err.println("[GWConnection] Error while initializing key store: ");
			e.printStackTrace();
			System.exit(-1);
		}
		
		//custom network config without a config file
		NetworkConfig networkConfig = NetworkConfig.createStandardWithoutFile();
		networkConfig.set(Keys.ACK_TIMEOUT, 40000);
		networkConfig.setInt(Keys.MAX_RESOURCE_BODY_SIZE, 8192);
		NetworkConfig.setStandard(networkConfig);
		
		client = new CoapClient();
		
		client.setEndpoint(new CoapEndpoint(dtlsConnector, networkConfig));
		client.setTimeout(60000);
		
		//client.setEndpoint(new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard()));
		
		//after opening connection: fetch well known
		System.out.println("[GWConnection] Fetching well-known...");
		System.out.println("[GWConnection] " + this.get("/.well-known/core").getResponseText());
	}
	
	/**
	 * Enable debug output
	 */
	public void debugEnable() {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.FINER);
		
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.FINER);
	}
	/**
	 * Disable debug output
	 */
	public void debugDisable() {
		CaliforniumLogger.initialize();
		CaliforniumLogger.setLevel(Level.OFF);
		
		ScandiumLogger.initialize();
		ScandiumLogger.setLevel(Level.OFF);
	}
	
	/**
	 * get the device-class by id
	 * @param deviceID
	 * @return a TradfriDevice instance
	 */
	public TradfriDevice device(int deviceID) {
		//put a new TradfriDevice to the devices-map if it doesn't exist yet
		if(!devices.containsKey(deviceID)) {
			devices.put(deviceID, new TradfriDevice(this, deviceID));
		}
		
		return devices.get(deviceID);
	}
	
	/**
	 * get the group-class by id
	 * @param groupID
	 * @return a TradfriGroup instance
	 */
	public TradfriGroup group(int groupID) {
		//put a new TradfriGroup to the groups-map if it doesn't exist yet
		if(!groups.containsKey(groupID)) {
			groups.put(groupID, new TradfriGroup(this, groupID));
		}
		
		return groups.get(groupID);
	}
	
	/**
	 * ping the Gateway in order to keep the connection open
	 */
	public void doPing() {
		client.setURI("coap://192.168.2.65:5684/.well-known/core");
		client.ping(5000);
	}
	
	/**
	 * perform a blocking get request
	 * @param incomplete coap uri (e.g. '/15001/65538')
	 * @return the Response
	 */
	public CoapResponse get(String uri){
		uri = "coaps://" + this.gatewayIP + uri;
		
		CoapResponse response = null;
		try {
			//construct an URI to check validity
			@SuppressWarnings("unused")
			URI temp = new URI(uri);
			
			client.setURI(uri);
			response = client.get();

		} catch (URISyntaxException e) {
			System.err.println("[GWConnection] Invalid URI in GWConnection.get: " + e.getMessage());
			System.exit(-1);
		}

		return response;
	}
	
	/**
	 * start observing, register handler
	 * @param uri incomplete coap uri (e.g. '/15001/65538')
	 * @param handler Handler-Function that is called once it was updated
	 */
	public void observe(String uri, CoapHandler handler){
		uri = "coaps://" + this.gatewayIP + uri;
		
		try{
			//construct an URI to check validity
			@SuppressWarnings("unused")
			URI temp = new URI(uri);
		}catch(URISyntaxException e){
			System.err.println("[GWConnection] Invalid URI in GWConnection.observe: " + e.getMessage());
			System.exit(-1);
		}
		
		/*Request temp = new Request(Code.GET);
		temp.setURI(uri);
		temp.setObserve();*/
		
		client.setURI(uri);
		client.observe(handler);
	}
	
	/**
	 * Do a Put-Request on a specified URI. The data gets the content type application/json
	 * @param URI incomplete coap uri (e.g. '/15001/65538')
	 * @return The response, null if the request wasn't successfull
	 */
	public CoapResponse putJSON(String uri, String payload){
		uri = "coaps://" + this.gatewayIP + uri;
		
		CoapResponse response = null;
		try {
			//construct an URI to check validity
			@SuppressWarnings("unused")
			URI temp = new URI(uri);
			
			client.setURI(uri);
			response = client.put(payload, MediaTypeRegistry.APPLICATION_JSON);

		} catch (URISyntaxException e) {
			System.err.println("[GWConnection] Invalid URI in GWConnection.putJSON: " + e.getMessage());
			System.exit(-1);
		}

		return response;
	}
}
