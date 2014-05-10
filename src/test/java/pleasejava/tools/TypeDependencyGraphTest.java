package pleasejava.tools;

import org.junit.Test;

/**
 * @author Tomas Zalusky
 */
public class TypeDependencyGraphTest {

	private void t(String[] expected, String xml) {
		TypeDependencyGraph graph = new TypeDependencyGraph(xml);
		
	}
	
	@Test
	public void simple() {
		String xml = ""
				+ "<tdg>                                               \n"
				+ "  <varray name='a_test_package.var1' of='integer' />\n"
				+ "</tdg>                                              \n"
		;
		String[] expected = {"a_test_package.var1", "integer"};
		t(expected,xml);
	}

}
