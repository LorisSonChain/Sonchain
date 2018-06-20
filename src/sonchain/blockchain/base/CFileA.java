package sonchain.blockchain.base;

import owchart.owlib.Base.RefObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;


//****************************************************************************\
//*                                                                             *
//* CFileA.cs -    File functions, types, and definitions                        *
//*                                                                             *
//*               Version 1.00 ★★★★★                                       *
//*                                                                             *
//*               Copyright (c) 2016-2016, Client. All rights reserved.         *
//*               Created by Lord.                                              *
//*                                                                             *
//******************************************************************************


/** 
 CFileA
*/
public class CFileA
{
	public static final int OF_READWRITE = 2;
	public static final int OF_SHARE_DENY_NONE = 0x40;
	
	public static boolean Append(String file, String content)
	{
		try
		{
			OutputStream os = new FileOutputStream(new File(file), true);
			os.write(content.getBytes());
			os.close();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public static void CreateDirectory(String dir)
	{
		try
		{
			File f = new File(dir);
			if(f.exists())
			{
				f.mkdir();
			}
		}
		catch (Exception ex)
		{
		}
	}
	
	public static boolean GetDirectories(String dir, java.util.ArrayList<String> dirs)
	{
		try
		{
			File f = new File(dir);
			if(f.exists() && f.isDirectory())
			{
				File[] fList = f.listFiles();
				int fListSize = fList.length;
				for(int i = 0; i < fListSize; i++)
				{
					File subFile = fList[i];
					if(subFile.isDirectory())
					{
						dirs.add(subFile.getAbsolutePath());
					}
				}
				return true;
			}
			return false;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static boolean GetFiles(String dir, java.util.ArrayList<String> files)
	{
		try
		{
			File f = new File(dir);
			if(f.exists() && f.isDirectory())
			{
				File[] fList = f.listFiles();
				int fListSize = fList.length;
				for(int i = 0; i < fListSize; i++)
				{
					File subFile = fList[i];
					if(subFile.isFile())
					{
						files.add(subFile.getAbsolutePath());
					}
				}
				return true;
			}
			return false;
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static boolean IsDirectoryExist(String dir)
	{
		try
		{
			File f = new File(dir);
			return f.isDirectory() && f.exists();
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static boolean IsFileExist(String file)
	{
		try
		{
			File f = new File(file);
			return f.isFile() && f.exists();
		}
		catch (Exception ex)
		{
			return false;
		}
	}

	public static boolean Read(String file, RefObject<String> content)
	{
		try
		{
			InputStream is = new FileInputStream(new File(file));
			byte[] buffer = new byte[is.available()];//新建一个字节数组
			is.read(buffer);
			content.argvalue = new String(buffer);
			is.close();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	public static boolean RemoveFile(String file)
	{
		return false;
	}
	
	public static boolean Write(String file, String content)
	{
		try
		{
			OutputStream os = new FileOutputStream(new File(file));
			os.write(content.getBytes());
			os.close();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}
}