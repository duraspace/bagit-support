/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.Test;

/**
 * @author dbernstein
 * @since Dec 14, 2016
 */
public class BagConfigTest {

    @Test
    public void testFromFile() {
        final File testFile = new File("src/test/resources/configs/bagit-config.yml");
        final BagConfig config = new BagConfig(testFile);

        final Map<String, String> bagInfo = config.getBagInfo();
        assertNotNull(bagInfo);
        assertThat(bagInfo.get(BagConfig.SOURCE_ORGANIZATION_KEY))
            .isNotNull()
            .isEqualTo("My University ✓");

        assertTrue(config.hasTagFile("aptrust-info.txt"));
        final Map<String, String> customTags = config.getFieldsForTagFile("aptrust-info.txt");
        assertNotNull(customTags);
        assertEquals(customTags.get(BagConfig.ACCESS_KEY).toUpperCase(), BagConfig.AccessTypes.RESTRICTED.name());

    }

}
