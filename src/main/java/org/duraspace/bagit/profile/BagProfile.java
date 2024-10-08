/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.profile;

import static org.duraspace.bagit.profile.BagProfileConstants.ACCEPT_BAGIT_VERSION;
import static org.duraspace.bagit.profile.BagProfileConstants.ACCEPT_SERIALIZATION;
import static org.duraspace.bagit.profile.BagProfileConstants.ALLOW_FETCH_TXT;
import static org.duraspace.bagit.profile.BagProfileConstants.BAGIT_PROFILE_INFO;
import static org.duraspace.bagit.profile.BagProfileConstants.BAGIT_TAG_SUFFIX;
import static org.duraspace.bagit.profile.BagProfileConstants.BAG_INFO;
import static org.duraspace.bagit.profile.BagProfileConstants.MANIFESTS_ALLOWED;
import static org.duraspace.bagit.profile.BagProfileConstants.MANIFESTS_REQUIRED;
import static org.duraspace.bagit.profile.BagProfileConstants.OTHER_INFO;
import static org.duraspace.bagit.profile.BagProfileConstants.SERIALIZATION;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_FILES_ALLOWED;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_FILES_REQUIRED;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_MANIFESTS_ALLOWED;
import static org.duraspace.bagit.profile.BagProfileConstants.TAG_MANIFESTS_REQUIRED;
import static org.duraspace.bagit.profile.BagProfileConstants.UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import org.duraspace.bagit.BagConfig;
import org.duraspace.bagit.exception.ProfileValidationException;
import org.slf4j.Logger;

/**
 * A BagProfile contains the entire contents of a BagIt profile specified through the profile's json.
 *
 * @author mikejritter
 * @author escowles
 * @since 2016-12-12
 */
public class BagProfile {

    private static final String APTRUST_IDENTIFIER = "aptrust";
    private static final String PERSEIDS_IDENTIFIER = "perseids";
    private static final String METAARCHIVE_IDENTIFIER = "metaarchive";
    private static final String FEDORA_IDENTIFIER = "fedora-import-export";
    private static final String BEYONDTHEREPO_IDENTIFIER = "beyondtherepository";

    public enum Serialization {
        FORBIDDEN, REQUIRED, OPTIONAL, UNKNOWN;

        /**
         * Retrieve the {@link Serialization} from a string representation
         *
         * @param value the String value to use
         * @return the {@link Serialization} the {@code value} is equal to
         */
        public static Serialization of(final String value) {
            switch (value.toLowerCase()) {
                case "forbidden": return FORBIDDEN;
                case "required": return REQUIRED;
                case "optional": return OPTIONAL;
                default: return UNKNOWN;
            }
        }
    }

    /**
     * Enum of the built in profiles which are provided with bagit-support
     */
    public enum BuiltIn {
        APTRUST(APTRUST_IDENTIFIER),
        BEYOND_THE_REPOSITORY(BEYONDTHEREPO_IDENTIFIER),
        FEDORA_IMPORT_EXPORT(FEDORA_IDENTIFIER),
        METAARCHIVE(METAARCHIVE_IDENTIFIER),
        PERSEIDS(PERSEIDS_IDENTIFIER);

        private final String identifier;

        /**
         * Default constructor
         *
         * @param identifier the identifier of the profile
         */
        BuiltIn(final String identifier) {
            this.identifier = identifier;
        }

        /**
         * Retrieve the BuiltIn profile from an identifier with an additional profile to use as a match against a
         * "default" identifier
         *
         * @param identifier the identifier of the profile
         * @param profile the {@link BuiltIn} profile to return if the identifier matches "default"
         * @return the matching {@link BuiltIn} profile
         */
        public static BuiltIn from(final String identifier, final BuiltIn profile) {
            if ("default".equals(identifier.toLowerCase())) {
                return profile;
            }
            return from(identifier);
        }

