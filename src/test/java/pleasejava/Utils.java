package pleasejava;

import static com.google.common.collect.FluentIterable.from;

import java.util.Map;

import org.junit.Assert;
import org.junit.ComparisonFailure;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

/**
 * @author Tomas Zalusky
 */
public class Utils {

	private static final String LS = String.format("%n");

	public static void main(String[] args) {
		System.out.println("Hello world");
	}
	
	/**
	 * Appends padding at the beginning of each line of multilineText.
	 * @param multiLineText
	 * @param padding appended string, usually \t or space(s)
	 * @return string with applied change
	 */
	public static String indent(String multiLineText, String padding) {
		return multiLineText.replaceAll("([^\\n]+)",padding + "$1");
	}

	/**
	 * Equivalent to buf.append(String.format(format,args)).
	 * @param buf
	 * @param format
	 * @param args
	 */
	public static StringBuilder appendf(StringBuilder buf, String format, Object... args) {
		String s = String.format(format, args);
		buf.append(s);
		return buf;
	}

	public static <V> Function<Map.Entry<?,V>,V> _mapEntryValue() {
		return new Function<Map.Entry<?,V>,V>() {
			public V apply(Map.Entry<?,V> input) {
				return input.getValue();
			}
		};
	}

	/**
	 * Like {@link Assert#assertEquals(Object, Object)},
	 * except throwing {@link ComparisonFailure} also for non-String objects.
	 * This lets Eclipse to visualize differences when clicking on error in JUnit view.
	 * @param expected
	 * @param actual
	 */
	public static void assertEquals(Object expected, Object actual) {
		try {
			Assert.assertEquals(expected, actual);
		} catch (AssertionError e) {
			throw new ComparisonFailure(e.getMessage(),
					String.valueOf(expected),
					String.valueOf(actual)
			);
		}
	}
	
	/**
	 * Like {@link Assert#assertEquals(String, Object, Object)},
	 * except throwing {@link ComparisonFailure} also for non-String objects.
	 * This lets Eclipse to visualize differences when clicking on error in JUnit view.
	 * @param expected
	 * @param actual
	 */
	public static void assertEquals(String message, Object expected, Object actual) {
		try {
			Assert.assertEquals(message, expected, actual);
		} catch (AssertionError e) {
			throw new ComparisonFailure(e.getMessage(),
					String.valueOf(expected),
					String.valueOf(actual)
			);
		}
	}

	/**
	 * Helper ancestor class for toString method of hierarchical structures.
	 * Supports indendation and resulting buffer.
	 * (I consider adding more logic premature as currently there are only 2 ToString implementations.)
	 * @author Tomas Zalusky
	 */
	public static abstract class ToStringSupport {
		
		protected static final int TAB_SPACES = 4;
		
		protected final StringBuilder buf;
		
		public ToStringSupport(StringBuilder buf) {
			this.buf = buf;
		}

		protected static String indent(int level) {
			return Strings.repeat(" ",level * TAB_SPACES);
		}

		@Override
		public String toString() {
			return buf.toString();
		}

	}
	
	/**
	 * Formats content of given buffer to table columns
	 * based on occurence of certain characters in text:
	 * <pre>
	 * first column "second column in quotes" #third column after hash
	 * </pre>
	 * @param buf
	 */
	public static void align(StringBuilder buf) {
		final int COLS = 3;
		final Function<String,String[]> lineSplitter = new Function<String,String[]>() {
			public String[] apply(String input) {
				String[] result = new String[COLS];
				int f = 0;
				int q1 = input.indexOf('"',f);
				if (q1 == -1) {
					result[0] = result[1] = "";
				} else {
					result[0] = CharMatcher.WHITESPACE.trimTrailingFrom(input.substring(0,q1));
					int q2 = input.indexOf('"',q1 + 1);
					result[1] = CharMatcher.WHITESPACE.trimTrailingFrom(input.substring(q1,q2 + 1));
					f = q2 + 1;
				}
				int h1 = input.indexOf('#',f);
				if (h1 == -1) {
					result[2] = "";
				} else {
					result[2] = CharMatcher.WHITESPACE.trimTrailingFrom(input.substring(h1));
				}
				return result;
			}
		};
		final int[] maximums = new int[COLS];
		Iterable<String> lines = Splitter.onPattern("\\r?\\n").split(buf);
		for (String line : lines) {
			String[] parts = lineSplitter.apply(line);
			for (int i = 0; i < COLS; i++) maximums[i] = Math.max(maximums[i], parts[i].length());
		}
		StringBuilder result = Joiner.on(LS).appendTo(new StringBuilder(), from(lines).transform(new Function<String,String>() {
			public String apply(String line) {
				StringBuilder result = new StringBuilder();
				String[] parts = lineSplitter.apply(line);
				for (int i = 0; i < COLS; i++) appendf(result,"%s%s ",parts[i], Strings.repeat(" ",maximums[i] - parts[i].length()));
				return CharMatcher.WHITESPACE.trimTrailingFrom(result).toString();
			}
		}));
		buf.setLength(0);
		buf.append(result);
	}
	
}
