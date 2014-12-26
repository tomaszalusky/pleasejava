package pleasejava.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
	
	private final String name;
	
	private final String expected;
	
	public TypeNodeTreeTest(String name) throws IOException {
		this.name = name;
		this.expected = record ? null : readExpectedOutput(getClass(),name);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"simple"},
				{"dag1"},
				{"alltypes"},
				{"topLevelNestedTable"},
				{"topLevelNestedTableInPackageNestedTable"},
				{"topLevelNestedTableInPackageNestedTableInPackageNestedTable"},
				{"topLevelVarray"},
				{"topLevelVarrayInPackageVarray"},
				{"topLevelVarrayInPackageVarrayInPackageVarray"},
				{"topLevelNestedTableInPackageVarray"},
				{"topLevelVarrayInPackageNestedTable"},
				{"topLevelRecord"},
		});
	}

	@Test
	public void test() throws IOException {
		TypeGraph graph = loadGraph(name);
		List<Type> topologicalOrdering = graph.getTopologicalOrdering();
		AbstractSignature rootType = (AbstractSignature)topologicalOrdering.get(0);
		TypeNodeTree tnt = graph.toTypeNodeTree(rootType);
		String actual = tnt.toString();
		if (record) {
			writeExpectedOutput(getClass(),name,actual);
		} else {
			Utils.assertEquals(expected,actual);
		}
	}

}
