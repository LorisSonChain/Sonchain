package sonchain.blockchain.core;


import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestWaitWeight {
	@Test
    public void test1() throws IOException 
    {
		WaitWeight waitWeight = new WaitWeight(1,2);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		waitWeight.toJson(blockNode);
		String content = mapper.writeValueAsString (blockNode);
		System.out.println(content);
    }
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		WaitWeight waitWeight = new WaitWeight();
		String content ="{\"wait_sec\":1,\"weight\":2}";
	    JsonNode node = mapper.readTree(content); 
	    waitWeight.jsonParse(node);
	    System.out.println(node);
	}

	
}
