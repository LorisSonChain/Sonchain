package sonchain.blockchain.libraries.appbase;

public class ApplicationImpl {
	public VariablesMap m_variablesMap = new VariablesMap();
	public OptionsDescription m_app_options = new OptionsDescription();
	public OptionsDescription m_cfg_options = new OptionsDescription();
	
	public String m_data_dir = "";
	public String m_config_dir = "";
	public String m_logging_config = "";
	public int m_version;
}
