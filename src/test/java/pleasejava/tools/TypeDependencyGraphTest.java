package pleasejava.tools;

import static com.google.common.collect.FluentIterable.from;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
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
public class TypeDependencyGraphTest extends AbstractTypeGraphTest {

	/**
	 * Helper method, expects nodes in given topological ordering specified by their names.
	 * @param expectedNames
	 * @throws IOException 
	 */
	private void t(String... expectedNames) throws IOException {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String methodName = stackTrace[2].getMethodName();
		if ("t".equals(methodName)) {
			methodName = stackTrace[3].getMethodName();
		}
		TypeDependencyGraph graph = loadGraph(methodName);
		List<Type> actual = graph.getTopologicalOrdering();
		List<String> actualNames = from(actual).transform(Type._getName).toList();
		assertEquals(ImmutableList.copyOf(expectedNames), actualNames);
		
		//System.out.println(graph);
		System.out.println(actual.get(0).toTypeNode(null));
	}

	@Test public void simple() throws IOException {t("a_test_package.var1","integer");}
	
	@Test public void dag1() throws IOException {t("main","a","d","b","c","e","f","g","h","i","varchar2(100)");}
	
	@Test public void alltypes() throws IOException {t(
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

}
