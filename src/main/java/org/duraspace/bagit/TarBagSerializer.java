package org.duraspace.bagit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Serialize a BagIt Bag into a Tar archive
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class TarBagSerializer implements BagSerializer {
    private final String extension = ".tar";

    @Override
    public Path serialize(final Path root) {
        final Path parent = root.getParent().toAbsolutePath();
        final String bagName = root.getFileName().toString();

        final Path serializedBag = parent.resolve(bagName + extension);
        try(final OutputStream os = Files.newOutputStream(serializedBag);
            final TarArchiveOutputStream tar = new TarArchiveOutputStream(os)) {
            final List<Path> files = Files.walk(root).collect(Collectors.toList());
            for (Path bagEntry : files) {
                final String name = parent.relativize(bagEntry).toString();
                final ArchiveEntry entry = tar.createArchiveEntry(bagEntry.toFile(), name);
                tar.putArchiveEntry(entry);
                if (bagEntry.toFile().isFile()) {
                    try (InputStream inputStream = Files.newInputStream(bagEntry)) {
                        IOUtils.copy(inputStream, tar);
                    }
                }
                tar.closeArchiveEntry();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return serializedBag;
    }

}