        /**
         * Retrieve a built in profile from an identifier
         *
         * @param identifier the identifier to retrieve a profile for
         * @return the {@link BuiltIn} profile
         * @throws IllegalArgumentException if the {@code identifier} is not supported
         */
        public static BuiltIn from(final String identifier) {
            switch (identifier.toLowerCase()) {
                case APTRUST_IDENTIFIER: return APTRUST;
                case BEYONDTHEREPO_IDENTIFIER: return BEYOND_THE_REPOSITORY;
                case FEDORA_IDENTIFIER: return FEDORA_IMPORT_EXPORT;
                case METAARCHIVE_IDENTIFIER: return METAARCHIVE;
                case PERSEIDS_IDENTIFIER: return PERSEIDS;
                default: throw new IllegalArgumentException("Unsupported profile identifier. Accepted values are: " +
                                                            Arrays.stream(BuiltIn.values())
                                                                  .map(BuiltIn::getIdentifier)
                                                                  .collect(Collectors.joining(", ")));
            }
        }

        /**
         * Get the identifier associated with the profile
         *
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }
    }

    private static final Logger logger = getLogger(BagProfile.class);

    private boolean allowFetch;
    private Serialization serialization;

    private Set<String> acceptedBagItVersions;
    private Set<String> acceptedSerializations;

    private Set<String> tagFilesAllowed;
    private Set<String> tagFilesRequired;

    private Set<String> allowedPayloadAlgorithms;
    private Set<String> allowedTagAlgorithms;

    private Set<String> payloadDigestAlgorithms;
    private Set<String> tagDigestAlgorithms;

    private final Map<String, Map<String, ProfileFieldRule>> metadataFields = new HashMap<>();
    private final Map<String, String> profileMetadata = new HashMap<>();

    /**
     * Create a BagProfile that uses the {@link BuiltIn} BEYOND_THE_REPOSITORY profile.
     *
     * @throws IOException if the beyondtherepository.json cannot be loaded
     */
    public BagProfile() throws IOException {
        this(BuiltIn.BEYOND_THE_REPOSITORY);
    }

    /**
     * Load a BagProfile from a {@link BuiltIn} profile type
     *
     * @param builtInProfile the supported profile to load
     * @throws IOException if there is an error reading the json
     */
    public BagProfile(final BuiltIn builtInProfile) throws IOException {
        final String resource = "profiles/" + builtInProfile.identifier + ".json";
        final URL resourceURL = this.getClass().getClassLoader().getResource(resource);
        try (InputStream in = Objects.requireNonNull(resourceURL).openStream()) {
            load(in);
        }
    }

    /**
     * Create a BagProfile from a given InputStream
     *
     * @param in InputStream containing the Bag profile JSON document
     * @throws IOException when there is an I/O error reading JSON
     */
    public BagProfile(final InputStream in) throws IOException {
        load(in);
    }

    private void load(final InputStream in) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final JsonNode json = mapper.readTree(new InputStreamReader(in, UTF_8));

        loadProfileInfo(json);

        allowFetch = json.has(ALLOW_FETCH_TXT) ? json.get(ALLOW_FETCH_TXT).asBoolean() : true;
        serialization = json.has(SERIALIZATION) ? Serialization.of(json.get(SERIALIZATION).asText())
                                                : Serialization.OPTIONAL;

        acceptedBagItVersions = arrayValues(json, ACCEPT_BAGIT_VERSION);
        acceptedSerializations = arrayValues(json, ACCEPT_SERIALIZATION);

        tagFilesAllowed = arrayValues(json, TAG_FILES_ALLOWED);
        tagFilesRequired = arrayValues(json, TAG_FILES_REQUIRED);

        allowedPayloadAlgorithms = arrayValues(json, MANIFESTS_ALLOWED);
        allowedTagAlgorithms = arrayValues(json, TAG_MANIFESTS_ALLOWED);

        payloadDigestAlgorithms = arrayValues(json, MANIFESTS_REQUIRED);
        tagDigestAlgorithms = arrayValues(json, TAG_MANIFESTS_REQUIRED);

        metadataFields.put(BAG_INFO.toLowerCase(), metadataFields(json.get(BAG_INFO)));

        if (json.get(OTHER_INFO) != null) {
            loadOtherTags(json);
        }
    }

    private void loadProfileInfo(final JsonNode json) {
        final JsonNode tag = json.get(BAGIT_PROFILE_INFO);
        if (tag != null) {
            tag.fields().forEachRemaining(entry -> profileMetadata.put(entry.getKey(), entry.getValue().asText()));
        }
    }

