package sonchain.blockchain.core;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestPermissionLevel {

	@Test 
	public void test1() throws IOException 
	{
		PermissionLevel pl = new PermissionLevel("sd","kkkk");
		ObjectMapper mapper = new ObjectMapper();
	    ObjectNode transNode = mapper.createObjectNode();
	    pl.toJson(transNode);
	    String content = mapper.writeValueAsString (transNode);
	    System.out.println(content);
	    
	}
	
	@Test 
	public void test2() throws IOException 
	{
		PermissionLevel pl = new PermissionLevel();
		ObjectMapper mapper = new ObjectMapper();
		String content = "{\"actor\":\"sd\",\"permission\":\"kkkk\"}";
		JsonNode node = mapper.readTree(content); 
		System.out.println(node);
		pl.jsonParse(node);
	}
}
