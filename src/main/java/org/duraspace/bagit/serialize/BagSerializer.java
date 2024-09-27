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
     * Default date/time (in milliseconds since epoch) to set for Zip Entries
     * that do not have a last modified date. If the date/time is not set
     * then it will default to current system date/time.
     * This is less than ideal, as it causes the MD5 checksum of Zip file to
     * change whenever a Zip file is regenerated (even if compressed files are unchanged).
     * 1589346000 seconds * 1000 = May 13, 2020 GMT (the date BagIt-Support 1.0.0 was released)
     */
    long DEFAULT_MODIFIED_DATE = 1589346000L * 1000;

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
     * @param lastModifiedTime the time (in milliseconds) to set time fields in file metadata
     * @return the {@link Path} to the serialized BagIt bag
     * @throws IOException if there is an error writing to the archive
     * @throws UnsupportedOperationException if the child class does not implement this method
     */
    Path serializeWithTimestamp(Path root, Long lastModifiedTime) throws IOException;
}
