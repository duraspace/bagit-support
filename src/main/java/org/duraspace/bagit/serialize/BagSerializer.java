/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Serialize a BagIt bag into a single format.
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public interface BagSerializer {

    /**
     * Serialize a BagIt bag depending on the format defined by the implementing class. This only puts the files into
     * an archive, with the name of the {@code root} directory serving as the name of the final file.
     *
     * @param root the {@link Path} which is the top level directory of the BagIt bag
     * @return the {@link Path} to the serialized BagIt bag
     * @throws IOException if there is an error writing to the archive
     */
    Path serialize(Path root) throws IOException;

    /**
     * Serialize a BagIt bag and set file creation, last modified, and access times for each zip entry.
     * Setting these times is required to ensure that MD5 checksums of identical bags created at
     * different times will match.
     *
     * This only puts the files into an archive, with the name of the{@code root} directory serving
     * as the name of the final file.
     *
     * @param root the {@link Path} which is the top level directory of the BagIt bag
     * @param lastModifiedTime the time (in milliseconds) to set times in file metadata
     * @return the {@link Path} to the serialized BagIt bag
     * @throws IOException if there is an error writing to the archive
     * @throws UnsupportedOperationException if the child class does not implement this method
     */
    Path serializeWithTimestamp(Path root, Long lastModifiedTime) throws IOException;
}
