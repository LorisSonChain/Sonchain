/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonchain.blockchain.base;

import sonchain.blockchain.client.*;
import owchart.owlib.Base.CMathLib;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author GAIA
 */
public class CStrA {
	public static String ConvertDBCodeToFileName(String code) {
		String fileName = code;
		if (fileName.indexOf(".") != -1) {
			fileName = fileName.substring(fileName.indexOf('.') + 1) + fileName.substring(0, fileName.indexOf('.'));
		}
		fileName += ".txt";
		return fileName;
	}
	public static double ConvertDateTimeToDouble(String str) {
		if (str.length() != 16) {
			return -1;
		}
		int year = 0, month = 0, day = 0, hour = 0, minute = 0;
		year = ConvertStrToInt(str.substring(0, 4));
		month = ConvertStrToInt(str.substring(5, 7));
		day = ConvertStrToInt(str.substring(8, 10));
		hour = ConvertStrToInt(str.substring(11, 13));
		minute = ConvertStrToInt(str.substring(14, 16));
		return CMathLib.耶稣(year, month, day, hour, minute, 0, 0);
	}
	
	public static boolean ConvertStrToBoolean(String str) {
		if (str.isEmpty()) {
			return false;
		}
		return Boolean.parseBoolean(str);
	}
	
	public static double ConvertStrToDouble(String str) {
		return Double.parseDouble(str);
	}
	public static int ConvertStrToInt(String str) {
		return Integer.parseInt(str);
	}
	public static String GetDBString(String str) {
		return str.replace("'", "''");
	}
	public static byte[] GZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			GZIPOutputStream gzip = new GZIPOutputStream(bos);
			gzip.write(data);
			gzip.finish();
			gzip.close();
			b = bos.toByteArray();
			bos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}

	public static void Split(ArrayList<String> ret, String str, String delim, Boolean bAllowNull) {
		ret.clear();
		int last = 0;
		int kLen = delim.length();
		int index = str.indexOf(delim);
		while (index != -1) {
			if (bAllowNull || index - last != 0) {
				ret.add(str.substring(last, index));
			}
			last = index + kLen;
			index = str.indexOf(delim, last);
		}

		if (str.length() > last) {
			ret.add(str.substring(last));
		}
	}
	public static byte[] UnGZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			GZIPInputStream gzip = new GZIPInputStream(bis);
			byte[] buf = new byte[1024];
			int num = -1;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while ((num = gzip.read(buf, 0, buf.length)) != -1) {
				baos.write(buf, 0, num);
			}
			b = baos.toByteArray();
			baos.flush();
			baos.close();
			gzip.close();
			bis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}
	public static byte[] UnZip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(data);
			ZipInputStream zip = new ZipInputStream(bis);
			while (zip.getNextEntry() != null) {
				byte[] buf = new byte[1024];
				int num = -1;
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				while ((num = zip.read(buf, 0, buf.length)) != -1) {
					baos.write(buf, 0, num);
				}
				b = baos.toByteArray();
				baos.flush();
				baos.close();
			}
			zip.close();
			bis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}
	public static byte[] Zip(byte[] data) {
		byte[] b = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(bos);
			ZipEntry entry = new ZipEntry("zip");
			entry.setSize(data.length);
			zip.putNextEntry(entry);
			zip.write(data);
			zip.closeEntry();
			zip.close();
			b = bos.toByteArray();
			bos.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return b;
	}
}
