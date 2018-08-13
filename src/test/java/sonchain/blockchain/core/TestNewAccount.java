package sonchain.blockchain.core;


import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sonchain.blockchain.accounts.NewAccount;

public class TestNewAccount {
	@Test
    public void test1() throws IOException 
    {
		byte[] a= {23,34};
		byte[] b= {30,6};
		Authority authority = new Authority();
		authority.setThreshold(20);
		NewAccount newAccount = new NewAccount();
		newAccount.setCreater("232");
		newAccount.setActive(authority);;
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode blockNode = mapper.createObjectNode();
		newAccount.toJson(blockNode);
		String content = mapper.writeValueAsString (blockNode);
		System.out.println(content);
    }
	@Test
	public void test2() throws IOException 
	{
		ObjectMapper mapper = new ObjectMapper();
		NewAccount newAccount = new NewAccount();
		String content ="{\"creater\":\"232\",\"name\":\"\",\"newProducers\":{\"threshold\":20,\"keys\":[],\"accounts\":[],\"waits\":[]}}";
	    JsonNode node = mapper.readTree(content); 
	    newAccount.jsonParse(node);
	    System.out.println(node);
	}
}
