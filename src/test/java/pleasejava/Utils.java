package pleasejava;

import java.util.Map;

import org.junit.Assert;
import org.junit.ComparisonFailure;

import com.google.common.base.Function;
import com.google.common.base.Strings;

/**
 * @author Tomas Zalusky
 */
public class Utils {

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
	
}
