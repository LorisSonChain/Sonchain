package sonchain.blockchain.core;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class TestAuthority {
	@Test
    public void test1() throws IOException 
    {
		List<KeyWeight> ks = new ArrayList<KeyWeight>();
		List<PermissionLevelWeight> listPlw = new ArrayList<PermissionLevelWeight>();
		byte[] a= {23,34};
		KeyWeight keyWeight = new KeyWeight();
		keyWeight.setKey(a);
		ks.add(keyWeight);
		PermissionLevelWeight plw = new PermissionLevelWeight();
		plw.setWeight(2);
		WaitWeight waitWeight = new WaitWeight();
		waitWeight.setWait_sec(40);
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode Node = mapper.createObjectNode();
		Authority authority = new Authority();
		authority.setThreshold(23);;
		authority.setKeys(ks);
		authority.toJson(Node);
		String content = mapper.writeValueAsString (Node);
		System.out.println(content);
    }
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		Authority authority = new Authority();
		String content ="{\"threshold\":23,\"keys\":[{\"key\":\"1722\",\"weight\":0}],\"accounts\":[],\"waits\":[]}";
		JsonNode node = mapper.readTree(content); 
	    authority.jsonParse(node);
	}
}