    private void loadOtherTags(final JsonNode json) {
        final JsonNode arrayTags = json.get(OTHER_INFO);
        if (arrayTags != null && arrayTags.isArray()) {
            final Iterator<JsonNode> arrayEntries = arrayTags.elements();
            while (arrayEntries.hasNext()) {
                final JsonNode entries = arrayEntries.next();
                final Iterator<Map.Entry<String, JsonNode>> fields = entries.fields();
                while (fields.hasNext()) {
                    final Map.Entry<String, JsonNode> entry = fields.next();
                    final String tagName = entry.getKey().toLowerCase();
                    metadataFields.put(tagName, metadataFields(entry.getValue()));
                }
            }
        }
        logger.debug("metadataFields is {}", metadataFields);
    }

    private static Set<String> arrayValues(final JsonNode json, final String key) {
        final JsonNode values = json.get(key);

        if (values == null) {
            return Collections.emptySet();
        }

        final Set<String> results = new HashSet<>();
        for (int i = 0; i < values.size(); i++) {
            results.add(values.get(i).asText());
        }
        return results;
    }

    /**
     * Loads required tags and allowed values
     *
     * @param json json to parse
     * @return map of tags => set of allowed values
     */
    private static Map<String, ProfileFieldRule> metadataFields(final JsonNode json) {
        if (json == null) {
            return Collections.emptyMap();
        }

        final Map<String, ProfileFieldRule> results = new HashMap<>();
        // why not use the entry to iterate?
        for (final Iterator<String> it = json.fieldNames(); it.hasNext(); ) {
            // fields to pass to the ProfileFieldRule constructor
            boolean required = false;
            boolean repeatable = true;
            boolean recommended = false;
            String description = "No description";

            final String name = it.next();
            final JsonNode field = json.get(name);

            // read each of the fields for the ProfileFieldRule:
            // required, repeated, recommended, description, and values
            final JsonNode requiredNode = field.get("required");
            if (requiredNode != null && requiredNode.asBoolean()) {
                required = requiredNode.asBoolean();
            }

            final JsonNode repeatedNode = field.get("repeatable");
            if (repeatedNode != null) {
                repeatable = repeatedNode.asBoolean();
            }

            final JsonNode recommendedNode = field.get("recommended");
            if (recommendedNode != null && recommendedNode.asBoolean()) {
                recommended = recommendedNode.asBoolean();
            }

            final JsonNode descriptionNode = field.get("description");
            if (descriptionNode != null && !descriptionNode.asText().isEmpty()) {
                description = descriptionNode.asText();
            }

            final Set<String> values = arrayValues(field, "values");

            results.put(name, new ProfileFieldRule(required, repeatable, recommended, description, values));
        }

        return results;
    }

    /**
     * Retrieve the BagIt-Profile-Identifier for this profile
     *
     * @return the BagIt-Profile-Identifier, or an empty string if none is found
     */
    public String getIdentifier() {
        return profileMetadata.getOrDefault(BagProfileConstants.BAGIT_PROFILE_IDENTIFIER, "");
    }

    /**
     * Boolean flag allowing a fetch.txt file
     *
     * @return true if fetch.txt is allowed, false otherwise
     */
    public boolean isAllowFetch() {
        return allowFetch;
    }

    /**
     * Get the support of serialization for a Bag.
     *
     * Allowed values are: forbidden, required, and optional
     *
     * @return String value of "forbidden", "required", or "optional"
     */
    public Serialization getSerialization() {
        return serialization;
    }

    /**
     * Get the supported BagIt versions
     *
     * @return Set of BagIt version numbers
     */
    public Set<String> getAcceptedBagItVersions() {
        return acceptedBagItVersions;
    }

    /**
     * Get the supported serialization formats
     *
     * If {@link BagProfile#getSerialization()} has a value of required or optional, at least one value is needed.
     * If {@link BagProfile#getSerialization()} is forbidden, this has no meaning
     *
     * @return Set of serialization formats
     */
    public Set<String> getAcceptedSerializations() {
        return acceptedSerializations;
    }

    /**
     * Get the names of allowed tag files; supports unix style globbing
     *
     * All the tag files listed in {@link BagProfile#getTagFilesRequired()} must be in included in this
     *
     * @return Set of allowed tag files
     */
    public Set<String> getTagFilesAllowed() {
        return tagFilesAllowed;
    }

