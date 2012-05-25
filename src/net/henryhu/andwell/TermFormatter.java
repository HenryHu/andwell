package net.henryhu.andwell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.UnderlineSpan;

public class TermFormatter {
	private SpannableStringBuilder str;
	private Color color;
	private Style style;
	int curpos;
	abstract class Format {
		private int _start;
		Format()
		{
			_start = curpos;
		}
		public void start()
		{
			_start = curpos;
		}
		public abstract Object[] getSpan();
		public int getStart()
		{
			return _start;
		}
		public void finish()
		{
			if (curpos != _start)
			{
				Object[] spans = getSpan();
				for (int i=0; i<spans.length; i++)
					str.setSpan(spans[i], _start, curpos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
		}
	}
	
	static int makeColor(int r, int g, int b)
	{
		return 0xFF000000 | r << 16 | g << 8 | b;
	}

	static final int[] Colors = {
		makeColor(0, 0, 0),
		makeColor(170, 0, 0),
		makeColor(0, 170, 0),
		makeColor(170, 85, 0),
		makeColor(0, 0, 170),
		makeColor(170, 0, 170),
		makeColor(0, 170, 170),
		makeColor(170, 170, 170),
	};
	
	static final int[] BrightColors = {
		makeColor(85, 85, 85),
		makeColor(255, 85, 85),
		makeColor(85, 255, 85),
		makeColor(255, 255, 85),
		makeColor(85, 85, 255),
		makeColor(255, 85, 255),
		makeColor(85, 255, 255),
		makeColor(255, 255, 255),
	};
	
	class Color extends Format {
		private int _color;
		private boolean _bright;
		private int _back;
		private boolean _negative;
		private boolean _halfchar;
		static final int DEF_FORE_COLOR = 7;
		static final int DEF_BACK_COLOR = 0;
		Color(int color, int back)
		{
			_color = color;
			_back = back;
			_bright = false;
			_negative = false;
			_halfchar = false;
		}
		
		public int getTermColor(int color, boolean bright)
		{
			if (bright)
				return BrightColors[color];
			else
				return Colors[color];
		}
		
		void setBright(boolean bright)
		{
			if (bright != _bright)
			{
				finish();
				start();
				_bright = bright;
			}
		}
		
		void setNegative(boolean negative)
		{
			if (negative != _negative)
			{
				finish();
				start();
				_negative = negative;
			}
		}
		
		void setColor(int color)
		{
			if (color != _color)
			{
				finish();
				start();
				_color = color;
			}
		}
		
		void setBack(int back)
		{
			if (back != _back)
			{
				finish();
				start();
				_back = back;
			}
		}
		
		void halfChar()
		{
			_halfchar = true;
		}
		
		public Object[] getSpan()
		{
			int fore, back;
			int tfore, tback;
			if (_negative)
			{
				tfore = _back;
				tback = _color;
			} else {
				tfore = _color;
				tback = _back;
			}
			fore = getTermColor(tfore, _bright);
			back = getTermColor(tback, false);
			return new Object[] { new ForegroundColorSpan(fore),
					new BackgroundColorSpan(back) };
		}
		
	}	
	
	class Style extends Format {
		private boolean _underline;
		Style()
		{
			_underline = false;
		}
		public void setUnderline(boolean underline)
		{
			if (underline != _underline)
			{
				finish();
				start();
				_underline = underline;
			}
		}
		public Object[] getSpan()
		{
			if (_underline)
				return new Object[] { new UnderlineSpan() };
			else
				return new Object[] { };
		}
	}
	
	public void parseGraphicCommand(List<Integer> args)
	{
		if (args.size() == 0)
			args.add(0);
		for (int j=0; j<args.size(); j++)
		{
			int arg = args.get(j);
			if (arg == 0)
			{
				reset();
			} else
			if (arg == 1)
			{
				color.setBright(true);
			} else
			if (arg == 4)
			{
				style.setUnderline(true);
			} else
			if (arg == 7)
			{
				color.setNegative(true);
			} else
			if (arg >= 30 && arg <= 37)
			{
				color.setColor(arg - 30);
			} else
			if (arg >= 40 && arg <= 47)
			{
				color.setBack(arg - 40);
			} else
			if (arg == 50) {
				// half-char color
				color.halfChar();
			} else {
				// not supported
			}
		}

	}

	public void reset()
	{
		color.setColor(Color.DEF_FORE_COLOR);
		color.setBack(Color.DEF_BACK_COLOR);
		color.setBright(false);
		color.setNegative(false);
		style.setUnderline(false);
	}
	
    public SpannableStringBuilder parseFormat(String orig, boolean artMode)
    {
    	str = new SpannableStringBuilder();
    	curpos = 0;
    	
    	color = new Color(Color.DEF_FORE_COLOR, Color.DEF_BACK_COLOR);
    	style = new Style();
    	char lch = '\0';
    	boolean lastSet = false;
    	for (int i=0; i<orig.length(); i++)
    	{
    		boolean still = true;
    		char ch = orig.charAt(i);
    		if (lch == '\n')
    		{
    			if (ch == ':')
    			{
    				reset();
    				color.setColor(2);
    				lastSet = true;
    			} else
    			if (ch == '\u3010')
    			{
    				reset();
    				color.setBright(true);
    				color.setColor(3);
    				lastSet = true;
    			} else {
    				if (lastSet)
    					reset();
    			}
    		}
    		if (ch == 0x1b && i != orig.length() - 1 && 
    				orig.charAt(i+1) == '[') // ESCAPE
    		{
    			int end = -1;
    			char cmd = 'm';
    			for (int j=i+2; j<orig.length(); j++)
    			{
    				if ((orig.charAt(j) >= '@') && (orig.charAt(j) <= '~'))
    				{
    					end = j;
    					cmd = orig.charAt(j);
    					break;
    				}
    			}
    			if (end != -1)
    			{
    				still = false;
    				List<Integer> args = new ArrayList<Integer>();
    				{
    					StringBuilder arg = new StringBuilder();
    					for (int j=i+2; j<end; j++)
    					{
    						char xch = orig.charAt(j);
    						if (xch == ';')
    						{
    							if (arg.length() != 0)
    							{
    								args.add(Integer.parseInt(arg.toString()));
    								arg = new StringBuilder();
    							}
    						}
    						if (xch >= '0' && xch <= '9')
    							arg.append(xch);
    					}
    					if (arg.length() != 0)
    						args.add(Integer.parseInt(arg.toString()));
    				}
    				switch (cmd)
    				{
    				case 'm':
    					parseGraphicCommand(args);
    					break;
    				default:
    					// not supported
    				}
    				i = end;
    			}
    		}
    		if (still)
    		{
    			appendChar(ch, artMode);
    		}
    		lch = ch;
    	}
    	
    	color.finish();
    	style.finish();
    	
    	return str;
    }
    
    private final static HashMap<Character, String> repMap;
    private final static int[][] chnRange =
	{
    	{0x2460, 0x24ea},
//    	{0x25b2, 0x25e5},
		{0x2e80, 0x2fd5},
		{0x3000, 0x9fcb},
		{0xf900, 0xfad9},
		{0xfe30, 0xfe6b},
		{0xff01, 0xff5e},
	};
    
    static {
    	repMap = new HashMap<Character, String>();
    	repMap.put('\u221d', "oc");
    	repMap.put('\u221e', "oo");
    	repMap.put('\u2227', "/\\");
    	repMap.put('\u2228', "\\/");
    	repMap.put('\u2229', "\u256d\u256e");
    	repMap.put('\u222e', "S ");
    	repMap.put('\u223d', "un"); // horiz S
    	repMap.put('\u2261', "=="); // equivalence
    	repMap.put('\u2299', "()"); // big o
    	
    	repMap.put('\ufe35', "\u256d\u256e");
    	repMap.put('\ufe36', "\u2570\u256f");
    	repMap.put('\ufe37', "\u2572\u2571");
    	repMap.put('\ufe38', "\u2571\u2572");
    	repMap.put('\ufe39', "\u250c\u2510");
    	repMap.put('\ufe3a', "\u2514\u2518");
    	repMap.put('\ufe3b', "\u2552\u2555");
    	repMap.put('\ufe3e', "\\/");
    	repMap.put('\ufe40', "\u2572\u2571");
    	
    	repMap.put('\uff36', "\\/");
    	repMap.put('\uff3b', "[ ");
    	repMap.put('\uff3d', " ]");
    	repMap.put('\uff1c', "< ");
    	repMap.put('\uff1e', " >");
    	repMap.put('\uff0b', "\u2524\u251c"); // big +
    	repMap.put('\uffe3', "\u2594\u2594");
    	
    	// table
    	repMap.put('\u2502', "\u2502 ");
    	repMap.put('\u2503', "\u2502 ");
    	repMap.put('\u250c', "\u250c\u2500"); // table corner
    	repMap.put('\u2510', "\u2510 ");
    	repMap.put('\u2514', "\u2514\u2500");
    	repMap.put('\u2518', "\u2518 ");
    	
    	repMap.put('\u251c', "\u251c\u2500");
    	repMap.put('\u2524', "\u2524 ");
    	repMap.put('\u252c', "\u252c\u2500");
    	repMap.put('\u2534', "\u2534\u2500");
    	repMap.put('\u253c', "\u253c\u2500");
    	
    	// double-lined table
    	repMap.put('\u2551', "\u2551 ");
    	for (char ch = '\u2552'; ch <= '\u2563'; ch++)
    	{
    		switch((ch - '\u2552') % 6)
    		{
    		case 0:
    		case 2:
    			repMap.put(ch, new String() + ch + '\u2550');
    			break;
    		case 1:
    			repMap.put(ch, new String() + ch + '\u2500');
    			break;
    		default:
    			repMap.put(ch, new String() + ch + ' ');
    		}
    	}
    	
    	for (char ch = '\u2564'; ch <= '\u256c'; ch++)
    	{
    		switch ((ch - '\u2564') % 3)
    		{
    		case 0:
    		case 2:
    			repMap.put(ch, new String() + ch + '\u2550');
    			break;
    		case 1:
    			repMap.put(ch, new String() + ch + '\u2500');
    			break;
    		}
    	}
    	
    	repMap.put('\u256d', "\u256d\u2500"); // curly table corner
    	repMap.put('\u256e', "\u256e ");
    	repMap.put('\u256f', "\u256f ");
    	repMap.put('\u2570', "\u2570\u2500");
    	repMap.put('\u2571', " \u2571"); // /
    	repMap.put('\u2572', "\u2572 "); // \
    	
    	repMap.put('\u25e2', "\u25e2\u2588");
    	repMap.put('\u25e3', "\u2588\u25e3");
    	repMap.put('\u25e4', "\u2588\u25e4");
    	repMap.put('\u25e5', "\u25e5\u2588");
    	
    	repMap.put('\u3009', " >");
    	repMap.put('\u300d', " \u2510");
    	repMap.put('\u3013', "==");
    	
    	repMap.put('\u00a7', "\u00a7 ");
    	repMap.put('\u00b7', "\u00b7 ");
    	
    	repMap.put('\u5f50', "=]"); // exist
    	
    }
    
    boolean isChinese(char ch)
    {
    	for (int i=0; i<chnRange.length; i++)
    	{
    		if (ch >= chnRange[i][0] && ch <= chnRange[i][1])
    		{
    			return true;
    		}
    	}
    	return false;
    }
    
    void appendChar(char ch, boolean artMode)
    {
    	if (repMap.containsKey(ch))
    	{
    		str.append(repMap.get(ch));
    		curpos += repMap.get(ch).length();
    	} else {
			str.append(ch);
			curpos++;
			if (isChinese(ch))
			{
				if (artMode)
					str.setSpan(new RelativeSizeSpan((float) 1.26), curpos - 1, curpos,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if ((ch & 0xf000) == 0x2000)
			{
				str.append(ch);
				curpos++;
			} 
		}
    }

}
