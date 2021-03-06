package plsql;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jdom2.input.JDOMParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import pleasejava.Utils;

/**
 * Tests correct loading of type graph.
 * @author Tomas Zalusky
 */
@RunWith(Parameterized.class)
public class TypeGraphLoadTest extends AbstractTypeGraphTest {

	private static boolean record = false;

	private final String graphName;
	
	private final String expected;

	private final Class<? extends Exception> exceptionClass;
	
	private final Object expectedMessageOrMatcher;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	public TypeGraphLoadTest(String graphName, Class<? extends Exception> exceptionClass, Object expectedMessageOrMatcher) throws IOException {
		this.graphName = graphName;
		this.expected = record ? null : (exceptionClass == null ? readExpectedOutput(getClass(),graphName) : null);
		this.exceptionClass = exceptionClass;
		this.expectedMessageOrMatcher = expectedMessageOrMatcher;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"simple"               , null                                , null},
				{"dag1"                 , null                                , null},
				{"alltypes"             , null                                , null},
				{"toplevel"             , null                                , null},
				{"javatype"             , null                                , null},
				{"invalidType"          , UndeclaredTypeException.class       , "'nonexisting'"},
				{"invalidPlsqlConstruct", InvalidPlsqlConstructException.class, "'nonexisting'"},
				{"invalidDependencies"  , TypeCircularityException.class      , "'nst1'"},
				{"invalidXml1"          , InvalidXmlException.class           , "'empty record'"},
				{"invalidXml2"          , InvalidXmlException.class           , "'missing attribute name'"},
				{"invalidXml3"          , InvalidXmlException.class           , "'invalid parameter mode foo'"},
				{"invalidXml4"          , RuntimeException.class              , new TypeSafeMatcher<Exception>() {
					public void describeTo(Description description) {}
					@Override
					public boolean matchesSafely(Exception item) {
						return item.getCause() instanceof JDOMParseException;
					}
				}},
		});
	}
	
	@Test
	public void test() throws IOException {
		if (exceptionClass != null) {
			exception.expect(exceptionClass);
			if (expectedMessageOrMatcher instanceof String) {
				exception.expectMessage((String)expectedMessageOrMatcher);
			} else {
				exception.expect((Matcher<?>)expectedMessageOrMatcher);
			}
			loadGraph(graphName);
		} else {
			TypeGraph typeGraph = loadGraph(graphName);
			String actual = typeGraph.toString();
			if (record) {
				writeExpectedOutput(getClass(),graphName,actual);
			} else {
				Utils.assertEquals(expected,actual);
			}
		}
	}
	
}
