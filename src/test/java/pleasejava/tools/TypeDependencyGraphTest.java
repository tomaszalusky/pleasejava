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
	
	@Test public void simple() {t("a_test_package.var1", "integer");}

}
