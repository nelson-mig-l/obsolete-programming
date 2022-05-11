import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SolutionTest {

    ByteArrayInputStream in;
    ByteArrayOutputStream out;

    String expected;

    @Test
    void testArithmetic() throws URISyntaxException, IOException {
        test("arithmetic-test");
    }

    @Test
    void testStackManipulations() throws URISyntaxException, IOException {
        test("stack-manipulations");
    }

    @Test
    void testLogic() throws URISyntaxException, IOException {
        test("logic");
    }

    @Test
    void testSimpleFunctionSquare() throws URISyntaxException, IOException {
        test("simple-function-square");
    }

    @Test
    void testFunctionAndTest() throws URISyntaxException, IOException {
        test("function-and-test");
    }

    @Test
    void testFunctionCallingFunctionAndNestedIf() throws URISyntaxException, IOException {
        test("function-calling-function-and-nested-if");
    }

    @Test
    void testTheQueenOfFunctions() throws URISyntaxException, IOException {
        test("the-queen-of-functions");
    }

    @Test
    void testIHaveNoLoopsAndIMustIterate() throws URISyntaxException, IOException {
        test("i-have-no-loops-and-i-must-iterate");
    }

    @Test
    void testHelloFibonacci() throws URISyntaxException, IOException {
        test("hello-fibonacci");
    }

    @Test
    void testIntegerSquareRoot() throws URISyntaxException, IOException {
        test("integer-square-root");
    }

    void test(String program) throws URISyntaxException, IOException {
        givenProgram(program);
        whenProgramIsExecuted();
        thenOutputIsAsExpected();
    }

    private void givenProgram(String program) throws URISyntaxException, IOException {
        final List<String> lines = Files.readAllLines(getResourcePath(program + ".txt"));
        final int count = lines.size();
        final String data = String.join("\n", lines);
        in = new ByteArrayInputStream((count + "\n" + data).getBytes());

        final byte[] bytes = Files.readAllBytes(getResourcePath(program + "-result.txt"));
        expected = new String(bytes);

        out = new ByteArrayOutputStream();
    }

    private void whenProgramIsExecuted() {
        new Solution(in, new PrintStream(out)).run();
    }

    private void thenOutputIsAsExpected() {
        assertEquals(expected, out.toString());
    }

    private Path getResourcePath(final String name) throws URISyntaxException {
        final URL resource = SolutionTest.class.getClassLoader().getResource(name);
        return Paths.get(resource.toURI());
    }

}
