/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    public Path serialize(final Path root) throws IOException {
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
        }

        return serializedBag;
    }

}
