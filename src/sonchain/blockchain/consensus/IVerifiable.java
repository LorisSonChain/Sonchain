package sonchain.blockchain.consensus;

import java.util.List;

import sonchain.blockchain.base.Binary;

public interface IVerifiable extends ISerializable, IScriptContainer{

	//用于验证该对象的脚本列表
	Witness[] getScripts();
	void setScripts(Witness[] witness);
    
    /// <summary>
    /// 反序列化未签名的数据
    /// </summary>
    /// <param name="reader">数据来源</param>
    void deserializeUnsigned(Binary reader);

    /// <summary>
    /// 获得需要校验的脚本Hash值
    /// </summary>
    /// <returns>返回需要校验的脚本Hash值</returns>
    List<byte[]> getScriptHashesForVerifying();
    
    /// <summary>
    /// 序列化未签名的数据
    /// </summary>
    /// <param name="writer">存放序列化后的结果</param>
    void serializeUnsigned(Binary writer);	
}
