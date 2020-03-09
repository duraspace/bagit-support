/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;


import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test basic bag writing functionality to make sure we are writing compliant bags
 *
 * @author mikejritter
 * @since 2020-03-05
 */
public class BagWriterTest {

    // set up expected bag, data file, and tag files
    private final String bagName = "bag-writer-test";
    private final String filename = "hello-writer";
    private final String extraTagName = "extra-tag.txt";

    private Path bag;
    private BagProfile profile;

    @Before
    public void setup() throws URISyntaxException, IOException {
        final URL sampleUrl = this.getClass().getClassLoader().getResource("sample");
        final Path sample = Paths.get(Objects.requireNonNull(sampleUrl).toURI());
        bag = sample.resolve(bagName);

        profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
    }

    @After
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
        final BagWriter writer = new BagWriter(bag.toFile(), Sets.newHashSet(sha1.bagitName(), sha256.bagitName(),
                                                                             sha512.bagitName()));

        // Setup the data file
        final Path data = bag.resolve("data");
        final Path file = Files.createFile(data.resolve(filename));
        final Map<File, String> sha1Sums = Maps.newHashMap(file.toFile(), HexEncoder.toString(sha1MD.digest()));
        final Map<File, String> sha256Sums  = Maps.newHashMap(file.toFile(), HexEncoder.toString(sha256MD.digest()));
        final Map<File, String> sha512Sums = Maps.newHashMap(file.toFile(), HexEncoder.toString(sha512MD.digest()));

        writer.addTags(extraTagName, Maps.newHashMap("test-key", "test-value"));
        final Map<String, String> bagInfoFields = new HashMap<>();
        bagInfoFields.put(BagConfig.SOURCE_ORGANIZATION_KEY, "bagit-support");
        bagInfoFields.put(BagConfig.BAGGING_DATE_KEY, ISO_LOCAL_DATE.format(LocalDate.now()));
        bagInfoFields.put(BagConfig.BAG_SIZE_KEY, "0 bytes");
        bagInfoFields.put(BagConfig.PAYLOAD_OXUM_KEY, "1.0");
        writer.addTags(BagConfig.BAG_INFO_KEY, bagInfoFields);
        writer.registerChecksums(sha1.bagitName(), sha1Sums);
        writer.registerChecksums(sha256.bagitName(), sha256Sums);
        writer.registerChecksums(sha512.bagitName(), sha512Sums);

        writer.write();

        final Path bagit = bag.resolve("bagit.txt");
        final Path extra = bag.resolve(extraTagName);
        final Path bagInfo = bag.resolve(BagConfig.BAG_INFO_KEY);
        final Path sha1Manifest = bag.resolve("manifest-" + sha1.bagitName() + ".txt");
        final Path sha1Tagmanifest = bag.resolve("tagmanifest-" + sha1.bagitName() + ".txt");
        final Path sha256Manifest = bag.resolve("manifest-" + sha256.bagitName() + ".txt");
        final Path sha256Tagmanifest = bag.resolve("tagmanifest-" + sha256.bagitName() + ".txt");
        final Path sha512Manifest = bag.resolve("manifest-" + sha512.bagitName() + ".txt");
        final Path sha512Tagmanifest = bag.resolve("tagmanifest-" + sha512.bagitName() + ".txt");

        // Assert that all tag files (bagit.txt, bag-info.txt, etc) exist
        assertThat(bagit).exists();
        assertThat(extra).exists();
        assertThat(bagInfo).exists();
        assertThat(sha1Manifest).exists();
        assertThat(sha1Tagmanifest).exists();
        assertThat(sha256Manifest).exists();
        assertThat(sha256Tagmanifest).exists();
        assertThat(sha512Manifest).exists();
        assertThat(sha512Tagmanifest).exists();

        // Assert that bagit.txt contains expected lines
        final List<String> bagitLines = Files.readAllLines(bagit);
        assertThat(bagitLines).contains("BagIt-Version: 0.97", "Tag-File-Character-Encoding: UTF-8");

        // Assert that bag-info.txt contains... the bare necessities
        final List<String> bagInfoLines = Files.readAllLines(bagInfo);
        assertThat(bagInfoLines).contains(BagConfig.SOURCE_ORGANIZATION_KEY + ": bagit-support");

        // Assert that extra-tag.txt exists
        final List<String> extraLines = Files.readAllLines(extra);
        assertThat(extraLines).contains("test-key: test-value");

        // Finally, pass BagProfile validation and BagIt validation
        final BagReader reader = new BagReader();
        final BagVerifier verifier = new BagVerifier();
        try {
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
}