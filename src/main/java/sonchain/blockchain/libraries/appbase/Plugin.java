package sonchain.blockchain.libraries.appbase;

import org.apache.log4j.Logger;

public class Plugin extends AbstractPlugin{

	public static final Logger m_logger = Logger.getLogger(Plugin.class);
	public Plugin(){
		
	}
	public Plugin(String name)
	{		
		m_name = name;
	}
	
	private PluginState m_state = PluginState.REGISTERED;	
	@Override
	public PluginState getState()
	{
		return m_state;
	}
	
	private String m_name = "";
	@Override
	public String getName()
	{
		return m_name;
	}
	
	@Override
	public void initialize(VariablesMap options)
	{
		m_logger.debug("initialize start");
		if(m_state == PluginState.REGISTERED){
			m_state = PluginState.INITIALIZED;
			pluginInitialize(options);
			Application.getInstance().plugin_initialized(this);
		}		
		m_logger.debug("initialize end");
	}
	
	@Override
	public void setProgramOptions(OptionsDescription cli, OptionsDescription cfg)
	{
		
	}
	
	@Override
	public void startup()
	{
		m_logger.debug("startup start");
		if(m_state == PluginState.INITIALIZED){
			m_state = PluginState.STARTED;
			pluginStartup();
			Application.getInstance().plugin_started(this);
		}	
		m_logger.debug("startup end");	
	}
	
	@Override
	public void shutdown()
	{
		m_logger.debug("shutdown start");
		if(m_state == PluginState.STARTED){
			m_state = PluginState.STOPPED;
			pluginShutdown();			
		}
		m_logger.debug("shutdown end");	
	}	

	@Override
	public void pluginInitialize(VariablesMap options){		
	}

	@Override
	public void pluginStartup(){		
	}

	@Override
	public void pluginShutdown(){		
	}
}
