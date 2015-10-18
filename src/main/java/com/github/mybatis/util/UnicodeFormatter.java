package com.github.mybatis.util;

import com.google.common.base.Strings;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理字符串的unicode表示
 * Created by lirui on 2015-10-18 11:05.
 */
public class UnicodeFormatter {
  private static final String[] HEX_DIGIT = new String[256];

  static {
    for (int i = 0; i < 256; i++) {
      HEX_DIGIT[i] = byteToHex((byte) i);
    }
  }

  private static final Pattern NET_UNICODE_PATTERN = Pattern.compile("&#(\\d{1,5});");
  private static final Pattern UNICODE_PATTERN = Pattern.compile("\\\\u([0-9a-fA-F]{1,4})");

  private static String byteToHex(byte b) {
    final char byteDigit[] = {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };
    char[] array = {byteDigit[(b >> 4) & 0x0f], byteDigit[b & 0x0f]};
    return new String(array);
  }

  public static boolean isHexChar(char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  public static String toHex(int i) {
    return toHex((char) i);
  }

  public static String toHex(char c) {
    int hi = c >>> 8;
    int lo = c & 0x0ff;
    return HEX_DIGIT[hi] + HEX_DIGIT[lo];
  }

  public static String toUnicodeString(int i) {
    return toUnicodeString((char) i);
  }

  public static String toUnicodeString(char c) {
    int hi = c >>> 8;
    int lo = c & 0x0ff;
    return "\\u" + HEX_DIGIT[hi] + HEX_DIGIT[lo];
  }

  /**
   * 把所有字符都换成\\uxxxx的字符编码样式
   * @param s 待编码字符
   * @return 编码后的字符串
   */
  public static String toUnicodeString(String s) {
    if (Strings.isNullOrEmpty(s)) return "";
    int length = s.length();
    StringBuilder sbd = new StringBuilder(length * 6);
    for (int i=0; i< length; i++) {
      char c = s.charAt(i);
      int hi = c >>> 8;
      int lo = c & 0x0ff;
      sbd.append('\\').append('u').append(HEX_DIGIT[hi]).append(HEX_DIGIT[lo]);
    }
    return sbd.toString();
  }

  /**
   * 把出了ASCII编码之外的所有字符都换成\\uxxxx的字符编码样式
   * @param s 待编码字符
   * @return 编码后的字符串
   */
  public static String nonAsciiToUnicodeString(String s) {
    if (Strings.isNullOrEmpty(s)) return "";
    int length = s.length();
    StringBuilder sbd = new StringBuilder(length * 2);
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      if (c < 256) {
        sbd.append(c);
      } else {
        int hi = c >>> 8;
        int lo = c & 0x0ff;
        sbd.append('\\').append('u').append(HEX_DIGIT[hi]).append(HEX_DIGIT[lo]);
      }
    }
    return sbd.toString();
  }

  /**
   * utf8可以编码1-6字节的字符，但是mysql5.5版本之前只能支持3字节，这里针对4字节以上的进行编码。 <br/>
   * http://baike.baidu.com/view/25412.htm <br/>
   * 正常汉字范围在 4e00 - 9fa5，全角字符在 ff00 - ffef，超过这个范围的就使用unicode进行编码。<br/>
   * 针对已经是\\uxxxx编码的pattern，在其前面放一个0x03的不可见字符做标识，decode的时候不做处理。<br/>
   * @param s
   * @return
   */
  public static String unicodeUTF8mb4String(String s) {
    if (Strings.isNullOrEmpty(s)) return "";
    int len = s.length();
    StringBuilder sbd = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (c > '\u9fa5') { //非正常汉字范围
        if (c >= '\uff00' && c <= '\uffef') { //全角字符范围
          sbd.append(c);
        } else {
          sbd.append(toUnicodeString(c));
        }
      } else if (c == '\\') {
        if (i + 3 < len && s.charAt(i + 1) == 'u' && isHexChar(s.charAt(i + 2))) {
          sbd.append('\u0003');
        }
        sbd.append(c);
      } else {
        sbd.append(c);
      }
    }

    return sbd.length() != len ? sbd.toString() : s;
  }

  /**
   * 字符串转换为int，避免运行时异常
   *
   * @param str        待处理字符串
   * @param defaultVal 默认值
   * @return 转换失败返回指定默认值
   */
  private static int toInt(String str, int defaultVal) {
    try {
      return Integer.parseInt(str.trim());
    } catch (Exception e) {
      return defaultVal;
    }
  }

  /**
   * 转换&#123;这种编码为正常字符<br/>
   * 有些手机会将中文转换成&#123;这种编码,这个函数主要用来转换成正常字符.
   *
   * @param str 待转换字符
   * @return String
   */
  public static String decodeNetUnicodeString(String str) {
    if (Strings.isNullOrEmpty(str)) return str;
    Matcher m = NET_UNICODE_PATTERN.matcher(str);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String mcStr = m.group(1);
      int charValue = toInt(mcStr, -1);
      String s = charValue > 0 ? String.valueOf(charValue) : "";
      m.appendReplacement(sb, Matcher.quoteReplacement(s));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /**
   * 转换\\u123这种编码为正常字符<br/>
   *
   * @param s 待转换字符
   * @return String
   */
  public static String decodeUnicodeString(String s) {
    if (Strings.isNullOrEmpty(s)) return s;

    int length = s.length();
    StringBuilder sb = new StringBuilder(length);
    Matcher m = UNICODE_PATTERN.matcher(s);
    int begin = 0;
    while (m.find()) {
      int start = m.start();
      if (start > 0 && s.charAt(start - 1) == '\u0003') {
        if (start - 1 > begin) {
          sb.append(s, begin, start - 1);
        }
        begin = start;
        continue;
      }
      sb.append(s, begin, start);
      String mcStr = m.group(1);
      try {
        char charValue = (char) Integer.parseInt(mcStr, 16);
        sb.append(charValue);
        begin = m.end();
      } catch (NumberFormatException e) {
        System.out.println(e.getMessage());
      }
    }

    if (begin < length) {
      sb.append(s, begin, length);
    }
    return sb.toString();
  }
}
