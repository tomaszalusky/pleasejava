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
	public void javatype() throws IOException, JDOMException {
		t("javatype");
	}

	@Test
	public void alltypes() throws IOException, JDOMException {
		t("alltypes");
	}
	
	private void t(String graphName) throws IOException, JDOMException {
		TypeGraph typeGraph = loadGraph(graphName);
		String fileSubpath = String.format("typegraph/%s.xml",graphName);
		try (InputStream is = TypeGraphTopologicalOrderingTest.class.getResourceAsStream(fileSubpath)) {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(is);
			Element rootElement = doc.getRootElement();
			JavaModel javaModel = JavaModel.from(typeGraph, rootElement);
			System.out.println(javaModel);
		}
	}

}
