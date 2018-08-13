package sonchain.blockchain.core;


import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestKeyWeight {
	@Test
    public void test1() throws IOException 
    {
		byte[] a= {23,34};
		byte[] b= {30,6};
		KeyWeight keyWeight = new KeyWeight(a,2);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		keyWeight.toJson(blockNode);
		String content = mapper.writeValueAsString (blockNode);
		System.out.println(content);
    }
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		KeyWeight keyWeight = new KeyWeight();
		String content ="{\"key\":\"1722\",\"weight\":2}";
	    JsonNode node = mapper.readTree(content); 
	    keyWeight.jsonParse(node);
	    System.out.println(node);
	}
}
