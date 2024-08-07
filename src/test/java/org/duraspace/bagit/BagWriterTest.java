/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;


import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.FileUtils;
import org.assertj.core.util.Maps;
import org.duraspace.bagit.profile.BagProfile;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test basic bag writing functionality to make sure we are writing compliant bags
 *
 * @author mikejritter
 * @since 2020-03-05
 */
public class BagWriterTest {

    // set up expected bag, data file, and tag files
    private final String bagName = "bag-writer-test";
    private final String filename = "hello-writer-✓";
    private final String extraTagName = "extra-tag.txt";

    private Path bag;
    private BagProfile profile;

    @BeforeEach
    public void setup() throws URISyntaxException, IOException {
        final URL sampleUrl = this.getClass().getClassLoader().getResource("sample");
        final Path sample = Paths.get(Objects.requireNonNull(sampleUrl).toURI());
        bag = sample.resolve(bagName);

        profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
    }

    @AfterEach
    public void teardown() {
        if (bag != null) {
            FileUtils.deleteQuietly(bag.toFile());
        }
    }

    @Test
    public void write() throws IOException {
        // The message digests to use
        final BagItDigest sha1 = BagItDigest.SHA1;
        final BagItDigest sha256 = BagItDigest.SHA256;
        final BagItDigest sha512 = BagItDigest.SHA512;
        final MessageDigest sha1MD = sha1.messageDigest();
        final MessageDigest sha256MD = sha256.messageDigest();
        final MessageDigest sha512MD = sha512.messageDigest();

        // Create a writer with 3 manifest algorithms
        Files.createDirectories(bag);
        final BagWriter writer = new BagWriter(bag.toFile(), Sets.newHashSet(sha1, sha256, sha512));

        // Setup the data files
        final Path data = bag.resolve("data");
        final Path file = Files.createFile(data.resolve(filename));

        final LinkedHashMap<File, String> sha1Sums = new LinkedHashMap<>();
        sha1Sums.put(file.toFile(), HexEncoder.toString(sha1MD.digest()));

        final LinkedHashMap<File, String> sha256Sums = new LinkedHashMap<>();
        sha256Sums.put(file.toFile(), HexEncoder.toString(sha256MD.digest()));

        final LinkedHashMap<File, String> sha512Sums = new LinkedHashMap<>();
        sha512Sums.put(file.toFile(), HexEncoder.toString(sha512MD.digest()));

        // second file
        final Path file2 = Files.createFile(data.resolve(filename + "2"));
        sha1Sums.put(file2.toFile(), HexEncoder.toString(sha1MD.digest()));
        sha256Sums.put(file2.toFile(), HexEncoder.toString(sha256MD.digest()));
        sha512Sums.put(file2.toFile(), HexEncoder.toString(sha512MD.digest()));

        writer.addTags(extraTagName, Maps.newHashMap("test-key", "test-value"));
        writer.addTags(extraTagName, Maps.newHashMap("additional-key", "additional-value"));
        final Map<String, String> bagInfoFields = new HashMap<>();
        bagInfoFields.put(BagConfig.SOURCE_ORGANIZATION_KEY, "bagit-support-✓");
        bagInfoFields.put(BagConfig.BAGGING_DATE_KEY, ISO_LOCAL_DATE.format(LocalDate.now()));
        bagInfoFields.put(BagConfig.BAG_SIZE_KEY, "0 bytes");
        bagInfoFields.put(BagConfig.PAYLOAD_OXUM_KEY, "1.0");
        writer.addTags(BagConfig.BAG_INFO_KEY, bagInfoFields);
        writer.registerChecksums(sha1, sha1Sums);
        writer.registerChecksums(sha256, sha256Sums);
        writer.registerChecksums(sha512, sha512Sums);

        writer.write();

        final Path bagit = bag.resolve("bagit.txt");
        final Path extra = bag.resolve(extraTagName);
        final Path bagInfo = bag.resolve(BagConfig.BAG_INFO_KEY);
        final Path sha1Tagmanifest = bag.resolve("tagmanifest-" + sha1.bagitName() + ".txt");
        final Path sha256Tagmanifest = bag.resolve("tagmanifest-" + sha256.bagitName() + ".txt");
        final Path sha512Tagmanifest = bag.resolve("tagmanifest-" + sha512.bagitName() + ".txt");

        checkBagTagFiles(Sets.newHashSet(sha1, sha256, sha512), Sets.newHashSet(sha1, sha256, sha512));

        // Assert that bagit.txt contains expected lines
        final List<String> bagitLines = Files.readAllLines(bagit);
        assertThat(bagitLines).containsSequence("BagIt-Version: 1.0", "Tag-File-Character-Encoding: UTF-8");

        // Assert that bag-info.txt contains... the bare necessities
        final List<String> bagInfoLines = Files.readAllLines(bagInfo);
        assertThat(bagInfoLines).contains(BagConfig.SOURCE_ORGANIZATION_KEY + ": bagit-support-✓");

        // Assert that extra-tag.txt exists
        final List<String> extraLines = Files.readAllLines(extra);
        assertThat(extraLines)
            .hasSize(2)
            .contains("test-key: test-value", "additional-key: additional-value");

        // Assert that tagmanifest-{sha1,sha256,sha512}.txt contain the manifest checksums
        tagManifestsContain(Sets.newHashSet(sha1Tagmanifest, sha256Tagmanifest, sha512Tagmanifest),
                            Sets.newHashSet(sha1, sha256, sha512));

        // Finally, pass BagProfile validation and BagIt validation
        validateBag();
    }

