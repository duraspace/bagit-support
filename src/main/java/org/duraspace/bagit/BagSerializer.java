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
     * @throws IOException if there is an error writing to the archive
     */
    Path serialize(Path root) throws IOException;

}
