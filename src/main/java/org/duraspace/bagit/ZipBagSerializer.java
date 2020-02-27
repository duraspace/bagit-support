package org.duraspace.bagit;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Serialize a BagIt bag into a zip archive without compression
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class ZipBagSerializer implements BagSerializer {
    private final String extension = ".zip";

    @Override
    public Path serialize(final Path root) {
        final Path parent = root.getParent().toAbsolutePath();
        final String bagName = root.getFileName().toString();

        final Path serializedBag = parent.resolve(bagName + extension);
        try(final OutputStream os = Files.newOutputStream(serializedBag);
            final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(os)) {

            // it would be nice not to have to collect the files which are walked, but we're required to try/catch
            // inside of a lambda which isn't the prettiest. maybe a result could be returned which contains either a
            // Path or the Exception thrown... just an idea
            final List<Path> files = Files.walk(root).collect(Collectors.toList());
            for (Path bagEntry : files) {
                final String name = parent.relativize(bagEntry).toString();
                final ArchiveEntry entry = zip.createArchiveEntry(bagEntry.toFile(), name);
                zip.putArchiveEntry(entry);
                if (bagEntry.toFile().isFile()) {
                    try (InputStream inputStream = Files.newInputStream(bagEntry)) {
                        IOUtils.copy(inputStream, zip);
                    }
                }
                zip.closeArchiveEntry();
            }
        } catch (IOException e) {
            // todo: log or something
            // blooh blah
        }

        return serializedBag;
    }
}