    @Test
    public void testWriteDistinctManifests() throws Exception {
        // The message digests to use
        final BagItDigest sha1 = BagItDigest.SHA1;
        final BagItDigest sha256 = BagItDigest.SHA256;
        final BagItDigest sha512 = BagItDigest.SHA512;
        final MessageDigest sha1MD = sha1.messageDigest();
        final MessageDigest sha256MD = sha256.messageDigest();

        // Create a writer with 3 manifest algorithms
        Files.createDirectories(bag);
        final BagWriter writer = new BagWriter(bag.toFile(), Sets.newHashSet(sha1, sha256), Sets.newHashSet(sha512));

        // Setup the data files
        final Path data = bag.resolve("data");
        final Path file = Files.createFile(data.resolve(filename));

        final LinkedHashMap<File, String> sha1Sums = new LinkedHashMap<>();
        sha1Sums.put(file.toFile(), HexEncoder.toString(sha1MD.digest()));

        final LinkedHashMap<File, String> sha256Sums = new LinkedHashMap<>();
        sha256Sums.put(file.toFile(), HexEncoder.toString(sha256MD.digest()));

        // second file
        final Path file2 = Files.createFile(data.resolve(filename + "2"));
        sha1Sums.put(file2.toFile(), HexEncoder.toString(sha1MD.digest()));
        sha256Sums.put(file2.toFile(), HexEncoder.toString(sha256MD.digest()));

        writer.addTags(extraTagName, Maps.newHashMap("test-key", "test-value"));
        writer.addTags(extraTagName, Maps.newHashMap("additional-key", "additional-value"));
        final Map<String, String> bagInfoFields = new HashMap<>();
        bagInfoFields.put(BagConfig.SOURCE_ORGANIZATION_KEY, "bagit-support-✓");
        bagInfoFields.put(BagConfig.BAGGING_DATE_KEY, ISO_LOCAL_DATE.format(LocalDate.now()));
        bagInfoFields.put(BagConfig.BAG_SIZE_KEY, "0 bytes");
        bagInfoFields.put(BagConfig.PAYLOAD_OXUM_KEY, "1.0");
        writer.addTags(BagConfig.BAG_INFO_KEY, bagInfoFields);
        writer.registerChecksums(sha1, sha1Sums);
        writer.registerChecksums(sha256, sha256Sums);

        writer.write();

        final Path bagit = bag.resolve("bagit.txt");
        final Path extra = bag.resolve(extraTagName);
        final Path bagInfo = bag.resolve(BagConfig.BAG_INFO_KEY);
        final Path sha512Tagmanifest = bag.resolve("tagmanifest-" + sha512.bagitName() + ".txt");

        checkBagTagFiles(Sets.newHashSet(sha1, sha256), Sets.newHashSet(sha512));

        // Assert that bagit.txt contains expected lines
        final List<String> bagitLines = Files.readAllLines(bagit);
        assertThat(bagitLines).containsSequence("BagIt-Version: 1.0", "Tag-File-Character-Encoding: UTF-8");

        // Assert that bag-info.txt contains... the bare necessities
        final List<String> bagInfoLines = Files.readAllLines(bagInfo);
        assertThat(bagInfoLines).contains(BagConfig.SOURCE_ORGANIZATION_KEY + ": bagit-support-✓");

        // Assert that extra-tag.txt exists
        final List<String> extraLines = Files.readAllLines(extra);
        assertThat(extraLines)
            .hasSize(2)
            .contains("test-key: test-value", "additional-key: additional-value");

        // Assert that tagmanifest-sha512.txt contain the manifest checksums
        tagManifestsContain(Sets.newHashSet(sha512Tagmanifest), Sets.newHashSet(sha1, sha256));

        // Finally, pass BagProfile validation and BagIt validation
        validateBag();
    }

