package plsql;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;

public class JavaModelTest extends AbstractTypeGraphTest {

	@Test
	public void test() throws IOException, JDOMException {
		TypeGraph typeGraph = loadGraph("javatype");
		String fileSubpath = String.format("typegraph/%s.xml","javatype");
		try (InputStream is = TypeGraphTopologicalOrderingTest.class.getResourceAsStream(fileSubpath)) {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			Element rootElement = doc.getRootElement();
			JavaModel javaModel = JavaModel.from(typeGraph, rootElement);
			System.out.println(javaModel);
		}
	}

}
