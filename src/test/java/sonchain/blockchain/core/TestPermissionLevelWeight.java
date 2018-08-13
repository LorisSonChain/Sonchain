package sonchain.blockchain.core;


import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestPermissionLevelWeight {
	@Test
    public void test1() throws IOException 
    {
		PermissionLevel pl = new PermissionLevel("23","llll"); 
		PermissionLevelWeight plw = new PermissionLevelWeight();
		plw.setWeight(20);
		plw.setPermission(pl);;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		plw.toJson(blockNode);
		String content = mapper.writeValueAsString (blockNode);
		System.out.println(content);
    }
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		PermissionLevelWeight plw = new PermissionLevelWeight();
		String content ="{\"weight\":20,\"permissions\":{\"actor\":\"23\",\"permission\":\"llll\"}}";
	    JsonNode node = mapper.readTree(content); 
	    plw.jsonParse(node);
	}
}