    private void validateBag() throws IOException {
        final BagReader reader = new BagReader();
        try (BagVerifier verifier = new BagVerifier()) {
            final Bag readBag = reader.read(bag);
            profile.validateBag(readBag);
            verifier.isValid(readBag, false);
        } catch (UnparsableVersionException | MaliciousPathException | UnsupportedAlgorithmException |
            InvalidBagitFileFormatException e) {
            fail("Unable to read bag:\n" + e.getMessage());
        } catch (VerificationException | MissingPayloadDirectoryException | MissingPayloadManifestException |
            FileNotInPayloadDirectoryException | CorruptChecksumException | MissingBagitFileException |
            InterruptedException e) {
            fail("Unable to verify bag:\n" + e.getMessage());
        }
    }

    private void checkBagTagFiles(final Set<BagItDigest> payloadAlgs, final Set<BagItDigest> tagsAlgs) {
        final Set<Path> expected = Sets.newHashSet(bag.resolve("data"), bag.resolve("bagit.txt"),
                                                   bag.resolve(extraTagName), bag.resolve(BagConfig.BAG_INFO_KEY));
        payloadAlgs.stream().map(alg -> bag.resolve("manifest-" + alg.bagitName() + ".txt")).forEach(expected::add);
        tagsAlgs.stream().map(alg -> bag.resolve("tagmanifest-" + alg.bagitName() + ".txt")).forEach(expected::add);

        try (DirectoryStream<Path> tagFiles = Files.newDirectoryStream(bag)) {
            assertThat(tagFiles).containsExactlyInAnyOrderElementsOf(expected);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private void tagManifestsContain(final Set<Path> tagmanifests, final Set<BagItDigest> contained)
        throws IOException {
        final String manifestRegex = contained.stream().map(BagItDigest::bagitName).collect(Collectors.joining("|"));
        for (Path tagmanifest : tagmanifests) {
            try (Stream<String> lines = Files.lines(tagmanifest)) {
                assertThat(lines)
                    .filteredOn(line -> line.contains("manifest"))
                    .hasSize(contained.size())
                    .allSatisfy(entry -> assertThat(entry).containsPattern(manifestRegex));
            }
        }
    }

    @Test
    public void testAddInvalidAlgorithm() throws IOException {
        assertThrows(IllegalArgumentException.class,
            ()->{
                // The message digests to use
                final BagItDigest sha1 = BagItDigest.SHA1;
                final BagItDigest sha256 = BagItDigest.SHA256;

                // Create a writer with 3 manifest algorithms
                Files.createDirectories(bag);
                final BagWriter writer = new BagWriter(bag.toFile(), Sets.newHashSet(sha1));

                // we don't need to pass any files, just the errant BagItDigest
                writer.registerChecksums(sha256, new LinkedHashMap<>());
            });
    }

}