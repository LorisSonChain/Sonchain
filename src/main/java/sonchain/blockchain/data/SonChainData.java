/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonchain.blockchain.data;

import java.util.ArrayList;
import java.util.List;

import sonchain.blockchain.core.Transaction;

/**
 *
 * @author GAIA_Todd
 */
public class SonChainData 
{
	/**
	 * 消息类型
	 * 0：默认
	 * 1：交易列表
	 * 2：挖矿完成通知
	 * 3：区块确认通知
	 * 4：区块更新通知
	 */
	public int m_type = 0;	
	
    public String m_text = "";
    
    public List<Transaction> m_trans = new ArrayList<Transaction>();
    
    public String m_preBlockHash = "";
    
    public String m_blockJson = "";
}
