package sonchain.blockchain.core;

import sonchain.blockchain.base.Binary;

public class StateBase {
	public final int StateVersion = 0;
	
	public int size(){
		return 4;
	}
	
	public void deserialize(Binary reader){
		try
		{
			if(reader.ReadInt() != StateVersion){
				throw new IllegalArgumentException("StateVersion is error");
			}
		}catch(Exception ex){
			
		}
	}
	
	public void serialize(Binary writer){

		try
		{
			writer.WriteInt(StateVersion);;
		}catch(Exception ex){
			
		}
	}
	
//	public JObject toJson(){
//		JObject json = new JObject();
//		json["version"] = StateVersion;
//		return json;
//	}
}
