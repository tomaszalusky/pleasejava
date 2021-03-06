package plsql;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import pleasejava.Utils;

/**
 * @author Tomas Zalusky
 */
@RunWith(Parameterized.class)
public class TypeNodeTreeTest extends AbstractTypeGraphTest {

	private static boolean record = false;
	
	private final String graphName;
	
	private final String executableName;
	
	private final String expected;
	
	public TypeNodeTreeTest(String graphName, String executableName) throws IOException {
		this.graphName = graphName;
		this.executableName = executableName;
		this.expected = record ? null : readExpectedOutput(getClass(),graphName,executableName);
	}

	@Parameterized.Parameters(name = "{index}: {0}-{1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"simple"  ,"main"},
				{"dag1"    ,"main"},
				{"alltypes","echo"},
				{"toplevel","tn_tr"},
				{"toplevel","pn_tn_tr"},
				{"toplevel","pn_pn_tn_tr"},
				{"toplevel","tv_tr"},
				{"toplevel","pv_tv_tr"},
				{"toplevel","pv_pv_tv_tr"},
				{"toplevel","pv_tn_tr"},
				{"toplevel","pn_tv_tr"},
				{"toplevel","tr"},
				{"toplevel","pr_tr"},
		});
	}

	@Test
	public void test() throws IOException {
		TypeGraph graph = loadGraph(graphName);
		AbstractSignature rootType = graph.findType(ProcedureSignature.class,executableName);
		if (rootType == null) { // silly check but anyway it will be refactored when disambiguating overloads (procedures and functions are only special kind of overload)
			rootType = graph.findType(FunctionSignature.class,executableName);
		}
		TypeNodeTree tnt = graph.toTypeNodeTree(rootType);
		String actual = tnt.toString();
		if (record) {
			writeExpectedOutput(getClass(),graphName,executableName,actual);
		} else {
			Utils.assertEquals(expected,actual);
		}
	}

}
