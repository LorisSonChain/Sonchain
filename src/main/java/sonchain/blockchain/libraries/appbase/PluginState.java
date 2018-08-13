package sonchain.blockchain.libraries.appbase;

import sonchain.blockchain.core.TransactionType;

public enum PluginState {
	///< the plugin is constructed but doesn't do anything
	REGISTERED,
	 ///< the plugin has initialized any state required but is idle
	INITIALIZED,
	///< the plugin is actively running
	STARTED,
	///< the plugin is no longer running
	STOPPED;
	
	public int GetValue()
	{
		return ordinal();
	}
  
	public static PluginState ForValue(int value)
	{
		return values()[value];
	}
}
