package cn.via.test;

import org.junit.Test;

import cn.via.Tokenizer;
import cn.via.Tokenizer.TAG;

/**
 * @author venia
 */
public class TestTokenizer {
	
	public static final String TEST_STR = 
			"{ " +
		    "\"name\": \"JSON中国\","+
		    "\"url\": \"http://www.json.org.cn\", "+
		    "\"page\": 88, "+
		    "\"isNonProfit\": true,"+
		    "\"address\": {"+
		    "    \"street\": \"浙大路38号.\","+
		    "    \"city\": \"浙江杭州\","+
		    "    \"country\": \"中国\""+
		    "},"+
		    "\"links\": ["+
		    "    {"+
		    "        \"name\": \"Google\","+
		    "        \"url\": \"http://www.google.com\""+
		    "    },"+
		    "    {"+
		    "        \"name\": \"Baidu\","+
		    "        \"url\": \"http://www.baidu.com\""+
		    "    },"+
		    "    {"+
		    "        \"name\": \"SoSo\","+
		    "        \"url\": \"http://www.SoSo.com\""+
		    "    }"+
		    "]"+
		"}";
	
	
	@Test
	public void test() {
			Tokenizer tkz = new Tokenizer(TEST_STR);
			TAG t = null;
			while((t = tkz.peek()) != TAG.EOF) {
				switch(t) {
				case TRUE:
					System.out.println(tkz.getToken());
					break;
					
				case FALSE:
					System.out.println(tkz.getToken());
					break;
					
				case NULL:
					System.out.println("null");
					break;
					
				case NUMBER:
					System.out.println(tkz.getNum());
					
					
				case COLON:
					System.out.println(":");
					break;
					
				case COMMA:
					System.out.println(",");
					break;
					
				case L_BRACE:
					System.out.println("{");
					break;
					
				case L_BRACKET:
					System.out.println("[");
					break;
					
				case R_BRACE:
					System.out.println("}");
					break;
					
				case R_BRACKET:
					System.out.println("]");
					break;
					
				case SEMICOLON:
					System.out.println(":");
					break;
					
				case STRING:
					System.out.println(tkz.getToken());
					break;
				}
				
				tkz.swallow(t);
			}
		}

}
