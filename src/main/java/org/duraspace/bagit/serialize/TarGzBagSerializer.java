/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * Serialize a BagIt bag to be a tar+gzip archive.
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class TarGzBagSerializer implements BagSerializer {
    private final String extension = ".tar.gz";

    @Override
    public Path serialize(final Path root) throws IOException {
        final Path parent = root.getParent().toAbsolutePath();
        final String bagName = root.getFileName().toString();

        final Path serializedBag = parent.resolve(bagName + extension);
        try(final OutputStream os = Files.newOutputStream(serializedBag);
            final GZIPOutputStream gzip = new GZIPOutputStream(os);
            final TarArchiveOutputStream tar = new TarArchiveOutputStream(gzip)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
            final List<Path> files = Files.walk(root).toList();
            for (Path bagEntry : files) {
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
}
