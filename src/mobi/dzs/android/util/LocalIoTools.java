/**
 * 
 */
package mobi.dzs.android.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

/**
 * 本地IO操作类
 * 
 * @author JerryLi(hzjerry@gmail.com)
 * @version 1.0
 * @date 2010-06-25
 */
public class LocalIoTools {
	/**
	 * 通过本地文件载入字符串内容
	 * 
	 * @param String
	 *            path 文件路徑
	 * @return String 读取成功后輸出文件内容
	 */
	public static String loadFromFile(String pathName) {
		StringBuffer outStrBuf = new StringBuffer();
		String read;
		BufferedReader bufedReader;
		
		try {
			File file = new File(pathName); // 获取文件句柄
			bufedReader = new BufferedReader(new FileReader(file)); // 打开缓存
			/* 循环方式逐行读入 */
			while ((read = bufedReader.readLine()) != null)
				outStrBuf.append(read);
			bufedReader.close();
		} catch (Exception d) {
			// System.out.println(d.getMessage());
			return null;
		}
		
		return outStrBuf.toString(); // 输出文件内容
	}

	/**
	 * byte内容追加到本地文件 如果文件不存在，则创建
	 * 
	 * @param String
	 *            path 文件路徑(末尾不要带“\”符号)
	 * @param String
	 *            name 文件名
	 * @param byte[] data 需要写入的数据
	 * 
	 * @return boolean
	 * @see android.permission.WRITE_EXTERNAL_STORAGE
	 */
	public static boolean appendByteToFile(String path, String name, byte[] data) {
		FileOutputStream fOutStream = null;
		
		try {
			/* 检查目录是否存在 */
			File file = new File(path); // 获取文件句柄
			if (!file.exists())
				if (!file.mkdirs())// 目录不存在，创建之
					return false; // 目录创建失败，退出

			/* 检查文件是否存在 */
			file = new File(path + "\\" + name); // 获取文件句柄
			if (!file.exists())
				if (!file.createNewFile()) // 文件不存在，创建之
					return false; // 文件创建失败，退出
			
			// 追加方式写入文件
			fOutStream = new FileOutputStream(file, true);
		} catch (Exception d) {
			return false;
		}
		
		/* 写文件 */
		try {
			fOutStream.write(data);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				fOutStream.close();
			} catch (IOException e) {
			}
		}

		return true;
	}
	/*
	 * Output: if (LocalIOTools.appendByte2File("F:/temp", "javatest.txt",
	 * "this is test.\r\nbuf write".getBytes()))
	 * System.out.println("write ok."); else System.out.println("write fail.");
	 */// :~

	/**
	 * byte内容写入本地文件 如果文件存在则覆盖之
	 * 
	 * @param String
	 *            path 文件路徑(末尾不要带“\”符号)
	 * @param String
	 *            name 文件名
	 * @param byte[] data 需要写入的数据
	 * 
	 * @return boolean
	 * @see android.permission.WRITE_EXTERNAL_STORAGE
	 */
	public static boolean coverByteToFile(String path, String name, byte[] data) {
		try {
			/* 检查目录是否存在 */
			File fhd = new File(path); // 获取文件句柄
			if (!fhd.exists())
				if (!fhd.mkdirs())// 目录不存在，创建之
					return false; // 目录创建失败，退出

			/* 检查文件是否存在 */
			fhd = new File(path + "/" + name); // 获取文件句柄
			if (fhd.exists())
				fhd.delete(); // 文件存在，删除

			// 追加方式写入文件
			FileOutputStream fso = new FileOutputStream(fhd);
			
			fso.write(data);
			fso.close();
			return true;
		} catch (Exception d) {
			System.out.println(d.getMessage());
			return false;
		}

	}/*
	 * Output: if (LocalIOTools.appendByte2File("F:/temp", "javatest.txt",
	 * "this is test.\r\nbuf write".getBytes()))
	 * System.out.println("write ok."); else System.out.println("write fail.");
	 */// :~
}
