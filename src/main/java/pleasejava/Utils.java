package pleasejava;

import static com.google.common.base.CharMatcher.WHITESPACE;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.ComparisonFailure;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
	 * Helper ancestor class for toString method
	 * where hierarchical structures are formatted into table-like format.
	 * Offers fluent API facilitating addition of strings into rows and columns.
	 * @author Tomas Zalusky
	 */
	public static abstract class ToStringSupport {
		
		protected static final int TAB_SPACES = 4;
		
		protected final List<List<String>> table;
		
		public ToStringSupport() {
			this.table = Lists.newArrayList();
			//newLine().append("");
		}

		protected static String indent(int level) {
			return Strings.repeat(" ",level * TAB_SPACES);
		}

		/**
		 * Appends new empty row in the end of table.
		 * Prevents addition of new lines into empty table.
		 * @return this
		 */
		public ToStringSupport newLine() {
			if (!table.isEmpty()) {
				table.add(Lists.<String>newArrayList());
			}
			return this;
		}

		/**
		 * Replaces content of last cell in last row with the concatenation of old content and appendee
		 * (i.e. doesn't change number of cells in table, only content)
		 * @param appendee appended string
		 * @return this
		 */
		public ToStringSupport appendToLastCell(String appendee) {
			List<String> lastRow = Iterables.getLast(table,null);
			if (lastRow == null) {
				table.add(lastRow = Lists.<String>newArrayList(""));
			}
			int index = lastRow.size() - 1;
			append(lastRow.remove(index) + appendee);
			return this;
		}
		
		/**
		 * Appends string as new cell at the end of last row of table.
		 * Creates new row if no such exists.
		 * @param cell
		 * @return
		 */
		public ToStringSupport append(String cell) {
			List<String> lastRow = Iterables.getLast(table,null);
			if (lastRow == null) {
				table.add(lastRow = Lists.<String>newArrayList());
			}
			lastRow.add(cell);
			return this;
		}
		
		/**
		 * Converts table into string. All columns are separated by single space.
		 * Columns with zero width are ignored.
		 * Extra whitespace is removed from right (from left are retained).
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder();
			int size = table.size();
			List<Integer> maximums = Lists.newArrayList();
			for (int r = 0; r < size; r++) {
				List<String> row = table.get(r);
				if (row.size() > maximums.size()) { // array of maximal lengths must not be shorter than current row
					maximums.addAll(Collections.nCopies(row.size() - maximums.size(),0));
				}
				for (int c = 0; c < row.size(); c++) {
					String cell = WHITESPACE.trimTrailingFrom(row.get(c));
					maximums.set(c, Math.max(maximums.get(c), cell.length()));
				}
			}
			for (int r = 0; r < size; r++) {
				List<String> row = table.get(r);
				StringBuilder rowBuf = new StringBuilder();
				for (int c = 0; c < row.size(); c++) {
					String cell = WHITESPACE.trimTrailingFrom(row.get(c));
					int max = maximums.get(c);
					if (max > 0) { // empty columns are ignored
						appendf(rowBuf,"%s%s ",cell, Strings.repeat(" ",max - cell.length()));
					}
				}
				appendf(b, (r == 0 ? "" : "%n") + "%s", WHITESPACE.trimTrailingFrom(rowBuf));
			}
			return b.toString();
		}

	}
	
}