    /**
     * Get the tag files which are required to exist
     *
     * @return Set of tag filenames
     */
    public Set<String> getTagFilesRequired() {
        return tagFilesRequired;
    }

    /**
     * Get the payload algorithms which are allowed
     *
     * When specified along with {@link BagProfile#getPayloadDigestAlgorithms()}, this must include at least all of the
     * manifest types listed in {@link BagProfile#getPayloadDigestAlgorithms()}.
     *
     * @return Set of digest algorithm names
     */
    public Set<String> getAllowedPayloadAlgorithms() {
        return allowedPayloadAlgorithms;
    }

    /**
     * Get the tag manifest algorithms which are allowed.
     *
     * When specified along with {@link BagProfile#getTagDigestAlgorithms()}, this must include at least all of the tag
     * manifest types listed in {@link BagProfile#getTagDigestAlgorithms()}.
     *
     * @return Set of digest algorithm names
     */
    public Set<String> getAllowedTagAlgorithms() {
        return allowedTagAlgorithms;
    }

    /**
     * Get the required digest algorithms for payload manifests.
     *
     * @return Set of digest algorithm names
     */
    public Set<String> getPayloadDigestAlgorithms() {
        return payloadDigestAlgorithms;
    }

    /**
     * Get the required digest algorithms for tag manifests.
     *
     * @return Set of digest algorithm names
     */
    public Set<String> getTagDigestAlgorithms() {
        return tagDigestAlgorithms;
    }

    /**
     * Get the required Bag-Info metadata fields.
     *
     * @return A map of field names to a ProfileFieldRule containing acceptance criteria
     */
    public Map<String, ProfileFieldRule> getMetadataFields() {
        return getMetadataFields(BAG_INFO);
    }

    /**
     * Get the required tags for the extra tag file
     *
     * @param tagFile the tag file to get tags for
     * @return map of tag = set of acceptable values, or null if tagFile doesn't exist
     */
    public Map<String, ProfileFieldRule> getMetadataFields(final String tagFile) {
        return metadataFields.get(tagFile.toLowerCase());
    }

    /**
     * Get all the section names in this profile, which can be used with getMetadataFields().
     *
     * @return set of section names
     */
    public Set<String> getSectionNames() {
        return metadataFields.keySet();
    }

    /**
     * Get the BagIt-Profile-Info section describing the BagIt Profile
     *
     * @return map of fields names to text descriptions
     */
    public Map<String, String> getProfileMetadata() {
        return profileMetadata;
    }

    /**
     * Validate a given BagConfig against the current profile
     *
     * @param config the BagConfig
     */
    public void validateConfig(final BagConfig config) {
        checkRequiredTagsExist(config.getTagFiles());
        for (final String section : config.getTagFiles()) {
            validateTag(section, config.getFieldsForTagFile(section));
        }
    }

    /**
     * Validate a configuration for tag files based on a mapping of BagIt tag filenames to key-value pairs.
     *
     * e.g. the filename "bag-info.txt" could contain the pairs "Source-Organization: DuraSpace" and
     * "Organization-Address: The Cloud"
     *
     * @param config the Map containing the configuration of BagIt tag files
     */
    public void validateTagFiles(final Map<String, Map<String, String>> config) {
        checkRequiredTagsExist(config.keySet());
        config.forEach(this::validateTag);
    }

    /**
     * Test that all required tag files exist
     *
     * @param tags the name of each tag file to check
     */
    private void checkRequiredTagsExist(final Set<String> tags) {
        for (String section : metadataFields.keySet()) {
            final String expected = section + BAGIT_TAG_SUFFIX;
            if (!tags.contains(expected)) {
                throw new RuntimeException("Missing configuration for required tag file " + expected);
            }
        }
    }

