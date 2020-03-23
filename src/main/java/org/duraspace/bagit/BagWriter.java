/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility to write BagIt bags.
 *
 * @author escowles
 * @since 2016-12-15
 */
public class BagWriter {

    private File bagDir;
    private File dataDir;
    private Set<BagItDigest> algorithms;

    private Map<BagItDigest, Map<File, String>> payloadRegistry;
    private Map<BagItDigest, Map<File, String>> tagFileRegistry;
    private Map<String, Map<String, String>> tagRegistry;

    /**
     * This map provides a way to retrieve all ongoing MessageDigests so that multiple checksums
     * can easily be run and retrieved
     */
    private Map<BagItDigest, DigestOutputStream> activeStreams;

    /**
     * Version of the BagIt specification implemented
     */
    public static String BAGIT_VERSION = "0.97";

    /**
     * Create a new, empty Bag
     * @param bagDir The base directory for the Bag (will be created if it doesn't exist)
     * @param algorithms Set of digest algorithms to use for manifests (e.g., "md5", "sha1", or "sha256")
     */
    public BagWriter(final File bagDir, final Set<BagItDigest> algorithms) {
        this.bagDir = bagDir;
        this.dataDir = new File(bagDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        this.algorithms = algorithms;
        payloadRegistry = new HashMap<>();
        tagFileRegistry = new HashMap<>();
        tagRegistry = new HashMap<>();

        final Map<String, String> bagitValues = new HashMap<>();
        bagitValues.put("BagIt-Version", BAGIT_VERSION);
        bagitValues.put("Tag-File-Character-Encoding", "UTF-8");
        tagRegistry.put("bagit.txt", bagitValues);

        activeStreams = new HashMap<>();
    }

    /**
     * Get the Bag's root directory
     * @return File object for the directory
     */
    public File getRootDir() {
        return bagDir;
    }

    /**
     * Register checksums of payload (data) files
     * @param algorithm Checksum digest algorithm name (e.g., "SHA-1")
     * @param filemap Map of Files to checksum values
     */
    public void registerChecksums(final BagItDigest algorithm, final Map<File, String> filemap) {
        if (!algorithms.contains(algorithm)) {
            throw new RuntimeException("Invalid algorithm: " + algorithm);
        }
        payloadRegistry.put(algorithm, filemap);
    }

    /**
     * Add tags (metadata) to the Bag
     * @param key Filename of the tag file (e.g., "bag-info.txt")
     * @param values Map containing field/value pairs
     */
    public void addTags(final String key, final Map<String, String> values) {
        tagRegistry.put(key, values);
    }

    /**
     * Get the current tag (metadata) of the Bag
     * @param key Filename of the tag file (e.g., "bag-info.txt")
     * @return Map of field/value pairs
     */
    public Map<String, String> getTags(final String key) {
        return tagRegistry.get(key);
    }

    /**
     * Write metadata and finalize Bag
     * @throws IOException when an I/O error occurs
     */
    public void write() throws IOException {
        writeManifests("manifest", payloadRegistry, true);
        for (String tagFile : tagRegistry.keySet()) {
            writeTagFile(tagFile);
        }
        writeManifests("tagmanifest", tagFileRegistry, false);
    }

    private void writeManifests(final String prefix, final Map<BagItDigest, Map<File, String>> registry,
                                final boolean registerToTags) throws IOException {
        final String delimiter = "  ";
        final char backslash = '\\';
        final char bagitSeparator = '/';
        final Path bag = bagDir.toPath();

        for (final BagItDigest algorithm : algorithms) {
            final Map<File, String> filemap = registry.get(algorithm);
            if (filemap != null) {
                final File f = new File(bagDir, prefix + "-" + algorithm.bagitName() + ".txt");
                try (OutputStream out = streamFor(f.toPath())) {
                    for (final File payload : filemap.keySet()) {
                        // replace all occurrences of backslashes, which are not allowed per the bagit spec
                        final String relative = bag.relativize(payload.toPath()).toString()
                                                   .replace(backslash, bagitSeparator);
                        final String line = filemap.get(payload) + delimiter + relative;
                        out.write(line.getBytes());
                        out.write("\n".getBytes());

                        if (registerToTags) {
                            for (Map.Entry<BagItDigest, DigestOutputStream> entry : activeStreams.entrySet()) {
                                addTagChecksum(entry.getKey(), f, entry.getValue().getMessageDigest());
                            }
                        }
                        activeStreams.clear();
                    }
                }
            }
        }
    }

    private void writeTagFile(final String key) throws IOException {
        final Map<String, String> values = tagRegistry.get(key);
        if (values != null) {
            final File f = new File(bagDir, key);

            try (OutputStream out = streamFor(f.toPath())) {
                for (final String field : values.keySet()) {
                    final byte[] bytes = (field + ": " + values.get(field) + "\n").getBytes();
                    out.write(bytes);
                }
            }

            for (Map.Entry<BagItDigest, DigestOutputStream> entry : activeStreams.entrySet()) {
                addTagChecksum(entry.getKey(), f, entry.getValue().getMessageDigest());
            }
        }

        activeStreams.clear();
    }

    /**
     * Create an {@link OutputStream} for a given {@link Path} which can be used to write data to the file.
     * This wraps the returned {@link OutputStream} with {@link DigestOutputStream}s in order to create a checksum
     * for the file as it is being written. There is one {@link DigestOutputStream} per {@link BagItDigest} in this
     * classes registered {@code algorithms}. Each {@link DigestOutputStream} is stored in the {@code activeStreams} so
     * that it can be retrieved later on.
     *
     * @param file the {@link Path} to create an {@link OutputStream} for
     * @return the {@link OutputStream}
     * @throws IOException if there is an error creating the {@link OutputStream}
     */
    private OutputStream streamFor(final Path file) throws IOException {
        OutputStream lastStream = Files.newOutputStream(file);
        for (BagItDigest algorithm : algorithms) {
            final DigestOutputStream dos = new DigestOutputStream(lastStream, algorithm.messageDigest());
            activeStreams.put(algorithm, dos);
            lastStream = dos;
        }

        return lastStream;
    }

    private void addTagChecksum(final BagItDigest algorithm, final File f, final MessageDigest digest) {
        if (digest != null) {
            final Map<File, String> m = tagFileRegistry.computeIfAbsent(algorithm, key -> new HashMap<>());
            m.put(f, HexEncoder.toString(digest.digest()));
        }
    }
}
