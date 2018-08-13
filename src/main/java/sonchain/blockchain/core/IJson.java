package sonchain.blockchain.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface IJson {

    void toJson(ObjectNode node);
	void jsonParse(JsonNode node);
	String toJson();
	void jsonParse(String json);
}
