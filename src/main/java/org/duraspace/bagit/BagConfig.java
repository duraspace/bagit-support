/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A convenience class for parsing and storing bagit-config.yml information. The bagit-config.yml represents
 * user-defined properties to be included in the bag-info.txt.
 *
 * @author dbernstein
 * @since Dec 14, 2016
 */
public class BagConfig {

    private static final Logger logger = LoggerFactory.getLogger(BagConfig.class);

    public enum AccessTypes {
        RESTRICTED, INSTITUTION, CONSORTIA
    }

    public static final String BAG_INFO_KEY = "bag-info.txt";

    private static final String APTRUST_INFO_KEY = "aptrust-info.txt";

    public static final String SOURCE_ORGANIZATION_KEY = "Source-Organization";

    public static final String ORGANIZATION_ADDRESS_KEY = "Organization-Address";

    public static final String CONTACT_NAME_KEY = "Contact-Name";

    public static final String CONTACT_PHONE_KEY = "Contact-Phone";

    public static final String CONTACT_EMAIL_KEY = "Contact-Email";

    public static final String EXTERNAL_DESCRIPTION_KEY = "External-Description";

    public static final String EXTERNAL_IDENTIFIER_KEY = "External-Identifier";

    public static final String INTERNAL_SENDER_DESCRIPTION_KEY = "Internal-Sender-Description";

    public static final String INTERNAL_SENDER_IDENTIFIER_KEY = "Internal-Sender-Identifier";

    public static final String BAGGING_DATE_KEY = "Bagging-Date";

    public static final String BAG_SIZE_KEY = "Bag-Size";

    public static final String PAYLOAD_OXUM_KEY = "Payload-Oxum";

    public static final String BAG_GROUP_IDENTIFIER = "Bag-Group-Identifier";

    public static final String TITLE_KEY = "Title";

    public static final String ACCESS_KEY = "Access";

    private final Map<String, Map<String, String>> map;

    /**
     * Default constructor
     *
     * @param bagConfigReader a reader for a bagit config yaml file (see src/test/resources/bagit-config.yml)
     */
    @SuppressWarnings("unchecked")
    public BagConfig(final Reader bagConfigReader) {
        YamlReader yaml = null;
        try {
            yaml = new YamlReader(bagConfigReader);
            map = (Map<String, Map<String, String>>) yaml.read();
        } catch (YamlException e) {
            logger.error("Unable to parse yaml", e);
            throw new IllegalStateException(e);
        } finally {
            if (yaml != null) {
                try {
                    yaml.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Returns an immutable map of bag info properties.
     *
     * @return a map of bag info properties
     */
    public Map<String, String> getBagInfo() {
        return Collections.unmodifiableMap(this.map.getOrDefault(BAG_INFO_KEY, Collections.emptyMap()));
    }

    /**
     * Returns an immutable map of aptrust info properties.
     *
     * @return a map of aptrust info properties
     */
    public Map<String, String> getAPTrustInfo() {
        return Collections.unmodifiableMap(this.map.get(APTRUST_INFO_KEY));
    }

    /**
     * Returns all the tag files from the config
     *
     * @return set of tag filenames
     */
    public Set<String> getTagFiles() {
        return map.keySet();
    }

    /**
     * Check if a tag file is listed in bag config
     *
     * @param tagFile the tag filename
     * @return true if it is list, false if not
     */
    public boolean hasTagFile(final String tagFile) {
        return map.containsKey(tagFile);
    }

    /**
     * Returns an immutable map of custom tags for a tag file
     *
     * @param tagFile name of the tag file to get fields for
     * @return a map of filenames to key-value property maps
     */
    public Map<String, String> getFieldsForTagFile(final String tagFile) {
        return Collections.unmodifiableMap(map.get(tagFile));
    }
}
