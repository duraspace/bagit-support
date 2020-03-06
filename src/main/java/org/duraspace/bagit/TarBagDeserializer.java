/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserializer for {@link gov.loc.repository.bagit.domain.Bag}s serialized using tar
 *
 * @author mikejritter
 * @since 2020-02-11
 */
public class TarBagDeserializer implements BagDeserializer {

    private final Logger logger = LoggerFactory.getLogger(TarBagDeserializer.class);

    protected TarBagDeserializer() {
    }

    @Override
    public Path deserialize(final Path root) throws IOException {
        logger.info("Extracting serialized bag: {}", root.getFileName());

        final Path parent = root.getParent();
        final int rootPathCount = root.getNameCount();
        Optional<String> filename = Optional.empty();
        try (TarArchiveInputStream tais = new TarArchiveInputStream(Files.newInputStream(root))) {
            ArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                final String name = entry.getName();

                logger.debug("Handling entry {}", entry.getName());
                final Path archiveFile = parent.resolve(name);

                if (Files.notExists(archiveFile.getParent())) {
                    Files.createDirectories(archiveFile.getParent());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(archiveFile);
                    if (archiveFile.getNameCount() == rootPathCount) {
                        logger.debug("Archive name is {}", archiveFile.getFileName());
                        filename = Optional.of(archiveFile.getFileName().toString());
                    }
                } else {
                    if (Files.exists(parent.resolve(name))) {
                        logger.warn("File {} already exists!", name);
                    } else {
                        Files.copy(tais, archiveFile);
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
