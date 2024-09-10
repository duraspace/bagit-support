/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.compress.archivers.zip.X000A_NTFS;
import org.apache.commons.compress.archivers.zip.X5455_ExtendedTimestamp;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serialize a BagIt bag into a zip archive without compression
 *
 * @author mikejritter
 * @since 2020-02-24
 */
public class ZipBagSerializer implements BagSerializer {
    private final String extension = ".zip";

    private static final long DEFAULT_MODIFIED_DATE = 1036368000L * 1000;

    private final Logger logger = LoggerFactory.getLogger(ZipBagSerializer.class);

    @Override
    public Path serialize(final Path root) throws IOException {
        final Path parent = root.getParent().toAbsolutePath();
        final String bagName = root.getFileName().toString();

        final Path serializedBag = parent.resolve(bagName + extension);
        try(final OutputStream os = Files.newOutputStream(serializedBag);
            final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(os)) {

            // it would be nice not to have to collect the files which are walked, but we're required to try/catch
            // inside of a lambda which isn't the prettiest. maybe a result could be returned which contains either a
            // Path or the Exception thrown... just an idea
            final List<Path> files = Files.walk(root).toList();
            for (Path bagEntry : files) {
                final String name = parent.relativize(bagEntry).toString();
                final ZipArchiveEntry entry = zip.createArchiveEntry(bagEntry.toFile(), name);
                zip.putArchiveEntry(entry);
                if (bagEntry.toFile().isFile()) {
                    FileUtils.copyFile(bagEntry.toFile(), zip);
                }
                zip.closeArchiveEntry();
            }
        }

        return serializedBag;
    }

    @Override
    public Path serializeWithTimestamp(final Path root, final Long lastModifiedTime) throws IOException {
        logger.info("Serializing bag with timestamp: {}", root.getFileName());

        final Path parent = root.getParent().toAbsolutePath();

        final String bagName = root.getFileName().toString();

        final DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

        final Path serializedBag = parent.resolve(bagName + extension);
        try(final OutputStream os = Files.newOutputStream(serializedBag);
            final ZipArchiveOutputStream zip = new ZipArchiveOutputStream(os)) {

            // it would be nice not to have to collect the files which are walked, but we're required to try/catch
            // inside of a lambda which isn't the prettiest. maybe a result could be returned which contains either a
            // Path or the Exception thrown... just an idea
            final List<Path> files = Files.walk(root).toList();
            for (Path bagEntry : files) {
                final String name = parent.relativize(bagEntry).toString();
                final ZipArchiveEntry entry = zip.createArchiveEntry(bagEntry.toFile(), name);

                final FileTime time;
                if (lastModifiedTime != 0) {
                    time = FileTime.fromMillis(lastModifiedTime);
                } else {
                    time = FileTime.fromMillis(DEFAULT_MODIFIED_DATE);
                }

                logger.debug("Setting ZipEntry creation, last modified and last access  times to: {}",
                    df.format(time.toMillis()));
                Files.setLastModifiedTime(bagEntry, time);

                final X5455_ExtendedTimestamp extendedTimestamp = new X5455_ExtendedTimestamp();
                extendedTimestamp.setCreateFileTime(time);
                extendedTimestamp.setModifyFileTime(time);
                extendedTimestamp.setAccessFileTime(time);
                entry.addExtraField(extendedTimestamp);

                final X000A_NTFS ntfsTimestamp = new X000A_NTFS();
                ntfsTimestamp.setCreateFileTime(time);
                ntfsTimestamp.setModifyFileTime(time);
                ntfsTimestamp.setAccessFileTime(time);
                entry.addExtraField(ntfsTimestamp);

                zip.putArchiveEntry(entry);
                if (bagEntry.toFile().isFile()) {
                    FileUtils.copyFile(bagEntry.toFile(), zip);
                }
                zip.closeArchiveEntry();
            }
        }

        return serializedBag;
    }
}
