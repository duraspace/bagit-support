/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link BagSerializer} and implementing classes
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class BagSerializerTest {

    private Path bag;
    private Path resources;
    private BagProfile profile;
    private Set<Path> bagFiles;

    @Before
    public void setup() throws IOException, URISyntaxException {
        final String samples = "sample";

        profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
        final URI sample = Objects.requireNonNull(this.getClass().getClassLoader().getResource(samples)).toURI();
        resources = Paths.get(sample);
        bag = Paths.get(sample).resolve("bag");
        bagFiles = Files.walk(bag)
                        .map(path -> resources.relativize(path))
                        .collect(Collectors.toSet());
    }

    @Test
    public void testZipSerializer() throws IOException {
        final BagSerializer zipper = SerializationSupport.serializerFor("zip", profile);
        final Path writtenBag = zipper.serialize(bag);

        assertThat(writtenBag).exists();
        assertThat(writtenBag).isRegularFile();

        // just make sure we can read it
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(Files.newInputStream(writtenBag))) {
            ArchiveEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                assertThat(bagFiles).contains(Paths.get(entry.getName()));
            }
        }

        Files.delete(writtenBag);
    }

    @Test
    public void testTarSerializer() throws IOException {
        final BagSerializer serializer = SerializationSupport.serializerFor("tar", profile);
        final Path writtenBag = serializer.serialize(bag);

        assertThat(writtenBag).exists();
        assertThat(writtenBag).isRegularFile();

        // just make sure we can read it
        try (TarArchiveInputStream zipIn = new TarArchiveInputStream(Files.newInputStream(writtenBag))) {
            ArchiveEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                assertThat(bagFiles).contains(Paths.get(entry.getName()));
            }
        }

        Files.delete(writtenBag);
    }

    @Test
    public void testGZipSerializer() throws IOException {
        final BagSerializer serializer = SerializationSupport.serializerFor("tgz", profile);
        final Path writtenBag = serializer.serialize(bag);

        assertThat(writtenBag).exists();
        assertThat(writtenBag).isRegularFile();

        // just make sure we can read it
        try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(writtenBag));
             TarArchiveInputStream zipIn = new TarArchiveInputStream(gzip)) {
            ArchiveEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                assertThat(bagFiles).contains(Paths.get(entry.getName()));
            }
        }

        Files.delete(writtenBag);
    }

}