package sonchain.blockchain.libraries.appbase;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class Application {

	public static final Logger m_logger = Logger.getLogger(Application.class);
	
	private static Application m_app = null;
	public Application(){		
	}
	
	public static Application getInstance(){
		if(m_app == null){
			m_app = new Application();
		}
		return m_app;
	}
	
	private ApplicationImpl m_my = new ApplicationImpl();
	private List<AbstractPlugin> m_initialized_plugins = Collections.synchronizedList(new ArrayList<AbstractPlugin>());
	private Map<String, AbstractPlugin> m_plugins = Collections.synchronizedMap(new HashMap<String, AbstractPlugin>());
	private List<AbstractPlugin> m_running_plugins = Collections.synchronizedList(new ArrayList<AbstractPlugin>());
	
	public int getVersion(){
		return m_my.m_version;
	}
	
	public void setVersion(int version){
		m_my.m_version = version;
	}
    
    public String get_data_dir(){
    	return m_my.m_data_dir;
    }
    
    public void set_data_dir(String data_dir){
    	m_my.m_data_dir = data_dir;
    }
    
    public String get_config_dir(){
    	return m_my.m_config_dir;
    }
    
    public void set_config_dir(String config_dir){
    	m_my.m_config_dir = config_dir;
    }
    
    public String get_logging_conf(){
    	return m_my.m_logging_config;
    }

    public void plugin_initialized(AbstractPlugin plug){ 
    	m_logger.debug("plugin_initialized start");
    	m_initialized_plugins.add(plug);
    	m_logger.debug("plugin_initialized end");
    }
    
    public void plugin_started(AbstractPlugin plug){ 
    	m_logger.debug("plugin_started start");
    	m_running_plugins.add(plug); 
    	m_logger.debug("plugin_started end");
    }
    
    public boolean initialize(List<AbstractPlugin> plugins, String[] args){
    	m_logger.debug("initialize start");
    	return initialize_impl(plugins, args);
    }
    
    public boolean initialize_impl(List<AbstractPlugin> plugins, String[] args){
    	m_logger.debug("initialize_impl start");
    	set_program_options();
    	if(plugins != null)
    	{
    		VariablesMap options = new VariablesMap();
	    	try
	    	{
	    		for(AbstractPlugin plugin : plugins){
	    			if(plugin.getState() == PluginState.REGISTERED){
		    			plugin.initialize(options);
	    			}
	    		}
	    	}
	    	catch(Exception ex){
	    		//TODO
	    		m_logger.error("initialize_impl error:" + ex.getMessage());
	    		return false;
	    	}
    	}
    	m_logger.debug("initialize_impl end");
    	return true;
    }
    
    public void startup(){
    	try
    	{
        	m_logger.debug("startup start");
    		for(AbstractPlugin plugin : m_initialized_plugins){
    			plugin.pluginStartup();
    		}
        	m_logger.debug("startup end");
    	}
    	catch(Exception ex){
    		shutdown();
    		m_logger.error("startup error:" + ex.getMessage());
    		//TODO
    	}
    }
    
    public void shutdown() {
    	m_logger.debug("shutdown start");
		for(AbstractPlugin plugin : m_running_plugins){
			plugin.shutdown();
		}
		m_running_plugins.clear();
		m_initialized_plugins.clear();
		m_plugins.clear();
    	m_logger.debug("shutdown end");
    }
    
    public void exec(){
    	m_logger.debug("exec start");
    	shutdown();
    	m_logger.debug("exec end");
    }
    
    public void quit() {
    	
    }
    
    public boolean register_plugin(AbstractPlugin plugin){
    	m_logger.debug("register_plugin start");
    	String name = plugin.getClass().getName();
    	if(m_plugins.containsKey(name)){
    		return true;
    	}
    	m_plugins.put(name, plugin);
    	m_logger.debug("register_plugin end");
    	return true;
    }
    
    public AbstractPlugin find_plugin(String name){
    	m_logger.debug("find_plugin start");
    	if(m_plugins.containsKey(name)){
    		return m_plugins.get(name);
    	}
    	m_logger.debug("find_plugin end");
    	return null;
    }
    
    public AbstractPlugin get_plugin(String name){
    	m_logger.debug("get_plugin start");
    	return find_plugin(name);
    }

    public void set_program_options(){
    	
    }
    
    public void write_default_config(String cfgFile){
    	
    }
    
    public void print_default_config(OutputStream os){
    }
}
