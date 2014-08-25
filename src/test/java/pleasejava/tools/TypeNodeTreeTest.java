package pleasejava.tools;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.FluentIterable.from;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import pleasejava.Utils;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

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
		});
	}

	@Test
	public void test() throws IOException {
		TypeDependencyGraph graph = loadGraph(name);
		List<Type> topologicalOrdering = graph.getTopologicalOrdering();
		TypeNode rootNode = topologicalOrdering.get(0).toTypeNode(null);
		String actual = rootNode.toString();
		if (record) {
			writeExpectedOutput(getClass(),name,actual);
		} else {
			Utils.assertEquals(expected,actual);
		}
	}

}
