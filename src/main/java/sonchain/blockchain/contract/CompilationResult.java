package sonchain.blockchain.contract;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CompilationResult {

    public Map<String, ContractMetadata> m_contracts = null;
    public String m_version = "";

    public static CompilationResult parse(String rawJson) throws IOException {
        if(rawJson == null || rawJson.isEmpty()){
            CompilationResult empty = new CompilationResult();
            empty.m_contracts = Collections.emptyMap();
            empty.m_version = "";

            return empty;
        } else {
            return new ObjectMapper().readValue(rawJson, CompilationResult.class);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContractMetadata {
        public String m_abi;
        public String m_bin;
        public String m_solInterface;
        public String m_metadata;

        public String getInterface() {
            return m_solInterface;
        }

        public void setInterface(String solInterface) {
        	m_solInterface = solInterface;
        }
    }
}
