/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_GTAR;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_GZIP;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_TAR;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_X_COMPRESSED_TAR;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_X_GTAR;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_X_GZIP;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_X_TAR;
import static org.duraspace.bagit.serialize.SerializationSupport.APPLICATION_ZIP;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.duraspace.bagit.exception.BagProfileException;
import org.duraspace.bagit.profile.BagProfile;
import org.junit.jupiter.api.Test;

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

    @Test
    public void testDeserializerForFileNotFound() throws Exception {
        assertThrows(UncheckedIOException.class,
            ()->{
                final BagProfile profile = new BagProfile(BagProfile.BuiltIn.FEDORA_IMPORT_EXPORT);
                final Path notFound = Paths.get("file-not-found");
                SerializationSupport.deserializerFor(notFound, profile);
            });
    }

    @Test
    public void testDeserializerNoProfileSupport() throws Exception {
        assertThrows(BagProfileException.class,
            ()->{
                // Currently the fedora profile only supports application/tar, so send a file which is not a tarball
                // see: profiles/fedora-import-export.json
                final BagProfile profile = new BagProfile(BagProfile.BuiltIn.FEDORA_IMPORT_EXPORT);
                final URL url = SerializationSupportTest.class.getClassLoader().getResource(
                    "sample/compress/bag-zip.zip");
                assertThat(url).isNotNull();

                final Path notSupported = Paths.get(url.toURI());
                SerializationSupport.deserializerFor(notSupported, profile);
            });
    }

    @Test
    public void testDeserializationNotSupported() throws Exception {
        assertThrows(UnsupportedOperationException.class,
            ()->{
                // A deserialization format which exists in a profile, but not by bagit-support
                // currently json because we have many json resources available
                final URL profileUrl = SerializationSupportTest.class.getClassLoader().getResource(
                    "profiles/profile.json");
                assertThat(profileUrl).isNotNull();
                final Path profileJson = Paths.get(profileUrl.toURI());
                final BagProfile profile = new BagProfile(Files.newInputStream(profileJson));

                SerializationSupport.deserializerFor(profileJson, profile);
            });
    }

    @Test
    public void testSerializerNoProfileSupport() throws Exception {
        assertThrows(BagProfileException.class,
            ()->{
                // A serialization/compression format which does not exist in the profile, currently xz
                final String xz = "application/x-xz";
                final BagProfile profile = new BagProfile(BagProfile.BuiltIn.FEDORA_IMPORT_EXPORT);
                SerializationSupport.serializerFor(xz, profile);
            });
    }

    @Test
    public void testSerializerNotSupported() throws Exception {
        assertThrows(UnsupportedOperationException.class,
            ()->{
                // A serialization/compression format which exists in a profile, but not by bagit-support
                // currently 7zip fits this
                final String sevenZip = "application/x-7z-compressed";
                final BagProfile profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
                SerializationSupport.serializerFor(sevenZip, profile);
            });
    }

}