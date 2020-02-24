package org.duraspace.bagit;

import java.nio.file.Path;

/**
 * Serialize a BagIt bag into a single format.
 *
 * @author mikejritter
 * @since 2020-02-24
 */
@FunctionalInterface
public interface BagSerializer {

    /**
     * Serialize a BagIt bag depending on the format defined by the implementing class. This only puts the files into
     * an archive, with the name of the {@code root} directory serving as the name of the final file.
     *
     * @param root the {@link Path} which is the top level directory of the BagIt bag
     * @return the {@link Path} to the serialized BagIt bag
     */
    Path serialize(Path root);

}
