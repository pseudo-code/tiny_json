package cn.via;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;

/**
 * @author venia
 */
public class Tokenizer {
	
	public static enum TAG {
		SOF, EOF,				// 开始 结束 
		L_BRACE, R_BRACE, 		// { }
		L_BRACKET, R_BRACKET,	// [ ]
		COMMA, SEMICOLON,		// , ;
		COLON,					// :
		STRING,					// "123" '321'
		TRUE,			  		// true
		FALSE,					// false
		NULL,					// null
		NUMBER					// int float bignumber(eg:12e+10)
	}
	
	public static final Boolean TRUE = new Boolean(true);
	public static final Boolean FALSE = new Boolean(false);
	
	private String src;
	private int pos;
	private int length;
	
	private String token;
	private Object num;
	
	private int rowCount;
	private int colCount;
	
	public Tokenizer(String src) {
		if(src == null) throw new NullPointerException();
		
		this.src = src;
		this.pos = 0;
		this.length = src.length();
		this.token = null;
		
		this.rowCount = 1;
		this.colCount = 1;
	}
	
	// 使用peek方法查看下一个token的类型，使用getToken可以获取下一个token
	public TAG peek() {
		if(pos >= length) {
			return TAG.EOF;
		}
		
		char charAt = '\0';
		
		while(true) { // 有空白字符时重复读，直到非空字符
			
			charAt = src.charAt(pos);
			switch(charAt) {
			case '{': return TAG.L_BRACE;
			case '}': return TAG.R_BRACE;
			case '[': return TAG.L_BRACKET;
			case ']': return TAG.R_BRACKET;
			case ',': return TAG.COMMA;
			case ';': return TAG.SEMICOLON;
			case ':': return TAG.COLON;
			
			case ' ':
			case '\t': {
				pos++; // 跳过空白字符
				break;
			}
			
			case '\r': {
				pos++; // 跳过换行符
				if('\n' == src.charAt(pos)) pos++; // 处理 \r\n 这种情况
				rowCount++;
				colCount=0;
				break;
			}
			case '\n': {
				pos++; // 跳过换行符
				rowCount++;
				colCount=0;
				break;
			}
			
			case '\"': {
				token = readString('\"');
				return TAG.STRING;
			}
			case '\'': {
				token = readString('\'');
				return TAG.STRING;
			}
			
			default: {
				token = readCharSequence();
				token = token.toLowerCase();
				if ("true".equals(token)) {
					return TAG.TRUE;
				}
				else if ("false".equals(token)) {
					return TAG.FALSE;
				}
				else if ("null".equals(token)) {
					return TAG.NULL;
				}
				else {
					// treat as a number
					try {
						if (token.contains("e")) {
							num = new BigDecimal(token);
						} else if (token.contains(".")) {
							num = Float.parseFloat(token);
						} else {
							num = Integer.parseInt(token);
						}
						return TAG.NUMBER;
					} catch (Exception e) {
						printEnv(System.out);
						throw new RuntimeException("Bad gramma at row:"
								+ getRowCount() + ", col:" + getColCount() + ".");
					}
				}
			}
			}
		}
	}
	
	// 读取一个字符串，返回："string" | 'string'
	private String readString(char sep) {
		int start = pos;
		int end = start+1; // 首字符为 " 或者 '，直接跳过
		
		while(end < length) {
			char c = src.charAt(end);
			end++;
			
			if(c == sep)
				break;
			else if(c == '\\') { // 转义
				if('u' == src.charAt(end)) { // 处理 \u004a
					end += 5;
				}
				else { // 处理 \" \\ \/ \b \f \n \r \t
					end++;
				}
			}
		}
		
		if(end > length) {
			printEnv(System.out);
			throw new RuntimeException("Bad grammar at row:" + getRowCount() + ", col:" + getColCount() + ".");
		}
		
		if(src.charAt(end - 1) != sep) {
			printEnv(System.out);
			throw new RuntimeException("Expect a " + sep + " at row:" + getRowCount() + ", col:" + getColCount() + ".");
		}
			
		return new String(src.substring(start+1, end-1)); // 去掉首位的引号
	}
	
	private String readCharSequence() {
		int start = pos;
		int end = start+1; // 首字符位其他類型字符
		
		while(end < length) {
			char c = src.charAt(end);
			if(c == '}' || c == ']' || c == ',' || c == ' ' || c == '\r' || c == '\n') break;
			end++;
		}
		
		return new String(src.substring(start, end));
	}
	
	// 可以獲取peek方法對應的token，衹有tag為 STRING 與 CHAR_SEQUENCE 時有意義
	public String getToken() {
		return token;
	}
	
	// 当tag为num时，可以获取num对象，其实例可能为Integer，Float， BigDicimal
	public Object getNum() {
		return num;
	}
	
	public int getRowCount() {
		return this.rowCount;
	}
	
	public int getColCount() {
		return this.colCount;
	}
	
	// 根据tag吃掉当前的token，将指针后移
	public void swallow(TAG tag) {
		switch(tag) {
		case L_BRACE:
		case R_BRACE:
		case L_BRACKET:
		case R_BRACKET:
		case COLON:
		case COMMA:
		case SEMICOLON:
			pos++;
			colCount++;
			break;
		
		case STRING:
			pos = pos + token.length() + 2;				// 2个引号
			colCount = colCount + token.length() + 2;
			token = null;
			break;
			
		case NULL:
		case TRUE:
		case FALSE:
		case NUMBER:
			pos += token.length();
			colCount += token.length();
			token = null;
			break;
		
		default:
			break;
		}
	}

	// 打印当前解析的环境
	public void printEnv(OutputStream out) {
		try {
			byte[] bytes = src.getBytes();
			int len = 20;
			
			out.write("JSON string may goes error at ==>\r\n ".getBytes());
			out.write(bytes, (pos>20)?pos-20:0, (pos>20)?20:pos);
			out.write('^'); // anchor
			out.write(bytes, pos, (length-pos-1)<20?(length-pos):20);
			out.flush();
		} catch (IOException e) { // ignore
		}
	}
}
