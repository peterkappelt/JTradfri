/**
 * 
 */
package net.kappelt.JTradfri.Tradfri;

/**
 * @author peter
 *
 */
public interface TradfriDeviceEventListener {

	/**
	 * Is invoked once a update for a device occured
	 * @param updatedDevice the TradfriDevice the updated happened for
	 */
	public void onUpdate(TradfriDevice updatedDevice);
}
