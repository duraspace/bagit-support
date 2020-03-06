/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for common deserialization operations for {@link gov.loc.repository.bagit.domain.Bag}s. Each deserializer
 * is instantiated independently of what it is working on so that only {@link BagDeserializer#deserialize(Path)}
 * needs to be called.
 *
 * @author mikejritter
 * @since 2020-02-11
 */
public interface BagDeserializer {

    /**
     * Deserialize a {@link gov.loc.repository.bagit.domain.Bag} located at the given {@code path}. This will create a
     * version of the bag in the parent directory of the given {@code path}.
     *
     * @param path the {@link Path} to the serialized version of a {@link gov.loc.repository.bagit.domain.Bag}
     * @return the {@link Path} to the deserialized bag
     * @throws IOException if there are any errors deserializing the bag
     */
    Path deserialize(final Path path) throws IOException;

}
