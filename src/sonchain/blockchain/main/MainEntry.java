package sonchain.blockchain.main;

import sonchain.blockchain.service.DataCenter;

public class MainEntry {

	public final static String FILESEPARATOR = System.getProperty("file.separator");
	
	public static void main(String[] args) throws Exception
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
		if (args != null && args.length > 0)
		{
			fileName = appPath + FILESEPARATOR + args[0];
		}
		DataCenter.StartService(fileName);
	}	
}
