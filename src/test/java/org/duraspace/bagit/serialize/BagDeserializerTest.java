/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Stream;

import org.duraspace.bagit.profile.BagProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test that zip, tar, and tar+gz extraction works as expected
 *
 * @author mikejritter
 * @since 2020-02-13
 */
public class BagDeserializerTest {

    private static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of("bag-tar.tar", "bag-tar"),
            Arguments.of("bag-tar-no-dirs.tar", "bag-tar"),
            Arguments.of("bag-zip.zip", "bag-zip"),
            Arguments.of("bag-zip-no-dirs.zip", "bag-zip"),
            Arguments.of("bag-gzip.tar.gz", "bag-gzip")
        );
    }

    public static final String BAG_INFO_TXT = "bag-info.txt";
    public static final String DATA_DIR = "data";
    private final String group = "compress";
    private Path target;

    private final String archive = "";
    private final String expectedDir = "";

    @BeforeEach
    public void setup() throws URISyntaxException {
        final URL sample = this.getClass().getClassLoader().getResource("sample");
        target = Paths.get(Objects.requireNonNull(sample).toURI());
        assertNotNull(target);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testExtract(final String archive, final String expectedDir) {
        final Path path = target.resolve(group).resolve(archive);
        try {
            final BagProfile profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
            final BagDeserializer deserializer = SerializationSupport.deserializerFor(path, profile);
            deserializer.deserialize(path);
        } catch (IOException e) {
            fail("Unexpected exception:\n" + e.getMessage());
        }

        final Path bag = target.resolve(group).resolve(expectedDir);
        assertTrue(Files.exists(bag));
        assertTrue(Files.exists(bag.resolve(BAG_INFO_TXT)));
        assertTrue(Files.exists(bag.resolve(DATA_DIR)));
        assertTrue(Files.isDirectory(bag.resolve(DATA_DIR)));
    }

}
