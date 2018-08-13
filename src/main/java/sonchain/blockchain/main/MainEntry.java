package sonchain.blockchain.main;

import sonchain.blockchain.plugins.wallet.Wallet;
import sonchain.blockchain.service.DataCenter;
/**
 * /**
 *                             _ooOoo_
 *                            o8888888o
 *                            88" . "88
 *                            (| -_- |)
 *                            O\  =  /O
 *                         ____/`---'\____
 *                       .'  \\|     |//  `.
 *                      /  \\|||  :  |||//  \
 *                     /  _||||| -:- |||||-  \
 *                     |   | \\\  -  /// |   |
 *                     | \_|  ''\---/''  |   |
 *                     \  .-\__  `-`  ___/-. /
 *                   ___`. .'  /--.--\  `. . __
 *                ."" '<  `.___\_<|>_/___.'  >'"".
 *               | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *               \  \ `-.   \_ __\ /__ _/   .-` /  /
 *          ======`-.____`-.___\_____/___.-`____.-'======
 *                             `=---='
 *          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *                     佛祖保佑        永无BUG
 * @author GAIA
 *
 */
public class MainEntry {

	public final static String FILESEPARATOR = System.getProperty("file.separator");
	
	public static void main(String[] args) 
	{
		//String filePath = DataCenter.GetAppPath() + "//";
		//String content1 = WalletManager.GenerateFullNewWalletFile("111111", new File(filePath));
		
		//System.console().printf(content1);
		// InitRedisTraderID();
		//String content1 = "UTC--2018-04-27T06-20-48.861000000Z--51ec4cb8ce27adb418897744462e58a672cfbc48.json";
		//filePath = DataCenter.GetAppPath() + "//" + content1;
		//RefObject<String> content = new RefObject<>("");
		//CFileA.Read(filePath, content);
		//String a = "efdbb0bf719bcc6effd5038c3ff86e232456419381a416899fa3be562519e0c5";
		//Credentials credentials = WalletManager.LoadCredentials("111111", content.argvalue);
		//Credentials credentials = WalletManager.LoadCredentials(a);
		String appPath = DataCenter.GetAppPath();
		String fileName = appPath + FILESEPARATOR + "test.js";
//		try{
//			
//			IPFS ipfs = new IPFS("/ip4/192.168.86.125/tcp/5001");
//			ipfs.refs.local();
//			
//			NamedStreamable.FileWrapper file = new NamedStreamable.FileWrapper(new File(fileName));			
//
//	        List<MerkleNode> addParts = ipfs.add(file);
//	        MerkleNode filePart = addParts.get(0);
//	        System.out.print(filePart.toJSONString());
//		}
//		catch(Exception ex){
//			int a = 0;
//			int b = 0;
//		}
		Wallet wallet = new Wallet();
		wallet.createKey("");
		
		if (args != null && args.length > 0)
		{
			fileName = appPath + FILESEPARATOR + args[0];
		}
		DataCenter.StartService(fileName);
	}	
}
