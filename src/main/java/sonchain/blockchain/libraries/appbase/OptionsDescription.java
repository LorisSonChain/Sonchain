package sonchain.blockchain.libraries.appbase;

import java.util.HashMap;
import java.util.Map;

public class OptionsDescription {

	private Map<String, String> m_optionValue = new HashMap<String, String>();
	private Map<String, String> m_optionDescritions = new HashMap<String, String>();
	
	public void addOption(String option, String value, String optionDescrition){
		m_optionValue.put(option, value);
		m_optionDescritions.put(option, optionDescrition);
	}
}
