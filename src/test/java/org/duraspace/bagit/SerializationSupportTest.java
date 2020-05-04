/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_GTAR;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_GZIP;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_TAR;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_X_COMPRESSED_TAR;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_X_GTAR;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_X_GZIP;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_X_TAR;
import static org.duraspace.bagit.SerializationSupport.APPLICATION_ZIP;

import java.util.Map;

import org.junit.Test;

/**
 * Verify our type mappings are what we expect
 *
 * all zip types map to application/zip
 * all tar types map to application/tar
 * all gzip types map to application/gzip
 *
 * @author mikejritter
 */
public class SerializationSupportTest {

    private final Map<String, String> commonTypeMap = SerializationSupport.getCommonTypeMap();

    @Test
    public void testZipMappings() {
        final String zipIdentifier = "zip";

        assertThat(commonTypeMap).hasEntrySatisfying(zipIdentifier,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_ZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_ZIP,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_ZIP));
    }

    @Test
    public void testTarMappings() {
        final String tarIdentifier = "tar";

        assertThat(commonTypeMap).hasEntrySatisfying(tarIdentifier,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_TAR));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_TAR,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_TAR));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_X_TAR,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_TAR));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_GTAR,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_TAR));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_X_GTAR,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_TAR));
    }

    @Test
    public void testGZipMappings() {
        final String tgzIdentifier = "tgz";
        final String gzipIdentifier = "gzip";
        final String tarGzIdentifier = "tar+gz";

        assertThat(commonTypeMap).hasEntrySatisfying(tgzIdentifier,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(gzipIdentifier,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(tarGzIdentifier,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_GZIP,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_X_GZIP,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
        assertThat(commonTypeMap).hasEntrySatisfying(APPLICATION_X_COMPRESSED_TAR,
                                                     value -> assertThat(value).isEqualTo(APPLICATION_GZIP));
    }

}