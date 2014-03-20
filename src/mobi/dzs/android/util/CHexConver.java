package mobi.dzs.android.util;

import android.annotation.SuppressLint;
import java.util.Locale;

/**
 * 16进制值与String/Byte之间的转换
 * 
 * @author JerryLi
 * @email lijian@dzs.mobi
 * @data 2011-10-16
 * */
public class CHexConver {
	private final static String mHexStr = "0123456789ABCDEF";
	private final static char[] mHexChars = mHexStr.toCharArray();

	/**
	 * 检查16进制字符串是否有效
	 *     字符长为2的倍数。
	 * 
	 * @param String
	 *            str 16进制字符串
	 * @return boolean
	 */
	@SuppressLint("DefaultLocale")
	public static boolean isHexStr(String str) {
		String tmpStr = str.trim().replace(" ", "").toUpperCase(Locale.US);
		int len = tmpStr.length();

		if (len < 1 || len % 2 != 0)
			return false;

		for (int i = 0; i < len; i++)
			if (!mHexStr.contains(tmpStr.substring(i, i + 1)))
				return false;

		return true;
	}

	/**
	 * 字符串转换成十六进制字符串
	 * 
	 * @param String
	 *            str 待转换的ASCII字符串
	 * @return String 每个Byte之间空格分隔，如: [61 6C 6B]
	 */
	public static String strToHex(String str) {
		return bytesToHex(str.getBytes());
	}

	/**
	 * 十六进制字符串转换成 ASCII字符串
	 * 
	 * @param String
	 *            hex字符串
	 * @return String 对应的字符串
	 */
	public static String hexToStr(String hexStr) {
		hexStr = hexStr.trim().replace(" ", "").toUpperCase(Locale.US);
		char[] hexs = hexStr.toCharArray();
		byte[] bytes = new byte[hexStr.length() / 2];
		int c;

		for (int i = 0; i < bytes.length; i++) {
			c = mHexStr.indexOf(hexs[2 * i]) << 4;
			c |= mHexStr.indexOf(hexs[2 * i + 1]);
			bytes[i] = (byte) (c & 0xFF);
		}
		
		return new String(bytes);
	}

	/**
	 * bytes转换成十六进制字符串
	 * 
	 * @param byte[] b byte数组
	 * @param int len 取前N位处理 N=iLen
	 * @return String 每个Byte值之间空格分隔
	 */
	public static String bytesToHex(byte[] b) {
		StringBuilder hexStr = new StringBuilder("");
		
		for (int i = 0; i < b.length; i++) {
			hexStr.append(mHexChars[b[i] & 0xFF >> 4]);
			hexStr.append(mHexChars[b[i] & 0x0F]);
			hexStr.append(' ');
		}
		
		return hexStr.toString().trim();
	}

	/**
	 * Hex字符串转换为Bytes
	 * 
	 * @param String
	 *            src Byte字符串，每个Byte之间没有分隔符(字符范围:0-9 A-F)
	 * @return byte[]
	 */
	public static byte[] hexToBytes(String hexStr) {
		/* 对输入值进行规范化整理 */
		hexStr = hexStr.trim().replace(" ", "").toUpperCase(Locale.US);
		// 处理值初始化
		int m = 0, n = 0;
		int len = hexStr.length() / 2; // 计算长度
		byte[] bytes = new byte[len]; // 分配存储空间

		for (int i = 0; i < len; i++) {
			m = i * 2 + 1;
			n = m + 1;
			bytes[i] = (byte) (Integer.decode("0x" + hexStr.substring(i * 2, m)
					+ hexStr.substring(m, n)) & 0xFF);
		}
		
		return bytes;
	}

	/**
	 * String的字符串转换成unicode的String
	 * 
	 * @param String
	 *            strText 全角字符串
	 * @return String 每个unicode之间无分隔符
	 * @throws Exception
	 */
	public static String strToUnicode(String strText) throws Exception {
		char c;
		StringBuilder str = new StringBuilder();
		int intAsc;
		String strHex;
		for (int i = 0; i < strText.length(); i++) {
			c = strText.charAt(i);
			intAsc = (int) c;
			strHex = Integer.toHexString(intAsc);
			if (intAsc > 128)
				str.append("\\u" + strHex);
			else
				// 低位在前面补00
				str.append("\\u00" + strHex);
		}
		return str.toString();
	}

	/**
	 * unicode的String转换成String的字符串
	 * 
	 * @param String
	 *            hex 16进制值字符串 （一个unicode为2byte）
	 * @return String 全角字符串
	 */
	public static String unicodeToString(String hex) {
		int t = hex.length() / 6;
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < t; i++) {
			String s = hex.substring(i * 6, (i + 1) * 6);
			// 高位需要补上00再转
			String s1 = s.substring(2, 4) + "00";
			// 低位直接转
			String s2 = s.substring(4);
			// 将16进制的string转为int
			int n = Integer.valueOf(s1, 16) + Integer.valueOf(s2, 16);
			// 将int转换为字符
			char[] chars = Character.toChars(n);
			str.append(new String(chars));
		}
		return str.toString();
	}
}
