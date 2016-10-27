package cn.via;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cn.via.Tokenizer.TAG;


/**
 * 一个java实现的简单的Json解析器。
 * @author venia
 */
public class TinyJsonParser {

	private static enum STATE {
		BEGIN, END,
		ARR_BEGIN, ARR_VALUE, ARR_VALUE_SEP, ARR_END,					// ARR is shorter for array, SEP is shorter for separator
		OBJ_BEGIN, MEM_NAME, MEM_NAME_SEP, MEM_VALUE, MEM_SEP, OBJ_END  // OBJ is shorter for object, MEM is shorter for member
	}
	
	// 上下文状态
	private static final class Context {
		
		private Object root = null;		// 根结点，只能是map或者list, object <==> map, array <==> list
		private STATE state = null;		// 状态
		private Stack<Object> stack;	// 栈，保存解析过程中的嵌套关系
		private Tokenizer tkz;
		
		private String memName;			// 解析对象过程中，保存键值对的键值
		
		public Context(Tokenizer k) {
			this.state = STATE.BEGIN;
			this.stack = new Stack<>();
			this.tkz = k;
		}
		
		public void setRoot(Object obj) {
			if(this.root != null)
				throw new IllegalStateException("Root had been setted!");
			
			if(!(obj instanceof List) && !(obj instanceof Map))
				throw new IllegalArgumentException("Root must be an array or a map!");
			
			this.root = obj;
			this.stack.push(obj);
		}
		
		public Object getRoot() {
			return this.root;
		}
		
		public STATE getState() {
			return this.state;
		}
		
		public void setState(STATE s) {
			this.state = s;
		}
		
		public void setMemName(String name) {
			this.memName = name;
		}
		
		public String getMemName() {
			return this.memName;
		}
		
		@SuppressWarnings("unchecked")
		public void setValueMember(String name, Object value) {
			if (name == null || name.length() == 0)
				throw new IllegalArgumentException("Name must not be empty!");
			
			if (!(stack.peek() instanceof Map))
				throw new IllegalStateException("Can only set a pair to a map!");
			
			((Map<String, Object>) stack.peek()).put(name, value);
		}
		
		@SuppressWarnings("unchecked")
		public void addValueItem(Object value) {
			if (!(stack.peek() instanceof List))
				throw new IllegalStateException("Can only add a value to an array"); 
			
			((List<Object>) stack.peek()).add(value);
		}
		
		@SuppressWarnings("unchecked")
		public void newObjectMember(String name, Map<Object, Object> map) {
			if (name == null || name.length() == 0)
				throw new IllegalArgumentException("Name must not be empty!");
			
			((Map<String, Object>) stack.peek()).put(name, map);
			stack.push(map);
		}
		
		@SuppressWarnings("unchecked")
		public void newObjectItem(Map<Object, Object> map) {
			((List<Object>) stack.peek()).add(map);
			stack.push(map);
		}
		
		@SuppressWarnings("unchecked")
		public void newArrayMember(String name, List<Object> list) {
			if (name == null || name.length() == 0)
				throw new IllegalArgumentException("Name must not be empty!");
			
			((Map<String, Object>) stack.peek()).put(name, list);
			stack.push(list);
		}
		
		@SuppressWarnings("unchecked")
		public void newArrayItem(List<Object> list) {
			((List<Object>) stack.peek()).add(list);
			stack.push(list);
		}
		
