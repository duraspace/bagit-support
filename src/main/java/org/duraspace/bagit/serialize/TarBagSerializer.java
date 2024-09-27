/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * Serialize a BagIt Bag into a Tar archive
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class TarBagSerializer implements BagSerializer {
    private final String extension = ".tar";

    @Override
    public Path serialize(final Path root) throws IOException {
        final Path parent = root.getParent().toAbsolutePath();
        final String bagName = root.getFileName().toString();

        final Path serializedBag = parent.resolve(bagName + extension);
        try (final OutputStream os = Files.newOutputStream(serializedBag);
            final TarArchiveOutputStream tar = new TarArchiveOutputStream(os);
            final Stream<Path> files = Files.walk(root)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

            final Iterator<Path> itr = files.iterator();
            while (itr.hasNext()) {
                final Path bagEntry = itr.next();
                final String name = parent.relativize(bagEntry).toString();
                final TarArchiveEntry entry = tar.createArchiveEntry(bagEntry.toFile(), name);
                tar.putArchiveEntry(entry);
                if (bagEntry.toFile().isFile()) {
                    FileUtils.copyFile(bagEntry.toFile(), tar);
                }
                tar.closeArchiveEntry();
            }
        }

        return serializedBag;
    }

    @Override
    public Path serializeWithTimestamp(final Path root, final Long lastModifiedTime) throws IOException {
        throw new UnsupportedOperationException("This method is not supported.");
    }
}
