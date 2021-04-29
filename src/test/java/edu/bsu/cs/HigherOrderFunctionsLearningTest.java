package edu.bsu.cs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneOffset;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.jayway.jsonpath.Filter.filter;

/**
 * Contains learning tests to demonstrate knowledge of higher-order functions
 * as used within
 * <a href="https://docs.oracle.com/en/java/javase/16/docs/api/java.base/java/util/stream/package-summary.html">
 * Java's Stream API</a>.
 */
public class HigherOrderFunctionsLearningTest {

    /**
     * Demonstrate the collection of a stream into a list and the comparison of two iterables.
     * This test should not be changed. It should simply pass, meaning it shows the correct
     * structure and can be used to ensure correct system configuration.
     */
    @Test
    public void testCollect() {
        List<Integer> actual =
                Stream.of(1, 2, 3)
                        .collect(Collectors.toList());
        List<Integer> expected = List.of(1, 2, 3);
        Assertions.assertIterableEquals(expected, actual);
    }

    /**
     * Use the <code>map</code> function to transform a list of numbers.
     */
    @Test
    public void testMap() {
        Stream<Integer> input = Stream.of(1, 2, 3).map(x -> x+1);
        List<Integer> actual = input.
                collect(Collectors.toList());
        List<Integer> expected = List.of(2, 3, 4);
        Assertions.assertIterableEquals(expected, actual);
    }

    /**
     * Use the <code>filter</code> function to find all the codes that start with 'b'.
     */
    @Test
    public void testFilter() {
        Stream<String> input = Stream.of("a1", "a2", "a3", "b1", "b2", "b3");
        List<String> actual = input
                .filter(x -> x.startsWith("b"))
                .collect(Collectors.toList());
        List<String> expected = List.of("b1", "b2", "b3");
        Assertions.assertIterableEquals(expected, actual);
    }

    /**
     * Use the <code>distinct</code> function to extract all of the unique alphabetic
     * code prefixes.
     */
    @Test
    public void testDistinct() {
        Stream<String> input = Stream.of("a1", "a2", "a3", "b1", "b2", "b3");
        List<String> actual = input
                .map(x -> x.substring(0, 1))
                .distinct()
                .collect(Collectors.toList());
        List<String> expected = List.of("a", "b");
        Assertions.assertIterableEquals(expected, actual);
    }

    /**
     * Concatenate all the codes that end in an even number.
     */
    @Test
    public void testConcatEven() {
        Stream<String> input = Stream.of("a1", "a2", "a3", "b1", "b2", "b3");
        String actual = input
                .filter(x -> Integer.parseInt(x.substring(1))
                        % 2 == 0)
                .collect(Collectors.joining());
        String expected = "a2b2";
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Count the number of changes in the data that predate March 1, 2021 (UTC).
     */
    @Test
    public void testCountByDate() {
        Stream<Revision> input = getRevisions("soup04.json");
        Instant predateInstant = Instant.parse("2021-03-01T00:00:00.00Z");
        long actual = input
                .filter(x -> x.timestamp.isBefore(predateInstant))
                .count();
        int expected = 3;
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Load the given resource as a stream of {@link Revision revisions}.
     * @param resourceName The name of the resource in the test/resources folder
     * @return a stream of revisions
     */
    private Stream<Revision> getRevisions(String resourceName) {
        // This implementation uses JsonPath to extract the portion of the file that
        // contains the revision data. Jackson databinding is then used to unmarshall
        // that data into Revision objects.
        // The JavaTimeModule is necessary in order to automatically unmarshall the
        // timestamp strings into JDK8 (JSR310) Instant objects.
        try {
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName);
            JSONArray jsonArray = JsonPath.read(in, "$..revisions.*");
            ObjectMapper mapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule());
            Revision[] revisions = mapper.readValue(jsonArray.toString(), Revision[].class);
            return Arrays.stream(revisions);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    /**
     * Count the number of revisions made by the whitelisted users.
     */
    @Test
    public void testCountWhitelisted() {
        Stream<Revision> input = getRevisions("soup30.json");
        List<String> whitelist = List.of("Sleepy Beauty", "Spencer", "QueasyQ");
        long actual = input
                .map(x -> x.user)
                .filter(whitelist::contains)
                .count();
        int expected = 3;
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Count the revisions, omitting anyone with "bot" in their name (case-insensitive).
     */
    @Test
    public void testCountNonBots() {
        Stream<Revision> input = getRevisions("soup30.json");
        long actual = input
                .filter(x -> !
                        x.user
                                .toLowerCase(Locale.ROOT)
                                .contains("bot"))
                .count();
        int expected = 26;
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Count the changes made in February (in UTC).
     */
    @Test
    public void testCountChangesInFebruary() {
        Stream<Revision> input = getRevisions("soup30.json");
        long actual = input
                .map(x -> x.timestamp
                        .atOffset(ZoneOffset.UTC)
                        .getMonth())
                .filter(x -> x.equals(Month.FEBRUARY))
                .count();
        int expected = 9;
        Assertions.assertEquals(expected, actual);
    }

    /**
     * Count the number of changes made in each month (in UTC).
     */
    @Test
    public void testCountChangesByMonth() {
        Month firstMonth = Month.FEBRUARY;
        Month secondMonth = Month.MARCH;
        Map<Month, Long> expected = Map.of(firstMonth, 3L, secondMonth, 1L);
        // moved the Month values originally in expected, to respective local variables,
        // so can be used in actual as well (lets it work for any pair of months entered prior)
        Stream<Revision> input = getRevisions("soup04.json");

        Stream<Month> revisionsBetweenMonths = input
                .map(x -> x.timestamp
                        .atOffset(ZoneOffset.UTC)
                        .getMonth())
                .filter(x -> x.equals(firstMonth) || x.equals(secondMonth));

        Map<Month, Long> actual = revisionsBetweenMonths
                .collect(Collectors
                        .groupingBy(Function.identity(), Collectors.counting()));

        Assertions.assertEquals(expected, actual);
    }

    /**
     * Determine which month had the most changes.
     * If more than one month is tied for the most, return an arbitrary one of these.
     */
    @Test
    public void testDetermineMostActiveMonth() {
        Stream<Revision> input = getRevisions("soup04.json");

        Map<Month, Long> revisionsByMonth = input
                .map(x -> x.timestamp.atOffset(ZoneOffset.UTC)
                        .getMonth())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        Month actual = Objects.requireNonNull(revisionsByMonth
                .entrySet()
                .stream().max(Map.Entry.comparingByValue())
                .orElse(null)).getKey();
        Month expected = Month.FEBRUARY;
        Assertions.assertEquals(expected, actual);
    }

    /**
     * A learning test that validates reading JSON results correctly.
     * There is no need to modify this test.
     */
    @Test
    public void testReadRevisions() {
        Stream<Revision> revisions = getRevisions("soup04.json");
        Assertions.assertEquals(4, revisions.count());
    }
}

@JsonAutoDetect
class Revision {
    public String user;
    public Instant timestamp;
}