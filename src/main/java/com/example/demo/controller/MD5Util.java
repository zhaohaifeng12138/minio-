package com.example.demo.controller;

import org.springframework.util.DigestUtils;

/**
 * @Author 赵海峰
 * @Description //加密
 * @Date  2021/6/10
 * @Param
 * @return
 **/
public class MD5Util {
	//盐，用于混交md5
	private static final String slat = "&%5123***&&%%$$#@";
	/**
	 * 生成md5
	 * @param
	 * @return
	 */
	public static String getMD5(String str) {
		String base = str +"/"+slat;
		String md5 = DigestUtils.md5DigestAsHex(base.getBytes());
		return md5;
	}
    public static String convertMD5(String inStr){

        char[] a = inStr.toCharArray();
        for (int i = 0; i < a.length; i++){
            a[i] = (char) (a[i] ^ 't');
            System.out.println(a[i]);
        }
        String s = new String(a);
        return s;

    }


    public static void main(String[] args) {
        String base = "str" +"/"+slat;
        System.out.println(getMD5(base));
        System.out.println(convertMD5(getMD5(base)));
    }
}