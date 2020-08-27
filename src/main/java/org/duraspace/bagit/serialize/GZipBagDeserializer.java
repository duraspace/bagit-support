/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.duraspace.bagit.BagProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deflate a gzipped bag so that the underlying bag can continue to be deserialized.
 *
 * @author mikejritter
 * @since 2020-02-11
 */
public class GZipBagDeserializer implements BagDeserializer {

    private final Logger logger = LoggerFactory.getLogger(GZipBagDeserializer.class);

    private final BagProfile profile;

    protected GZipBagDeserializer(final BagProfile profile) {
        this.profile = profile;
    }

    @Override
    public Path deserialize(final Path root) throws IOException {
        final Path parent = root.getParent();
        final String nameWithExtension = root.getFileName().toString();
        final int dotIdx = nameWithExtension.lastIndexOf(".");
        final String filename = (dotIdx == -1) ? nameWithExtension : nameWithExtension.substring(0, dotIdx);
        final Path serializedBag = parent.resolve(filename);

        // Deflate the gzip to get the base file
        logger.info("Deflating gzipped bag: {}", filename);
        try (final InputStream is = Files.newInputStream(root);
             final InputStream bis = new BufferedInputStream(is);
             final GzipCompressorInputStream gzipIS = new GzipCompressorInputStream(bis)) {

            Files.copy(gzipIS, serializedBag);
        } catch (FileAlreadyExistsException ex) {
            logger.warn("{} already decompressed! Continuing with deserialization.", root);
        }

        // Get a deserializer for the deflated content
        final BagDeserializer deserializer = SerializationSupport.deserializerFor(serializedBag, profile);
        return deserializer.deserialize(serializedBag);
    }
}
