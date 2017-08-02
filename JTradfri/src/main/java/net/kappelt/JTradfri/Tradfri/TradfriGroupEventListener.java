package net.kappelt.JTradfri.Tradfri;

public interface TradfriGroupEventListener {
	
	/**
	 * Is invoked once a update for a group occurred
	 * @param updatedGroup the TradfriGroup the updated happened for
	 */
	public void onUpdate(TradfriGroup updatedGroup);
}
