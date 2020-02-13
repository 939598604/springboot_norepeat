package com.example.norepeat;

import cn.hutool.json.JSONUtil;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
 
public class KeyUtil {
    public static String generate(Method method, Object... args) throws UnsupportedEncodingException {
		StringBuilder sb = new StringBuilder(method.toString());
		for (Object arg : args) {
			sb.append(toString(arg));
		}
		return md5(sb.toString());
	}
 
	private static String toString(Object object) {
		if (object == null) {
			return "null";
		}
		if (object instanceof Number) {
			return object.toString();
		}
		return JSONUtil.toJsonStr(object);
	}
 
	public static String md5(String str) {
		StringBuilder buf = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(str.getBytes());
			byte b[] = md.digest();
			int i;
			for (int offset = 0; offset < b.length; offset++) {
				i = b[offset];
				if (i < 0)
					i += 256;
				if (i < 16)
					buf.append(0);
				buf.append(Integer.toHexString(i));
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return buf.toString();
	}
}