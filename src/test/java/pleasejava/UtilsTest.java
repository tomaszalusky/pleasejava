package pleasejava;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;

import static org.junit.Assert.*;
import static pleasejava.Utils.findOnly;

/**
 * @author Tomas Zalusky
 */
public class UtilsTest {

	// valid cases
	@Test public void testFindOnly_0() {assertEquals(Optional.empty(), Stream.of().collect(findOnly()));}
	@Test public void testFindOnly_1() {assertEquals(Optional.of(1)  , Stream.of(1).collect(findOnly()));}
	// too many elements leads to ISE
	@Test(expected=IllegalStateException.class) public void testFindOnly_2() {Stream.of(1,2).collect(findOnly());}
	@Test(expected=IllegalStateException.class) public void testFindOnly_same() {Stream.of(1,1).collect(findOnly());}
	// null counts as element
	@Test(expected=IllegalStateException.class) public void testFindOnly_1null() {Stream.of(new Object[] {1,null}).collect(findOnly());}
	@Test(expected=IllegalStateException.class) public void testFindOnly_null1() {Stream.of(new Object[] {null,1}).collect(findOnly());}
	// single null leads to NPE - behavior consistent with Stream.findFirst
	@Test(expected=NullPointerException .class) public void testFindOnly_null() {Stream.of(new Object[] {null}).collect(findOnly());}

}
