/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.profile;

import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.duraspace.bagit.BagConfig.ACCESS_KEY;
import static org.duraspace.bagit.BagConfig.BAGGING_DATE_KEY;
import static org.duraspace.bagit.BagConfig.BAG_SIZE_KEY;
import static org.duraspace.bagit.BagConfig.CONTACT_EMAIL_KEY;
import static org.duraspace.bagit.BagConfig.CONTACT_NAME_KEY;
import static org.duraspace.bagit.BagConfig.CONTACT_PHONE_KEY;
import static org.duraspace.bagit.BagConfig.EXTERNAL_DESCRIPTION_KEY;
import static org.duraspace.bagit.BagConfig.ORGANIZATION_ADDRESS_KEY;
import static org.duraspace.bagit.BagConfig.PAYLOAD_OXUM_KEY;
import static org.duraspace.bagit.BagConfig.SOURCE_ORGANIZATION_KEY;
import static org.duraspace.bagit.BagConfig.TITLE_KEY;
import static org.duraspace.bagit.profile.BagProfileConstants.ACCEPT_BAGIT_VERSION;
import static org.duraspace.bagit.profile.BagProfileConstants.ACCEPT_SERIALIZATION;
import static org.duraspace.bagit.profile.BagProfileConstants.BAGIT_PROFILE_IDENTIFIER;
import static org.duraspace.bagit.profile.BagProfileConstants.BAGIT_PROFILE_INFO;
import static org.duraspace.bagit.profile.BagProfileConstants.BAGIT_PROFILE_VERSION;
import static org.duraspace.bagit.profile.BagProfileConstants.BAG_INFO;
import static org.duraspace.bagit.profile.BagProfileConstants.MANIFESTS_REQUIRED;
import static org.duraspace.bagit.profile.BagProfileConstants.PROFILE_VERSION;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_FILES_REQUIRED;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_MANIFESTS_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.domain.Version;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.duraspace.bagit.BagConfig;
import org.duraspace.bagit.BagItDigest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author escowles
 * @since 2016-12-13
 */
public class BagProfileTest {

    private final String testValue = "test-value";
    private final String defaultBag = "bag";

    // profile locations
    private final String defaultProfilePath = "profiles/profile.json";
    private final String extraTagsPath = "profiles/profileWithExtraTags.json";
    private final String invalidPath = "profiles/invalidProfile.json";
    private final String invalidSerializationPath = "profiles/invalidProfileSerializationError.json";

    // config locations
    private final String bagitConfig = "configs/bagit-config.yml";
    private final String bagitConfigBadAccess = "configs/bagit-config-bad-access.yml";
    private final String bagitConfigMissingAccess = "configs/bagit-config-missing-access.yml";
    private final String bagitConfigNoAptrust = "configs/bagit-config-no-aptrust.yml";

    private final Version defaultVersion = new Version(1, 0);
    private Path targetDir;

    private final Logger logger = LoggerFactory.getLogger(BagProfileTest.class);

    @BeforeEach
    public void setup() throws URISyntaxException {
        final URL url = this.getClass().getClassLoader().getResource("sample");
        targetDir = Paths.get(Objects.requireNonNull(url).toURI());
    }

    @Test
    public void testBuiltInProfiles() {
        // validate that the string passed in the constructor matches the string used in the switch statement
        for (BagProfile.BuiltIn value : BagProfile.BuiltIn.values()) {
            assertEquals(value, BagProfile.BuiltIn.from(value.getIdentifier()));
        }
    }

    @Test()
    public void testBuiltInNotSupported() {
        assertThrows(IllegalArgumentException.class,
            ()->{
                BagProfile.BuiltIn.from("test");
            });
    }

