package sonchain.blockchain.libraries.appbase;

import java.util.HashMap;
import java.util.Map;

public class VariablesMap {

	private Map<String, String> m_options = new HashMap<String, String>();
	
	public String get(String key){
		return m_options.get(key);
	}
	
	public void put(String key, String value){
		m_options.put(key, value);
	}
	
	public boolean containsKey(String key){
		return m_options.containsKey(key);
	}
	
	public int size(){
		return m_options.size();
	}
} 
