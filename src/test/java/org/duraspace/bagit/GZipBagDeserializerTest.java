/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.duraspace.bagit.exception.BagProfileException;
import org.junit.Test;

/**
 * Test the GZipBagDeserializer in the event the compressed archive which has been extracted from a gzip file is not
 * supported by a BagProfile.
 *
 */
public class GZipBagDeserializerTest {

    @Test
    public void testInvalidCompressedBag() throws URISyntaxException, IOException, BagProfileException {
        // Because the serializers operate strictly on Paths, just compress a file with gz
        // This allows us to create a "bag" which is not supported by any profile
        final URL resourcesURI = this.getClass().getClassLoader().getResource("profiles");
        final Path resources = Paths.get(resourcesURI.toURI());
        final Path profileJson = resources.resolve("profile.json");
        final Path invalidGz = resources.resolve("profile.json.gz");

        // compress w/ gzip
        int n = 0;
        final byte[] buffer = new byte[2048];
        try (InputStream bagIn = Files.newInputStream(profileJson);
            GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(Files.newOutputStream(invalidGz))) {
            while (-1 != (n = bagIn.read(buffer))) {
                gzOut.write(buffer, 0, n);
            }
        }

        // for the actually testing, try to deserialize only to run into an exception
        final BagProfile profile = new BagProfile(BagProfile.BuiltIn.BEYOND_THE_REPOSITORY);
        final BagDeserializer bagDeserializer = SerializationSupport.deserializerFor(invalidGz, profile);

        try {
            bagDeserializer.deserialize(invalidGz);
        } catch (IOException e) {
            assertThat(e).hasMessageContaining("BagProfile does not allow application/json");
        }
    }

}