package me.suff.regeneration.proxy;

import me.suff.regeneration.common.capability.CapabilityRegeneration;

/**
 * Created by Sub
 * on 17/09/2018.
 */
public class ServerProxy implements IProxy {
	
	@Override
	public void preInit() {
		CapabilityRegeneration.init();
	}
	
	@Override
	public void postInit() {
	
	}
}
