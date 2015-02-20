package plsql;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import pleasejava.Utils;

@RunWith(Parameterized.class)
public class JavaModelTest extends AbstractTypeGraphTest {

	private static boolean record = false;

	private final String graphName;

	private final String expected;

	public JavaModelTest(String graphName) throws IOException {
		this.graphName = graphName;
		this.expected = record ? null : readExpectedOutput(getClass(),graphName);
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{"alltypes"},
				{"javatype"},
		});
	}

	@Test
	public void test() throws IOException, JDOMException {
		TypeGraph typeGraph = loadGraph(graphName);
		String fileSubpath = String.format("typegraph/%s.xml",graphName);
		try (InputStream is = JavaModelTest.class.getResourceAsStream(fileSubpath)) {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			Element rootElement = doc.getRootElement();
			JavaModel javaModel = JavaModel.from(typeGraph, rootElement);
			String actual = javaModel.toString();
			if (record) {
				writeExpectedOutput(getClass(),graphName,actual);
			} else {
				Utils.assertEquals(expected,actual);
			}
		}
	}

}
