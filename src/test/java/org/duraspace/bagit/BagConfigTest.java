/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author dbernstein
 * @since Dec 14, 2016
 */
public class BagConfigTest {

    @Test
    public void testFromFile() throws IOException {
        final String APTRUST_TAG = "aptrust-info.txt";
        final String APTRUST_RESTRICTED = "Restricted";

        final Path testFile = Paths.get("src/test/resources/configs/bagit-config.yml");
        final BagConfig config = new BagConfig(Files.newBufferedReader(testFile));

        final Map<String, String> bagInfo = config.getBagInfo();
        assertNotNull(bagInfo);
        assertThat(bagInfo.get(BagConfig.SOURCE_ORGANIZATION_KEY))
            .isNotNull()
            .isEqualTo("My University ✓");

        assertTrue(config.hasTagFile(APTRUST_TAG));
        final Map<String, String> customTags = config.getFieldsForTagFile(APTRUST_TAG);
        assertNotNull(customTags);
        assertThat(customTags.get(BagConfig.ACCESS_KEY)).isEqualTo(APTRUST_RESTRICTED);

    }

}
