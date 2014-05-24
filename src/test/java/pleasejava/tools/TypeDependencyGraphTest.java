package pleasejava.tools;

import static com.google.common.collect.FluentIterable.from;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import pleasejava.tools.TypeDependencyGraph.TypeNode;

import com.google.common.collect.ImmutableList;

/**
 * @author Tomas Zalusky
 */
public class TypeDependencyGraphTest {

	private void t(String... expectedNames) {
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		String fileSubpath = String.format("tdg/%s.xml",methodName);
		try (InputStream is = TypeDependencyGraphTest.class.getResourceAsStream(fileSubpath)) {
			TypeDependencyGraph graph = new TypeDependencyGraph(is);
			List<TypeNode> actual = graph.getTopologicalOrdering();
			List<String> actualNames = from(actual).transform(TypeNode._getName).toList();
			assertEquals(ImmutableList.copyOf(expectedNames), actualNames);
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

}
