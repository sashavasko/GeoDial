/**
 * 
 */
package com.geodial;

import java.util.ArrayList;
import java.util.List;

import android.telephony.PhoneNumberUtils;
import android.util.Log;

/**
 * @author sasha
 *
 */
public class DialScript {
	private final static String TAG = "DialScript"; 
	public interface Item {
		public StringBuilder appendTo (StringBuilder sb);
		public Item add(Item other);
		public void appendChar(char c);
		public boolean backspace();
	}
	
	private List<Item> script = new ArrayList<Item>();
	
	public int length() {
		return script.size();
	}
	
	public class DialNumber implements Item {
		/**
		 * @param number
		 */
		public DialNumber(String number) {
			this.number = parseNumberToken(number,0);
		}

		String number;

		@Override
		public String toString() {
			return number;
		}

		@Override
		public StringBuilder appendTo(StringBuilder sb) {
			return sb.append(number);
		}

		@Override
		public Item add(Item item) {
			if (item instanceof DialNumber)
				this.number += ((DialNumber)item).number;
			return this;
		}

		@Override
		public void appendChar(char c) {
			number += c;
		}

		@Override
		public boolean backspace() {
			if (number.length() <= 1)
				return true;
			number = number.substring(0, number.length() - 1);
			return false;
		}
	}
	
	public class DialPause implements Item {
		/**
		 * @param length
		 */
		public DialPause(int length) {
			this.length = length;
		}

		int length;

		@Override
		public String toString() {
			return appendTo(new StringBuilder()).toString ();
		}
		
		@Override
		public StringBuilder appendTo (StringBuilder sb) {
			for (int i = 0 ; i < length ; i++)
				sb.append(PhoneNumberUtils.PAUSE);
			return sb;
		}
		
		public int increment (int change) {
			return length += change;
		}

		@Override
		public Item add(Item item) {
			if (item instanceof DialPause)
				this.length += ((DialPause)item).length;
			return this;
		}

		@Override
		public void appendChar(char c) {
			//if (Character.isDigit(c) && c != '0')	length = c - '0';
		}

		@Override
		public boolean backspace() {
			return (--length <= 0);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Item e : script)
			e.appendTo(sb);
		return sb.toString();
	}
	
	public static boolean isNumberChar (int code) {
		return Character.isDigit(code) || code == '-' 
				|| code == '(' || code == ')' || code == ' '
				|| code == '*' || code == '#';
		
	}

	public void parseString (String str, boolean appendHash) {
		int pos = 0 ; 
		int maxPos = str.codePointCount(0, str.length());
		try {
			while (pos < maxPos){
				int parsed = isNumberChar(str.codePointAt(pos))?
								parseNumber(str, pos) : parsePause(str, pos);
				Log.d(TAG, "string [" + str + "], pos = " + pos + " parsed = " + parsed);
				if (parsed == 0)
					return;
				pos += parsed;
			}
		} catch (IndexOutOfBoundsException e) {}
		if (appendHash){
			Item item = getLast ();
			if (item instanceof DialPause)
				script.add(new DialNumber("#"));
			else
				item.add(new DialNumber("#"));
		}
	}

	private int parsePause(String str, int pos) {
		switch (str.codePointAt(pos)) {
			case 'P' :
			case 'p' :
			case 'C' :
			case 'c' :
			case 'X' :
			case 'x' : 	addPause(2);
						return 1;
			case 'W' :
			case 'w' : 	addPause(1);
						return 1;
		}
		return 0;
	}

	protected Item getItem (int idx) {
		int scriptSize = script.size();
		if (idx < 0)
			idx = scriptSize-1;
			
		return 	scriptSize > idx ? script.get(idx) : null;
	}
	
	protected Item getLast () {
		return getItem (-1); 
	}
	
	public int addPause(int selection, int duration) {
		Log.d(TAG, "addPause "+duration);
		Item item = getItem (selection);
		if (item != null && !(item instanceof DialPause) && selection+1 <length() ) {
			item = getItem (++selection);
		}
		if (item != null && item instanceof DialPause) {
			((DialPause)item).increment (duration);
			return selection;
		} 

		script.add(new DialPause(duration));
		return length()-1;
	}	
	
	protected void addPause (int length) {
		addPause (-1, length);
	}

	protected int addNumber (String number) {
		Log.d(TAG, "addNumber "+number);
		if (!script.isEmpty() && !(getLast() instanceof DialPause))
			addPause(1);
		script.add(new DialNumber (number));
		return length()-1;
	}

	protected int addNumberFront (String number) {
		Log.d(TAG, "addNumberFront "+number);
		script.add(0, new DialNumber (number));
		if (script.size() > 1 && !(script.get(1) instanceof DialPause))
			addPause(0, 1);
		return 0;
	}
	
	private int parseNumber(String str, int pos) {
		int start = pos;
		try {
			while (isNumberChar(str.codePointAt(pos))) ++pos;
		} catch (IndexOutOfBoundsException e) {}
		if (start < pos){ // can use substring since we are dealing with digits which are 1 byte long
			addNumber(str.substring(start, pos));
		}
		return pos - start;
	}	

	public static String parseNumberToken(String str, int pos) {
		int start = pos;
		try {
			while (isNumberChar(str.codePointAt(pos))) ++pos;
		} catch (IndexOutOfBoundsException e) {}
		if (start < pos){ // can use substring since we are dealing with digits which are 1 byte long
			return str.substring(start, pos);
		}
		return null;
	}

	public void removeItem(Item item) {
		int idx = script.indexOf (item);
		Item prev = idx > 0 ? script.get(idx-1) : null;
		Item next = idx+1 < script.size() ? script.get(idx+1) : null;
		if (prev != null && next != null) {
			prev = prev.add(next);
			script.remove(idx+1);
		}
		script.remove(idx);
	}

	
}