    @Test
    public void testBasicProfileFromFile() throws Exception {
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(defaultProfilePath)));

        final String md5 = BagItDigest.MD5.bagitName();
        final String sha1 = BagItDigest.SHA1.bagitName();
        final String sha256 = BagItDigest.SHA256.bagitName();
        final String sha512 = BagItDigest.SHA512.bagitName();
        assertTrue(profile.getPayloadDigestAlgorithms().contains(md5));
        assertTrue(profile.getPayloadDigestAlgorithms().contains(sha1));
        assertTrue(profile.getPayloadDigestAlgorithms().contains(sha256));
        assertTrue(profile.getPayloadDigestAlgorithms().contains(sha512));

        assertFalse(profile.getTagDigestAlgorithms().contains(md5));
        assertTrue(profile.getTagDigestAlgorithms().contains(sha1));
        assertTrue(profile.getTagDigestAlgorithms().contains(sha256));
        assertTrue(profile.getTagDigestAlgorithms().contains(sha512));

        assertTrue(profile.getMetadataFields().get(SOURCE_ORGANIZATION_KEY).isRequired());
        assertFalse(profile.getMetadataFields().get(SOURCE_ORGANIZATION_KEY).isRepeatable());
        assertEquals(profile.getMetadataFields().get(SOURCE_ORGANIZATION_KEY).getDescription(),
                     SOURCE_ORGANIZATION_KEY);
        assertTrue(profile.getMetadataFields().get(ORGANIZATION_ADDRESS_KEY).isRequired());
        assertTrue(profile.getMetadataFields().get(ORGANIZATION_ADDRESS_KEY).isRepeatable());
        assertTrue(profile.getMetadataFields().get(CONTACT_NAME_KEY).isRequired());
        assertTrue(profile.getMetadataFields().get(CONTACT_PHONE_KEY).isRequired());
        assertTrue(profile.getMetadataFields().get(BAG_SIZE_KEY).isRequired());
        assertTrue(profile.getMetadataFields().get(BAGGING_DATE_KEY).isRequired());
        assertTrue(profile.getMetadataFields().get(PAYLOAD_OXUM_KEY).isRequired());
        assertFalse(profile.getMetadataFields().get(CONTACT_EMAIL_KEY).isRequired());

        assertTrue(profile.getMetadataFields().get(BAGIT_PROFILE_IDENTIFIER).isRepeatable());
        assertFalse(profile.getMetadataFields().get(BAGIT_PROFILE_IDENTIFIER).isRequired());
        assertFalse(profile.getMetadataFields().get(BAGIT_PROFILE_IDENTIFIER).isRecommended());
        assertEquals(profile.getMetadataFields().get(BAGIT_PROFILE_IDENTIFIER).getDescription(), "No description");

        assertTrue(profile.getSectionNames().stream().allMatch(t -> t.equalsIgnoreCase(BAG_INFO)));

        assertFalse(profile.isAllowFetch());
        assertEquals(BagProfile.Serialization.OPTIONAL, profile.getSerialization());
        assertTrue(profile.getAcceptedBagItVersions().contains("0.97"));
        assertTrue(profile.getAcceptedBagItVersions().contains("1.0"));
        assertTrue(profile.getAcceptedSerializations().contains("application/tar"));
        assertTrue(profile.getTagFilesAllowed().contains("*"));
        assertTrue(profile.getTagFilesRequired().isEmpty());
        assertTrue(profile.getAllowedTagAlgorithms().isEmpty());
        assertTrue(profile.getAllowedPayloadAlgorithms().isEmpty());
        assertThat(profile.getProfileMetadata())
            .isNotNull()
            .containsKey(EXTERNAL_DESCRIPTION_KEY)
            .hasEntrySatisfying(EXTERNAL_DESCRIPTION_KEY, entry -> assertThat(entry).contains("✓"));
    }

    @Test
    public void testLoadsEmptyMap() throws URISyntaxException, IOException {
        final String profilePath = "profiles/profileNoBagInfo.json";
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(profilePath)));
        final Map<String, ProfileFieldRule> bagInfoFields = profile.getMetadataFields(BAG_INFO);
        assertNotNull(bagInfoFields);
        assertTrue(bagInfoFields.isEmpty());
    }

    @Test
    public void testExtendedProfile() throws Exception {
        final String aptrustInfo = "APTrust-Info";
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(extraTagsPath)));

        assertTrue(profile.getSectionNames().stream().anyMatch(t -> t.equalsIgnoreCase(BAG_INFO)));
        assertTrue(profile.getSectionNames().stream().anyMatch(t -> t.equalsIgnoreCase(aptrustInfo)));
        assertTrue(profile.getSectionNames().stream().noneMatch(t -> t.equalsIgnoreCase("Wrong-Tags")));
        assertTrue(profile.getMetadataFields(aptrustInfo).containsKey(TITLE_KEY));
        assertTrue(profile.getMetadataFields(aptrustInfo).containsKey(ACCESS_KEY));
        assertTrue(profile.getMetadataFields(aptrustInfo).get(ACCESS_KEY).getValues().contains("Consortia"));
        assertTrue(profile.getMetadataFields(aptrustInfo).get(ACCESS_KEY).getValues().contains("Institution"));
        assertTrue(profile.getMetadataFields(aptrustInfo).get(ACCESS_KEY).getValues().contains("Restricted"));
    }


    @Test
    public void testGoodConfig() throws Exception {
        final BagConfig config = new BagConfig(Files.newBufferedReader(resolveResourcePath(bagitConfig)));
        final Map<String, Map<String, String>> configAsMap =
            config.getTagFiles().stream()
                  .collect(Collectors.toMap(identity(), config::getFieldsForTagFile));
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(extraTagsPath)));
        profile.validateConfig(config);
        profile.validateTagFiles(configAsMap);
    }

    @Test
    public void testBadAccessValue() throws Exception {
        assertThrows(RuntimeException.class,
            ()->{
                final BagConfig config = new BagConfig(Files.newBufferedReader(resolveResourcePath(
                    bagitConfigBadAccess)));
                final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(extraTagsPath)));
                profile.validateConfig(config);
            });
    }

    @Test
    public void testMissingAccessValue() throws Exception {
        assertThrows(RuntimeException.class,
            ()->{
                final BagConfig config = new BagConfig(Files.newBufferedReader(resolveResourcePath(
                    bagitConfigMissingAccess)));
                final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(
                    extraTagsPath)));
                profile.validateConfig(config);
            });
    }

    @Test
    public void testMissingSectionNotNeeded() throws Exception {
        final BagConfig config = new BagConfig(Files.newBufferedReader(resolveResourcePath(bagitConfigNoAptrust)));
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(defaultProfilePath)));
        profile.validateConfig(config);
    }

    @Test
    public void testMissingSectionRequired() throws Exception {
        assertThrows(RuntimeException.class,
            ()->{
                final BagConfig config = new BagConfig(Files.newBufferedReader(
                    resolveResourcePath(bagitConfigNoAptrust)));
                final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(extraTagsPath)));
                profile.validateConfig(config);
            });
    }

    @Test
    public void testAllProfilesPassValidation() throws IOException {
        final Path profiles = Paths.get("src/main/resources/profiles");

        Files.list(profiles).forEach(path -> {
            String profileIdentifier = path.getFileName().toString();
            profileIdentifier = profileIdentifier.substring(0, profileIdentifier.indexOf("."));
            logger.debug("Validating {}", profileIdentifier);
            BagProfile profile = null;
            try {
                profile = new BagProfile(BagProfile.BuiltIn.from(profileIdentifier));
            } catch (IOException e) {
                fail(e.getMessage());
            }

            validateProfile(Objects.requireNonNull(profile));
        });

    }

    @Test
    public void testInvalidBagProfile() throws IOException, URISyntaxException {
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(invalidPath)));
        try {
            assertThat(profile.getIdentifier()).isEmpty();
            validateProfile(profile);
            fail("Should throw an exception");
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            // check that the error message contains each failed section
            assertTrue(message.contains(BAGIT_PROFILE_INFO));
            assertTrue(message.contains(BAGIT_PROFILE_IDENTIFIER));
            assertTrue(message.contains(ACCEPT_SERIALIZATION));
            assertTrue(message.contains(MANIFESTS_REQUIRED));
            assertTrue(message.contains(TAG_MANIFESTS_REQUIRED));
            assertTrue(message.contains(TAG_FILES_REQUIRED));
            assertTrue(message.contains(ACCEPT_BAGIT_VERSION));
        }
    }

    @Test
    public void testInvalidBagProfileSerializationTypo() throws IOException, URISyntaxException {
        final BagProfile profile = new BagProfile(Files.newInputStream(resolveResourcePath(invalidSerializationPath)));
        try {
            validateProfile(profile);
            fail("Should throw an exception");
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            // check that the serialization field failed to parse
            assertTrue(message.contains("Unknown Serialization"));
        }
    }

    /**
     * Validates this {@link BagProfile} according to the BagIt Profiles specification found at
     * https://bagit-profiles.github.io/bagit-profiles-specification/
     *
     * This checks the following fields:
     *
     * BagIt-Profile-Info
     * Existence of the Source-Organization, External-Description, Version, BagIt-Profile-Identifier, and
     * BagIt-Profile-Version fields
     *
     * Serialization
     * Is equal to one of "forbidden", "required", or "optional"
     *
     * Accept-Serialization
     * If serialization has a value of required or optional, at least one value is needed.
     *
     * Manifests-Allowed
     * If specified, the {@link BagProfile#getPayloadDigestAlgorithms()} must be a subset of
     * {@link BagProfile#getAllowedPayloadAlgorithms()}
     *
     * Tag-Manifests-Allowed
     * If specified, the {@link BagProfile#getTagDigestAlgorithms()} must be a subset of
     * {@link BagProfile#getAllowedTagAlgorithms()}
     *
     * Tag-Files-Allowed
     * If specified, the {@link BagProfile#getTagFilesRequired()} must be a subset of
     * {@link BagProfile#getTagFilesAllowed()}. If not specified, all tags must match the '*' glob
     *
     * Accept-BagIt-Version
     * At least one version is required
     */
    private void validateProfile(final BagProfile profile) {
        final StringBuilder errors = new StringBuilder();

        // Bag-Profile-Info
        final List<String> expectedInfoFields = Arrays.asList(SOURCE_ORGANIZATION_KEY,
                                                              EXTERNAL_DESCRIPTION_KEY,
                                                              PROFILE_VERSION,
                                                              BAGIT_PROFILE_IDENTIFIER,
                                                              BAGIT_PROFILE_VERSION);
        final Map<String, String> bagInfo = profile.getProfileMetadata();
        for (final String expected : expectedInfoFields) {
            if (!bagInfo.containsKey(expected)) {
                if (errors.isEmpty()) {
                    errors.append("Error(s) in BagIt-Profile-Info:\n");
                }
                errors.append("  * Missing key ").append(expected).append("\n");
            }
        }

        // Serialization / Accept-Serialization
        final BagProfile.Serialization serialization = profile.getSerialization();
        if (serialization == BagProfile.Serialization.REQUIRED || serialization == BagProfile.Serialization.OPTIONAL) {
            if (profile.getAcceptedSerializations().isEmpty()) {
                errors.append("Serialization value of ").append(serialization)
                      .append(" requires at least one value in the Accept-Serialization field!\n");
            }
        } else if(serialization == BagProfile.Serialization.UNKNOWN) {
            errors.append("Unknown Serialization value ").append(serialization)
                  .append(". Allowed values are forbidden, required, or optional.\n");
        }

        // Manifests-Allowed / Manifests-Required
        final Set<String> allowedPayloadAlgorithms = profile.getAllowedPayloadAlgorithms();
        final Set<String> payloadDigestAlgorithms = profile.getPayloadDigestAlgorithms();
        if (!(allowedPayloadAlgorithms.isEmpty() || isSubset(payloadDigestAlgorithms, allowedPayloadAlgorithms))) {
            errors.append("Manifests-Required must be a subset of Manifests-Allowed!\n");
        }

        // Tag-Manifests-Allowed / Tag-Manifests-Required
        final Set<String> allowedTagAlgorithms = profile.getAllowedTagAlgorithms();
        final Set<String> tagDigestAlgorithms = profile.getTagDigestAlgorithms();
        if (!(allowedTagAlgorithms.isEmpty() || isSubset(tagDigestAlgorithms, allowedTagAlgorithms))) {
            errors.append("Tag-Manifests-Required must be a subset of Tag-Manifests-Allowed!\n");
        }

        // Tag-Files-Allowed / Tag-Files-Required
        final Set<String> tagFilesAllowed = profile.getTagFilesAllowed();
        final Set<String> tagFilesRequired = profile.getTagFilesRequired();
        if (!(tagFilesAllowed.isEmpty() || isSubset(tagFilesRequired, tagFilesAllowed))) {
            errors.append("Tag-Files-Required must be a subset of Tag-Files-Allowed!\n");
        }

        if (profile.getAcceptedBagItVersions().isEmpty()) {
            errors.append("Accept-BagIt-Version requires at least one entry!");
        }

        if (!errors.isEmpty()) {
            errors.insert(0, "Bag Profile json does not conform to BagIt Profiles specification! " +
                             "The following errors occurred:\n");
            throw new RuntimeException(errors.toString());
        }
    }

    /**
     * Check to see if a collection (labelled as {@code subCollection}) is a subset of the {@code superCollection}
     *
     * @param subCollection   the sub collection to iterate against and check if elements are contained within
     *                        {@code superCollection}
     * @param superCollection the super collection containing all the elements
     * @param <T>             the type of each collection
     * @return true if all elements of {@code subCollection} are contained within {@code superCollection}
     */
    private <T> boolean isSubset(final Collection<T> subCollection, final Collection<T> superCollection) {
        for (T t : subCollection) {
            if (!superCollection.contains(t)) {
                return false;
            }
        }

        return true;
    }

    @Test
    public void testValidateBag() throws IOException, URISyntaxException {
        final Bag bag = new Bag();
        bag.setVersion(defaultVersion);
        bag.setRootDir(targetDir.resolve(defaultBag));
        final BagProfile bagProfile = new BagProfile(Files.newInputStream(resolveResourcePath(defaultProfilePath)));

        putRequiredBagInfo(bag, bagProfile);
        putRequiredManifests(bag.getTagManifests(), bagProfile.getTagDigestAlgorithms());
        putRequiredManifests(bag.getPayLoadManifests(), bagProfile.getPayloadDigestAlgorithms());
        putRequiredTags(bag, bagProfile);

        bagProfile.validateBag(bag);
    }

    @Test
    public void testValidateBagFailure() throws IOException {
        final Long fetchLength = 0L;
        final Path fetchFile = Paths.get("data/fetch.txt");
        final URL fetchUrl = new URL("http://localhost/data/fetch.txt");

        final Bag bag = new Bag();
        bag.setItemsToFetch(Collections.singletonList(new FetchItem(fetchUrl, fetchLength, fetchFile)));
        bag.setVersion(new Version(0, 0));
        bag.setRootDir(targetDir.resolve(defaultBag));
        final BagProfile bagProfile = new BagProfile(BagProfile.BuiltIn.APTRUST);

        putRequiredBagInfo(bag, bagProfile);
        putRequiredManifests(bag.getPayLoadManifests(), bagProfile.getPayloadDigestAlgorithms());
        putRequiredTags(bag, bagProfile);

        try {
            bagProfile.validateBag(bag);
            fail("Validation did not throw an exception");
        } catch (RuntimeException e) {
            final String message = e.getMessage();
            assertTrue(message.contains("Profile does not allow a fetch.txt"));
            assertTrue(message.contains("No tag manifest"));
            assertTrue(message.contains("Required tag file \"aptrust-info.txt\" does not exist"));
            assertTrue(message.contains("Could not read info from \"aptrust-info.txt\""));
            assertTrue(message.contains("BagIt version incompatible"));

            assertFalse(message.contains("Missing tag manifest algorithm"));
        }
    }

    /**
     * Add required tag files to a Bag from a BagProfile
     *
     * @param bag the Bag
     * @param bagProfile the BagProfile defining the required files
     */
    private void putRequiredTags(final Bag bag, final BagProfile bagProfile) {
        final List<String> tagManifestExpected = Arrays.asList("manifest-sha1.txt", "bag-info.txt", "bagit.txt");

        // Always populate with the files we expect to see
        for (String expected : tagManifestExpected) {
            final Path required = Paths.get(expected);
            for (Manifest manifest : bag.getTagManifests()) {
                manifest.getFileToChecksumMap().put(required, testValue);
            }
        }

        for (String requiredTag : bagProfile.getTagFilesRequired()) {
            final Path requiredPath = Paths.get(requiredTag);
            for (Manifest manifest : bag.getTagManifests()) {
                manifest.getFileToChecksumMap().put(requiredPath, testValue);
            }
        }
    }

    /**
     *
     * @param manifests the manifests to add algorithms to
     * @param algorithms the algorithms to add
     */
    private void putRequiredManifests(final Set<Manifest> manifests, final Set<String> algorithms) {
        for (String algorithm : algorithms) {
            manifests.add(new Manifest(StandardSupportedAlgorithms.valueOf(algorithm.toUpperCase())));
        }
    }

    /**
     *
     * @param bag the Bag to set info fields for
     * @param profile the BagProfile defining the required info fields
     */
    private void putRequiredBagInfo(final Bag bag, final BagProfile profile) {
        final Map<String, ProfileFieldRule> bagInfoMeta = profile.getMetadataFields(BAG_INFO);
        bag.getMetadata().add(BAGIT_PROFILE_IDENTIFIER, profile.getIdentifier());
        for (Map.Entry<String, ProfileFieldRule> entry : bagInfoMeta.entrySet()) {
            if (entry.getValue().isRequired())  {
                bag.getMetadata().add(entry.getKey(), testValue);
            }
        }
    }

    private Path resolveResourcePath(final String resource) throws URISyntaxException {
        final URL url = this.getClass().getClassLoader().getResource(resource);
        return Paths.get(Objects.requireNonNull(url).toURI());
    }

}
