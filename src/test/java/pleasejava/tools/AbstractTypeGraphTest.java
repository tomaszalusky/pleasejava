package pleasejava.tools;

import static com.google.common.base.Throwables.propagate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Support class for all test working on graph of types.
 * @author Tomas Zalusky
 */
public abstract class AbstractTypeGraphTest {

	protected static TypeDependencyGraph loadGraph(String graphName) throws IOException {
		String fileSubpath = String.format("tdg/%s.xml",graphName);
		TypeDependencyGraph result;
		try (InputStream is = TypeGraphTopologicalOrderingTest.class.getResourceAsStream(fileSubpath)) {
			result = TypeDependencyGraph.createFrom(is);
		}
		return result;
	}

	protected static String readExpectedOutput(Class<?> clazz, String name) throws IOException {
		URL resource;
		try {
			String fileName = clazz.getSimpleName() + "-" + name + ".txt";
			resource = Resources.getResource(clazz,fileName);
		} catch (IllegalArgumentException e) { // resource does not exist
			throw propagate(e);
		}
		String result = Resources.toString(resource,Charsets.UTF_8);
		return result;
	}

	protected static void writeExpectedOutput(Class<?> clazz, String name, String content) throws IOException {
		File targetTestClassesDir;
		try {
			targetTestClassesDir = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			throw Throwables.propagate(e);
		}
		File serverRootDir = targetTestClassesDir.getParentFile().getParentFile();
		File srcTestResourcesDir = new File(serverRootDir,"src/test/resources");
		String fileName = clazz.getSimpleName() + "-" + name + ".txt";
		for (File defaultPackageDir : ImmutableList.of(srcTestResourcesDir,targetTestClassesDir)) { // recorded resource is stored into both src/test/resources and target/classes in order to prevent Eclipse or Maven inconsistencies
			File packageDir = new File(defaultPackageDir,clazz.getPackage().getName().replace(".","/"));
			File resource = new File(packageDir,fileName);
			Files.createParentDirs(resource);
			Files.write(content, resource, Charsets.UTF_8);
		}
	}
	
}