		public void closeObject() {
			if (stack.size() == 0)
				throw new RuntimeException("Bad gramma at row:"
						+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
			
			stack.pop();
		}
		
		public Object getWorkingObject() {
			if (stack.size() == 0)
				throw new RuntimeException("Bad gramma at row:"
						+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
			
			return stack.peek();
		}
		
		public boolean isStackEmpty() {
			return stack.size() == 0;
		}
	}
	
	
	// 主解析方法
	public static final Object parse(String source) {
		Tokenizer tkz = new Tokenizer(source);
		Context ctx = new Context(tkz);
		return parse(ctx, tkz);
	}
	
	// 自动状态机
	private static Object parse(Context ctx, Tokenizer tkz) {
		TAG tag = null;
		while ((tag = tkz.peek()) != Tokenizer.TAG.EOF) {
			
			switch (ctx.getState()) {
			case BEGIN:
				if (tag == TAG.L_BRACE) {
					ctx.setRoot(new HashMap<>());
					ctx.setState(STATE.OBJ_BEGIN);
				}
				else if (tag == TAG.L_BRACKET) {
					ctx.setRoot(new ArrayList<>());
					ctx.setState(STATE.ARR_BEGIN);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a '{' or '[' at row:" +
							tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case END: // do nothing
				break;
				
				
			case ARR_BEGIN:
				if (tag == TAG.L_BRACE) {
					ctx.newObjectItem(new HashMap<>());
					ctx.setState(STATE.OBJ_BEGIN);
				}
				else if (tag == TAG.L_BRACKET) {
					ctx.newArrayItem(new ArrayList<>());
					ctx.setState(STATE.ARR_BEGIN);
				}
				else if (tag == TAG.R_BRACKET) {
					ctx.closeObject();
					ctx.setState(STATE.ARR_END);
				}
				else if (tag == TAG.STRING) {
					ctx.addValueItem(tkz.getToken());
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.NULL) {
					ctx.addValueItem(null);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.TRUE) {
					ctx.addValueItem(Tokenizer.TRUE);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.FALSE) {
					ctx.addValueItem(Tokenizer.FALSE);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.NUMBER) {
					ctx.addValueItem(tkz.getNum());
					ctx.setState(STATE.ARR_VALUE);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expect a string or a number or a object at row:"
							+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case ARR_VALUE:
				if (tag == TAG.COMMA) {
					ctx.setState(STATE.ARR_VALUE_SEP);
				}
				else if (tag == TAG.R_BRACKET) {
					ctx.closeObject();
					ctx.setState(STATE.ARR_END);
				} else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expect a \",\" or a \"]\" or a object at row:"
							+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case ARR_VALUE_SEP:
				if (tag == TAG.L_BRACKET) {
					ctx.newArrayItem(new ArrayList<>());
					ctx.setState(STATE.ARR_BEGIN);
				}
				else if (tag == TAG.L_BRACE) {
					ctx.newObjectItem(new HashMap<>());
					ctx.setState(STATE.OBJ_BEGIN);
				}
				else if (tag == TAG.STRING) {
					ctx.addValueItem(tkz.getToken());
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.NULL) {
					ctx.addValueItem(null);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.TRUE) {
					ctx.addValueItem(Tokenizer.TRUE);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.FALSE) {
					ctx.addValueItem(Tokenizer.FALSE);
					ctx.setState(STATE.ARR_VALUE);
				}
				else if (tag == TAG.NUMBER) {
					ctx.addValueItem(tkz.getNum());
					ctx.setState(STATE.ARR_VALUE);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expect a string or a number or a object at row:"
							+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case ARR_END:
				if (tag == TAG.R_BRACKET) {
					ctx.closeObject();
					ctx.setState(STATE.ARR_END);
				}
				else if (tag == TAG.R_BRACE) {
					ctx.closeObject();
					ctx.setState(STATE.OBJ_END);
				}
				else if (tag == TAG.COMMA) {
					Object o = ctx.getWorkingObject();
					if (o instanceof List) {
						ctx.setState(STATE.ARR_VALUE_SEP);
					}
					else {
						ctx.setState(STATE.MEM_SEP);
					}
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a ']' or '}' at row:"
							+ tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case OBJ_BEGIN:
				if (tag == TAG.STRING) {
					ctx.setMemName(tkz.getToken());
					ctx.setState(STATE.MEM_NAME);
				}
				else if (tag == TAG.R_BRACE) {
					ctx.closeObject();
					ctx.setState(STATE.OBJ_END);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a \"string\" at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case MEM_NAME:
				if (tag == TAG.COLON) {
					ctx.setState(STATE.MEM_NAME_SEP);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a \":\" at at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case MEM_NAME_SEP:
				if (tag == TAG.L_BRACKET) {
					ctx.newArrayMember(ctx.getMemName(), new ArrayList<>());
					ctx.setState(STATE.ARR_BEGIN);
				}
				else if (tag == TAG.L_BRACE) {
					ctx.newObjectMember(ctx.getMemName(), new HashMap<>());
					ctx.setState(STATE.OBJ_BEGIN);
				}
				else if (tag == TAG.STRING) {
					ctx.setValueMember(ctx.getMemName(), tkz.getToken());
					ctx.setState(STATE.MEM_VALUE);
				}
				else if (tag == TAG.NULL) {
					ctx.setValueMember(ctx.getMemName(), null);
					ctx.setState(STATE.MEM_VALUE);
				}
				else if (tag == TAG.FALSE) {
					ctx.setValueMember(ctx.getMemName(), Tokenizer.FALSE);
					ctx.setState(STATE.MEM_VALUE);
				}
				else if (tag == TAG.TRUE) {
					ctx.setValueMember(ctx.getMemName(), Tokenizer.TRUE);
					ctx.setState(STATE.MEM_VALUE);
				}
				else if (tag == TAG.NUMBER) {
					ctx.setValueMember(ctx.getMemName(), tkz.getNum());
					ctx.setState(STATE.MEM_VALUE);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a value at at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case MEM_VALUE:
				if (tag == TAG.R_BRACE) {
					ctx.closeObject();
					ctx.setState(STATE.OBJ_END);
				}
				else if (tag == TAG.COMMA) {
					ctx.setState(STATE.MEM_SEP);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a \",\" or \"}\" at at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
				
			case MEM_SEP:
				if (tag == TAG.STRING) {
					ctx.setMemName(tkz.getToken());
					ctx.setState(STATE.MEM_NAME);
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Expected a \"string\" at at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
				
			case OBJ_END:
				if (tag == TAG.R_BRACKET) {
					ctx.closeObject();
					ctx.setState(STATE.ARR_END);
				}
				else if (tag == TAG.R_BRACE) {
					ctx.closeObject();
					ctx.setState(STATE.OBJ_END);
				}
				else if (tag == TAG.COMMA) {
					Object o = ctx.getWorkingObject();
					if (o instanceof List) {
						ctx.setState(STATE.ARR_VALUE_SEP);
					}
					else {
						ctx.setState(STATE.MEM_SEP);
					}
				}
				else {
					tkz.printEnv(System.out);
					throw new RuntimeException("Bad gramma at at row:" + tkz.getRowCount() + ", col:" + tkz.getColCount() + ".");
				}
				break;
			}
			
			tkz.swallow(tag);
		}
		
		if (!ctx.isStackEmpty()) {
			throw new RuntimeException("The object or array is not closed.");
		}
		
		return ctx.getRoot();
	}
	
	/**
	 * 将root对象转换为可以打印的字符串
	 * @param root
	 * @return
	 */
	public static String getPrintString(Object root) {
		if(root == null) return "";
		
		if(root instanceof List) {
			return convertListToString((List<?>)root, "");
		}
		else {
			return convertMapToString((Map<?, ?>)root, "");
		}
	}
	
	private static String convertMapToString(Map<?, ?> map, String tap) {
		StringBuilder sb = new StringBuilder();
		String innertap = tap+"\t";
		
		sb.append("{");
		map.forEach((k,v) -> {
			sb.append("\r\n").append(innertap).append(k).append(":");
			
			if(v instanceof Map) sb.append(convertMapToString((Map<?, ?>)v, innertap));
			else if(v instanceof List) sb.append(convertListToString((List<?>)v, innertap));
			else sb.append(v.toString());
			
			sb.append(',');
		});
		
		if(sb.charAt(sb.length()-1) == ',') {
			sb.deleteCharAt(sb.length()-1);
			sb.append("\r\n").append(tap).append("}");
			return sb.toString();
		} else {
			return sb.append("}").toString();
		}
	}

	private static String convertListToString(List<?> list, String tap) {
		StringBuilder sb = new StringBuilder();
		String innertap = tap+"\t";
		
		sb.append("[");
		list.forEach(i -> {
			sb.append("\r\n").append(innertap);
			
			if(i instanceof Map) sb.append(convertMapToString((Map<?, ?>)i, innertap));
			else if(i instanceof List) sb.append(convertListToString((List<?>)i, innertap));
			else sb.append(i.toString());
			
			sb.append(',');
		});
		
		if(sb.charAt(sb.length()-1) == ',') {
			sb.deleteCharAt(sb.length()-1);
			sb.append("\r\n").append(tap).append("]");
			return sb.toString();
		} else {
			return sb.append("]").toString();
		}
	}
	
}
