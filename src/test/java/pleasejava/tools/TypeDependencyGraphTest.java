package pleasejava.tools;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * @author Tomas Zalusky
 */
public class TypeDependencyGraphTest {

	private void t(String... expected) {
		String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
		String fileSubpath = String.format("tdg/%s.xml",methodName);
		try (InputStream is = TypeDependencyGraphTest.class.getResourceAsStream(fileSubpath)) {
			TypeDependencyGraph graph = new TypeDependencyGraph(is);
			
		} catch (IOException e) {
		}
	}
	
	@Test public void simple() {t("a_test_package.var1", "integer");}

}