    /**
     * Validate a Mapping of key value pairs for a tag file
     *
     * @param filename the name of the tag file to validate
     * @param fields A mapping of tag file names and their fields to validate
     */
    private void validateTag(final String filename, final Map<String, String> fields) {
        // strip the trailing file extension
        final String section = getSection(filename);
        logger.debug("Checking validation for {}", section);
        if (metadataFields.containsKey(section)) {
            try {
                ProfileValidationUtil.validate(section, getMetadataFields(section), fields);
                ProfileValidationUtil.validateTagIsAllowed(Paths.get(filename), tagFilesAllowed);
            } catch (ProfileValidationException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    /**
     * Normalize a filename to be what we expect is held in the MetadataFields key set
     *
     * @param filename the filename to normalize
     * @return the filename without a tag extension, so that it can be used with the metadataFields
     */
    private String getSection(final String filename) {
        // use two regexps
        // the main pattern: two groups - a wildcard matcher for the filename and the tag suffix
        // the replacement: just the first capture group
        final String replacement = "$1";
        final Pattern tagEnding = Pattern.compile("(.*)(\\" + BAGIT_TAG_SUFFIX + ")");
        final Matcher matcher = tagEnding.matcher(filename.toLowerCase());
        return matcher.replaceAll(replacement);
    }

    /**
     * Validate a given {@link Bag} against the current profile
     *
     * @param bag the Bag
     */
    public void validateBag(final Bag bag) {
        logger.info("Starting Bag to BagProfile conformance validator");

        final String tagIdentifier = "tag";
        final String fetchIdentifier = "fetch.txt";
        final String payloadIdentifier = "payload";
        final StringBuilder errors = new StringBuilder();

        final Path root = bag.getRootDir();
        final Set<Manifest> foundPayloadManifests = bag.getPayLoadManifests();
        final Set<Manifest> foundTagManifests = bag.getTagManifests();

        // check fetch rule
        if (!allowFetch && (!bag.getItemsToFetch().isEmpty() || Files.exists(root.resolve(fetchIdentifier)))) {
            errors.append("Profile does not allow a fetch.txt but fetch file found!\n");
        }

        // check payload manifest algorithms
        errors.append(ProfileValidationUtil.validateManifest(foundPayloadManifests, payloadDigestAlgorithms,
                                                             allowedPayloadAlgorithms, payloadIdentifier));

        // check tag manifest rules files allowed
        // the reporting can be redundant if no tag manifests are found, so only check the allowed algorithms and
        // tag files IF we have at least one tag manifest
        if (foundTagManifests.isEmpty()) {
            errors.append("No tag manifest found!\n");
        } else {
            errors.append(ProfileValidationUtil.validateManifest(foundTagManifests, tagDigestAlgorithms,
                                                                 allowedTagAlgorithms, tagIdentifier));

            // grab the first tag manifest and use that to check all registered tag files
            final Manifest manifest = foundTagManifests.iterator().next();
            final Set<Path> existingTagFiles = manifest.getFileToChecksumMap().keySet();

            for (Path tag : existingTagFiles) {
                final Path relativePath = tag.startsWith(root) ? root.relativize(tag) : tag;
                try {
                    ProfileValidationUtil.validateTagIsAllowed(relativePath, tagFilesAllowed);
                } catch (ProfileValidationException e) {
                    errors.append(e.getMessage());
                }
            }
        }

        // check all required tag files exist
        for (String tagName : tagFilesRequired) {
            final Path requiredTag = root.resolve(tagName);
            if (!requiredTag.toFile().exists()) {
                errors.append("Required tag file \"").append(tagName).append("\" does not exist!\n");
            }
        }

        // check *-info required fields
        for (String section : metadataFields.keySet()) {
            final String tagFile = section.toLowerCase() + BAGIT_TAG_SUFFIX;
            final Path resolved = root.resolve(tagFile);
            try {
                ProfileValidationUtil.validate(section, metadataFields.get(section), resolved);
            } catch (IOException e) {
                // error - could not read info
                errors.append("Could not read info from \"").append(tagFile).append("\"!\n");
            } catch (ProfileValidationException e) {
                errors.append(e.getMessage());
            }
        }

        // check allowed bagit versions
        if (!acceptedBagItVersions.contains(bag.getVersion().toString())) {
            errors.append("BagIt version incompatible; accepted versions are ")
                  .append(acceptedBagItVersions)
                  .append("\n");
        }

        // serialization seems unnecessary as the import export tool does not support importing serialized bags
        if (serialization == Serialization.REQUIRED) {
            logger.warn("Bag Profile requires serialization, import will continue if the bag has been deserialized");
        }

        // finally, if we have any errors throw an exception
        if (!errors.isEmpty()) {
            throw new RuntimeException("Bag profile validation failure: The following errors occurred: \n" +
                                       errors.toString());
        }
    }
}
