package pleasejava.tools;

import static com.google.common.collect.FluentIterable.from;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.hamcrest.Description;
import org.jdom2.input.JDOMParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.ImmutableList;

/**
 * Testing correct structure of type graph.
 * Helper methods t() infer their test data from name of caller method.
 * 
 * @author Tomas Zalusky
 */
@RunWith(JUnit4.class)
public class TypeDependencyGraphTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Helper method, expects exception of given type with message containing given string.
	 * @param exceptionClass
	 * @param expectedMessage
	 */
	private void t(Class<? extends Exception> exceptionClass, String expectedMessage) {
		exception.expect(exceptionClass);
		exception.expectMessage(expectedMessage);
		t();
	}
	
	/**
	 * Helper method, expects nodes in given topological ordering specified by their names.
	 * @param expectedNames
	 */
	private void t(String... expectedNames) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String methodName = stackTrace[2].getMethodName();
		if ("t".equals(methodName)) {
			methodName = stackTrace[3].getMethodName();
		}
		String fileSubpath = String.format("tdg/%s.xml",methodName);
		try (InputStream is = TypeDependencyGraphTest.class.getResourceAsStream(fileSubpath)) {
			TypeDependencyGraph graph = TypeDependencyGraph.createFrom(is);
			List<Type> actual = graph.getTopologicalOrdering();
			List<String> actualNames = from(actual).transform(Type._getName).toList();
			assertEquals(ImmutableList.copyOf(expectedNames), actualNames);
			
			//System.out.println(graph);
			System.out.println(actual.get(0).toTypeNode(null));
		} catch (IOException e) {
		}
	}
	
	@Test public void simple() {t("a_test_package.var1","integer");}
	
	@Test public void dag1() {t("main","a","d","b","c","e","f","g","h","i","varchar2(100)");}
	
	@Test public void alltypes() {t(
			"echo",
			"a_test_package.ibt1",
			"a_test_package.var1",
			"a_test_package.nst2",
			"a_test_package.nst3",
			"a_test_package.rec1",
			"a_test_package.var3",
			"a_test_package.ibt2",
			"a_test_package.nst1",
			"a_test_package.ibt3",
			"a_test_package.var2",
			"clob",
			"a_test_package.rec2",
			"a_test_package.var4",
			"a_test_package.nst4",
			"a_test_package.ibt4",
			"binary_integer",
			"a_test_package.ibt5",
			"a_test_package.nst5",
			"pls_integer",
			"a_test_package.rec3",
			"number(30)",
			"a_test_package.var5",
			"varchar2(200)",
			"boolean",
			"integer",
			"varchar2(100)"
	/* 
	types:
	                    ibt1                  var1    nst2  nst3       ----rec1--------  var3  ibt2  nst1  ibt3      var2  clob
	                      |                      \    /        \      /      |    |    \   |    |    /         \    /    
	        ------------rec2-----------------     var4          -nst4-     ibt4  bi     -----ibt5----           nst5
	       /         |          |            \      |          /             |                 |                  |        
	   var5        rec3         |             ----rec3---------              |               var5                 |       
	    |          ...          |            /                 \             |                ...                 |        
	varchar2(100)          pls_integer  boolean             integer      number(30)                         varchar2(200)  
	 */
	);}

	@Test public void invalidType() {
		t(UndeclaredTypeException.class,"'nonexisting'");
	}

	@Test public void invalidPlsqlConstruct() {
		t(InvalidPlsqlConstructException.class,"'nonexisting'");
	}
	
	@Test public void invalidDependencies() {
		t(TypeCircularityException.class,"'nst1'");
	}
	
	@Test public void invalidXml1() {
		t(InvalidXmlException.class,"'empty record'");
	}
	
	@Test public void invalidXml2() {
		t(InvalidXmlException.class,"'missing attribute name'");
	}
	
	@Test public void invalidXml3() {
		t(InvalidXmlException.class,"'invalid parameter mode foo'");
	}
	
	@Test public void invalidXml4() {
		exception.expect(RuntimeException.class);
		exception.expect(new TypeSafeMatcher<Exception>() {
			public void describeTo(Description description) {}
			@Override
			public boolean matchesSafely(Exception item) {
				return item.getCause() instanceof JDOMParseException;
			}
		});
		t();
	}
	
}
