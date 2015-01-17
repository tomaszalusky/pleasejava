package plsql;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * All tests. Subsequent test cases do a little bit more testing than previous ones,
 * in case of failure it's better to start fixing former tests.
 * (This is not a case of intertest dependency, only hint for better test maintenance.)
 * @author Tomas Zalusky
 */
@RunWith(Suite.class)
@SuiteClasses({
		TypeGraphLoadTest.class,
		TypeGraphTopologicalOrderingTest.class,
		TypeNodeTreeTest.class,
		TransferObjectTreeTest.class,
		TypeNodeTreeToTransferObjectTreeTest.class
})
public class AllTests {

}
