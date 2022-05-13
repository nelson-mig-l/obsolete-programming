import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Temperatures {

    @Test
    void testSimpleTestCase() throws URISyntaxException, IOException {
        assertEquals("1", testCaseWithInput("5 1 -2 -8 4 5"));
    }

    @Test
    void testOnlyNegativeNumbers() throws URISyntaxException, IOException {
        assertEquals("-5", testCaseWithInput("3 -12 -5 -137"));
    }

    @Test
    void testChooseTheRightTemperature() throws URISyntaxException, IOException {
        assertEquals("5", testCaseWithInput("6 42 -5 12 21 5 24"));
    }

    @Test
    void testChooseTheRightTemperature2() throws URISyntaxException, IOException {
        assertEquals("5", testCaseWithInput("6 42 5 12 21 -5 24"));
    }

    @Test
    void testComplexTestCase() throws URISyntaxException, IOException {
        assertEquals("2", testCaseWithInput("10 -5 -4 -2 12 -40 4 2 18 11 5"));
    }

    @Test
    void testNoTemperature() throws URISyntaxException, IOException {
        assertEquals("0", testCaseWithInput("0"));
    }

    private String testCaseWithInput(String inputs) throws URISyntaxException, IOException {
        final InputStream in = loadProgram(inputs);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Solution solution = new Solution(in, new PrintStream(out));
        solution.run();
        return out.toString().trim();
    }

    private static InputStream loadProgram(final String inputs) throws URISyntaxException, IOException {
        final List<String> lines = Files.readAllLines(getResourcePath("temperatures-golf.txt")).stream()
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());
        final int count = lines.size();
        final String data = String.join("\n", lines);
        return new ByteArrayInputStream((count + "\n" + data + "\n" + inputs).getBytes());
    }

    private static Path getResourcePath(final String name) throws URISyntaxException {
        final URL resource = SolutionTest.class.getClassLoader().getResource(name);
        return Paths.get(resource.toURI());
    }
}