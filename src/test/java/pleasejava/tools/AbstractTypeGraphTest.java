package pleasejava.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * Support class for all test working on graph of types.
 * @author Tomas Zalusky
 */
public abstract class AbstractTypeGraphTest {

	protected TypeDependencyGraph loadGraph(String graphName) throws IOException {
		String fileSubpath = String.format("tdg/%s.xml",graphName);
		TypeDependencyGraph result;
		try (InputStream is = TypeDependencyGraphTest.class.getResourceAsStream(fileSubpath)) {
			result = TypeDependencyGraph.createFrom(is);
		}
		return result;
	}

}
