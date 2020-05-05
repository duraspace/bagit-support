/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class to retrieve {@link BagDeserializer}s from a mime type
 *
 * @author mikejritter
 * @since 2020-02-11
 */
public class SerializationSupport {

    private static final Logger logger = LoggerFactory.getLogger(SerializationSupport.class);

    // zip
    protected static final String APPLICATION_ZIP = "application/zip";

    // tar + gtar
    protected static final String APPLICATION_TAR = "application/tar";
    protected static final String APPLICATION_GTAR = "application/gtar";
    protected static final String APPLICATION_X_TAR = "application/x-tar";
    protected static final String APPLICATION_X_GTAR = "application/x-gtar";

    // gzip
    protected static final String APPLICATION_GZIP = "application/gzip";
    protected static final String APPLICATION_X_GZIP = "application/x-gzip";
    protected static final String APPLICATION_X_COMPRESSED_TAR = "application/x-compressed-tar";

    public static final Set<String> ZIP_TYPES = Collections.singleton(APPLICATION_ZIP);
    public static final Set<String> TAR_TYPES = new HashSet<>(Arrays.asList(APPLICATION_TAR, APPLICATION_X_TAR,
                                                                            APPLICATION_GTAR, APPLICATION_X_GTAR));
    public static final Set<String> GZIP_TYPES = new HashSet<>(Arrays.asList(APPLICATION_GZIP, APPLICATION_X_GTAR,
                                                                             APPLICATION_X_COMPRESSED_TAR));

    /**
     * The commonTypeMap acts as a way to coerce various types onto a single format. E.g. handing application/gtar and
     * application/tar will go through the same class, so we map application/gtar to application/tar.
     */
    private static Map<String, String> commonTypeMap = initCommonTypeMapping();

    private SerializationSupport() {
    }

    /**
     * Just a way to instantiate the {@code commonTypeMap}
     *
     * @return the map of supported application types
     */
    private static Map<String, String> initCommonTypeMapping() {
        commonTypeMap = new HashMap<>();
        commonTypeMap.put("zip", APPLICATION_ZIP);
        commonTypeMap.put(APPLICATION_ZIP, APPLICATION_ZIP);

        commonTypeMap.put("tar", APPLICATION_TAR);
        commonTypeMap.put(APPLICATION_TAR, APPLICATION_TAR);
        commonTypeMap.put(APPLICATION_GTAR, APPLICATION_TAR);
        commonTypeMap.put(APPLICATION_X_TAR, APPLICATION_TAR);
        commonTypeMap.put(APPLICATION_X_GTAR, APPLICATION_TAR);

        commonTypeMap.put("tgz", APPLICATION_GZIP);
        commonTypeMap.put("gzip", APPLICATION_GZIP);
        commonTypeMap.put("tar+gz", APPLICATION_GZIP);
        commonTypeMap.put(APPLICATION_GZIP, APPLICATION_GZIP);
        commonTypeMap.put(APPLICATION_X_GZIP, APPLICATION_GZIP);
        commonTypeMap.put(APPLICATION_X_COMPRESSED_TAR, APPLICATION_GZIP);
        return commonTypeMap;
    }

    /**
     * Visible for testing only
     * Retrieve a copy of the commonTypeMap
     *
     * @return a copy of the commonTypeMap
     */
    protected static Map<String, String> getCommonTypeMap() {
        return new HashMap<>(commonTypeMap);
    }

    /**
     * Get a {@link BagDeserializer} for a given content type. Currently supported are:
     * zip ({@link SerializationSupport#ZIP_TYPES}) - {@link ZipBagDeserializer}
     * tar ({@link SerializationSupport#TAR_TYPES}) - {@link TarBagDeserializer}
     * tar+gz ({@link SerializationSupport#GZIP_TYPES}) - {@link GZipBagDeserializer}
     *
     * @param serializedBag the Bag (still serialized) to get a {@link BagDeserializer} for
     * @param profile the {@link BagProfile} to ensure that the content type is allowed
     * @return the {@link BagDeserializer}
     * @throws UnsupportedOperationException if the content type is not supported
     * @throws RuntimeException if the {@link BagProfile} does not allow serialization
     */
    public static BagDeserializer deserializerFor(final Path serializedBag, final BagProfile profile) {
        final Tika tika = new Tika();
        final String contentType;

        try {
            // use a less strict approach to handling content types through the commonTypeMap
            final String detectedType = tika.detect(serializedBag);
            contentType = commonTypeMap.getOrDefault(detectedType, detectedType);
            logger.debug("{}: {}", serializedBag, contentType);
        } catch (IOException e) {
            logger.error("Unable to get content type for {}", serializedBag);
            throw new RuntimeException(e);
        }

        if (profile.getAcceptedSerializations().contains(contentType)) {
            if (ZIP_TYPES.contains(contentType)) {
                return new ZipBagDeserializer();
            } else if (TAR_TYPES.contains(contentType)) {
                return new TarBagDeserializer();
            } else if (GZIP_TYPES.contains(contentType)) {
                return new GZipBagDeserializer(profile);
            } else {
                throw new UnsupportedOperationException("Unsupported content type " + contentType);
            }
        }

        throw new RuntimeException("BagProfile does not allow " + contentType + ". Accepted serializations are:\n" +
                                   profile.getAcceptedSerializations());
    }

    /**
     * Get a {@link BagSerializer} for a given content type and {@link BagProfile}. It takes both a short form (zip,
     * tar, gzip) and long form (application/zip, application/tar) version for the content type.
     *
     * @param contentType the content type to get a {@link BagSerializer} for
     * @param profile the {@link BagProfile} used for validating the {@code contentType}
     * @return the {@link BagSerializer}
     * @throws RuntimeException if the {@code contentType} is not supported
     */
    public static BagSerializer serializerFor(final String contentType, final BagProfile profile) {
        final String type = commonTypeMap.getOrDefault(contentType, contentType);
        if (profile.getAcceptedSerializations().contains(type)) {
            if (ZIP_TYPES.contains(type)) {
                return new ZipBagSerializer();
            } else if (TAR_TYPES.contains(type)) {
                return new TarBagSerializer();
            } else if (GZIP_TYPES.contains(type)) {
                return new TarGzBagSerializer();
            } else {
                throw new UnsupportedOperationException("Unsupported content type " + contentType);
            }
        }

        throw new RuntimeException("BagProfile does not allow " + type + ". Accepted serializations are:\n" +
                                   profile.getAcceptedSerializations());
    }

}
