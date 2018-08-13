package sonchain.blockchain.plugins.wallet;

import org.apache.log4j.Logger;

import sonchain.blockchain.libraries.appbase.OptionsDescription;
import sonchain.blockchain.libraries.appbase.Plugin;
import sonchain.blockchain.libraries.appbase.VariablesMap;

public class WalletPlugin extends Plugin{
	
	private WalletManager m_walletManager = null;
	public static final Logger m_logger = Logger.getLogger(WalletPlugin.class);
	
	public WalletPlugin()
	{		
	}
	
	public WalletPlugin(WalletPlugin walletPlugin){
		
	}
	
	@Override
	public String getName(){
		return "WalletPlugin";
	}

	@Override
	public void setProgramOptions(OptionsDescription cli, OptionsDescription cfg)
	{
		cfg.addOption("wallet-dir", ".", "The path of the wallet files (absolute path or relative to application data dir)");
		cfg.addOption("unlock-timeout", String.valueOf(900), 
				"Timeout for unlocked wallet in seconds (default 900 (15 minutes)). \r\n "
				+ "Wallets will automatically lock after specified number of seconds of inactivity.\r\n"
				+ "Activity is defined as any wallet command e.g. list-wallets.");
	}

	@Override
	public void pluginInitialize(VariablesMap options){
		m_logger.debug("pluginInitialize start");
		try{
			m_walletManager = new WalletManager();
			if(options.containsKey("wallet-dir")){
				String dir = options.get("wallet-dir");
				m_walletManager.setDir(dir);
			}
			if(options.containsKey("unlock-timeout")){
				int unlockTimeout = Integer.valueOf(options.get("unlock-timeout"));
				if(unlockTimeout < 0){
					m_logger.debug("Please specify a positive timeout " + unlockTimeout);
				}
				m_walletManager.setTimeout(unlockTimeout);
			}
			
		}catch(Exception ex){
			m_logger.error("pluginInitialize error " + ex.getMessage());
		}
		m_logger.debug("pluginInitialize end");
	}
	
	public WalletManager getWalletManger(){
		return m_walletManager;
	}
}
