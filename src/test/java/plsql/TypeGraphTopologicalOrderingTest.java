package plsql;

import static com.google.common.collect.FluentIterable.from;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableList;

/**
 * Tests topological ordering of types in type graph.
 * @author Tomas Zalusky
 */
@RunWith(Parameterized.class)
public class TypeGraphTopologicalOrderingTest extends AbstractTypeGraphTest {

	private final String graphName;
	
	private final List<String> expectedNames;
	
	public TypeGraphTopologicalOrderingTest(String graphName, List<String> expectedNames) {
		this.graphName = graphName;
		this.expectedNames = expectedNames;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"simple"  , ImmutableList.of("main","a_test_package.var1","integer")},
				{"dag1"    , ImmutableList.of("main","a","d","b","c","e","f","g","h","i","varchar2(100)")},
				{"alltypes", ImmutableList.of(
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
				)},
				{"topLevel",ImmutableList.of(
						"pn_pn_tn_tr","pn_tn_tr","pv_tn_tr","tn_tr","pv_pv_tv_tr","pv_tv_tr","pn_tv_tr","tv_tr","pr_tr","tr",
						"pkg.nst3","pkg.varn","pkg.var3","pkg.nstv","pkg.rec2","pkg.nst2","pkg.var2",
						"varchar2(10)","nst1","var1","rec","boolean","integer"
				)},
		});
	}

	@Test
	public void test() throws IOException {
		TypeGraph graph = loadGraph(graphName);
		List<Type> actual = graph.getTopologicalOrdering();
		List<String> actualNames = from(actual).transform(Type._getName).toList();
		assertEquals(expectedNames, actualNames);
	}

}
