/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.serialize;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserializer for {@link gov.loc.repository.bagit.domain.Bag}s serialized using zip
 *
 * @author mikejritter
 * @since 2020-02-01
 */
public class ZipBagDeserializer implements BagDeserializer {

    private final Logger logger = LoggerFactory.getLogger(ZipBagDeserializer.class);

    protected ZipBagDeserializer() {
    }

    @Override
    public Path deserialize(final Path root) throws IOException {
        logger.info("Extracting serialized bag: {}", root.getFileName());

        final Path parent = root.getParent();
        final int rootNameCount = root.getNameCount();
        Optional<String> filename = Optional.empty();
        try (ZipArchiveInputStream inputStream = new ZipArchiveInputStream(Files.newInputStream(root))) {
            ArchiveEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                final String name = entry.getName();

                logger.debug("Handling entry {}", entry.getName());
                final Path archiveFile = parent.resolve(name);

                if (Files.notExists(archiveFile.getParent())) {
                    Files.createDirectories(archiveFile.getParent());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(archiveFile);
                    if (archiveFile.getNameCount() == rootNameCount) {
                        logger.debug("Archive name is {}", archiveFile.getFileName());
                        filename = Optional.of(archiveFile.getFileName().toString());
                    }
                } else {
                    if (Files.exists(parent.resolve(name))) {
                        logger.warn("File {} already exists!", name);
                    } else {
                        Files.copy(inputStream, archiveFile);
                    }
                }
            }
        }

        final String extracted = filename.orElseGet(() -> {
            // get the name from the tarball minus the extension
            final String rootName = root.getFileName().toString();
            final int dotIdx = rootName.lastIndexOf(".");
            return rootName.substring(0, dotIdx);
        });
        return parent.resolve(extracted);
    }
}
