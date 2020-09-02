/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.profile;

import static org.duraspace.bagit.profile.ProfileValidationUtil.SYSTEM_GENERATED_FIELD_NAMES;
import static org.duraspace.bagit.profile.ProfileValidationUtil.validate;
import static org.duraspace.bagit.profile.ProfileValidationUtil.validateManifest;
import static org.duraspace.bagit.profile.ProfileValidationUtil.validateTagIsAllowed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import org.duraspace.bagit.exception.ProfileValidationException;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for profile validation.
 *
 * @author dbernstein
 * @since Dec 14, 2016
 */
public class ProfileValidationUtilTest {

    private static final String FIELD1 = "field1";

    private static final String FIELD2 = "field2";

    private Map<String, ProfileFieldRule> rules;

    private LinkedHashMap<String, String> fields;

    private static final boolean required = true;
    private static final boolean repeatable = true;
    private static final boolean recommended = false;

    @Before
    public void setup() {
        rules = new HashMap<>();
        fields = new LinkedHashMap<>();
        final Set<String> set = new HashSet<>();
        set.add("value1");
        set.add("value2");
        set.add("value3");
        final ProfileFieldRule field = new ProfileFieldRule(required, repeatable, recommended, "", set);
        rules.put(FIELD1, field);
    }

    @Test
    public void testEnforceValues() throws ProfileValidationException {
        fields.put(FIELD1, "value1");
        validate("profile-section", rules, fields);
    }

    @Test(expected = ProfileValidationException.class)
    public void testEnforceValuesMissingRequired() throws ProfileValidationException {
        fields.put("field2", "value1");
        validate("profile-section", rules, fields);
    }

    @Test(expected = ProfileValidationException.class)
    public void testEnforceValuesInvalidValue() throws ProfileValidationException {
        fields.put(FIELD1, "invalidValue");
        validate("profile-section", rules, fields);
    }

    @Test
    public void testMultipleValidationErrorsInOneExceptionMessage() {
        fields.put(FIELD1, "invalidValue");
        rules.put(FIELD2, new ProfileFieldRule(required, repeatable, recommended,
                                               "field 2 should fail", Collections.emptySet()));
        fields.put("field3", "any value");
        try {
            validate("profile-section", rules, fields);
            fail("previous line should have failed.");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains(FIELD1));
            assertTrue(e.getMessage().contains(FIELD2));
            assertFalse(e.getMessage().contains("field3"));
        }
    }

    @Test
    public void testIgnoreSystemGeneratedFields() throws Exception {
        fields.put(FIELD1, "value1");

        for (String fieldName : SYSTEM_GENERATED_FIELD_NAMES) {
            rules.put(fieldName, null);
        }

        validate("profile-section", rules, fields);

    }

    @Test
    public void testOnDiskInfoValidation() throws ProfileValidationException, IOException {
        rules.clear();
        rules.put("Source-Organization",
                  new ProfileFieldRule(required, repeatable, recommended, "", Collections.singleton("bagit-support")));
        rules.put("Organization-Address",
                  new ProfileFieldRule(required, repeatable, recommended, "",
                                       Collections.singleton("localhost-dot-localdomain")));
        final String bagInfoPath = "src/test/resources/sample/bag/bag-info.txt";
        validate("profile-section", rules, Paths.get(bagInfoPath));
    }

    @Test
    public void testGlobalTagMatch() throws ProfileValidationException {
        final Set<String> allowedTags = Collections.singleton("**");
        validateTagIsAllowed(Paths.get("test-info.txt"), allowedTags);
        validateTagIsAllowed(Paths.get("test-info/test-info.txt"), allowedTags);
    }

    @Test
    public void testEmptyListValidates() throws ProfileValidationException {
        validateTagIsAllowed(Paths.get("test-info.txt"), Collections.emptySet());
    }

    @Test
    public void testUniqueTagMatch() throws ProfileValidationException {
        final Set<String> allowedTags = Collections.singleton("test-info.txt");
        validateTagIsAllowed(Paths.get("test-info.txt"), allowedTags);
    }

    @Test(expected = ProfileValidationException.class)
    public void testTagIsNotAllowed() throws ProfileValidationException {
        final Set<String> allowedTags = Collections.singleton("test-tag.txt");
        validateTagIsAllowed(Paths.get("test-info.txt"), allowedTags);
    }

    @Test
    public void testSubDirectoryMatch() throws ProfileValidationException {
        final Set<String> allowedTags = Collections.singleton("ddp-tags/test-*");
        validateTagIsAllowed(Paths.get("ddp-tags/test-info.txt"), allowedTags);
        validateTagIsAllowed(Paths.get("ddp-tags/test-extra-info.txt"), allowedTags);
    }

    @Test
    public void testTagValidateIgnoresRequired() throws ProfileValidationException {
        final Set<String> allowedTags = Collections.singleton("test-info.txt");
        validateTagIsAllowed(Paths.get("bag-info.txt"), allowedTags);
        validateTagIsAllowed(Paths.get("bagit.txt"), allowedTags);
        validateTagIsAllowed(Paths.get("manifest-md5.txt"), allowedTags);
        validateTagIsAllowed(Paths.get("tagmanifest-sha512.txt"), allowedTags);
    }

    @Test
    public void testVerifyManifests() {
        final String type = "TEST";
        final Manifest manifest = new Manifest(StandardSupportedAlgorithms.SHA1);
        final Set<Manifest> manifests = Collections.singleton(manifest);
        final Set<String> constraints = Collections.singleton("sha1");

        // check that no errors were returned
        assertTrue(validateManifest(manifests, constraints, constraints, type).isEmpty());
        assertTrue(
            validateManifest(manifests, constraints, Collections.emptySet(), type).isEmpty());
    }

    @Test
    public void testVerifyManifestFailure() {
        final String type = "TEST";
        final Manifest manifest = new Manifest(StandardSupportedAlgorithms.SHA1);
        final Set<Manifest> manifests = Collections.singleton(manifest);
        final Set<String> required = Collections.singleton("md5");
        final Set<String> allowed = Collections.singleton("md5");

        final String result = validateManifest(manifests, required, allowed, type);
        assertTrue(result.contains("Missing"));
        assertTrue(result.contains("Unsupported"));
    }

}
