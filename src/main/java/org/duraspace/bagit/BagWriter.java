/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.duraspace.bagit.BagItDigest.MD5;
import static org.duraspace.bagit.BagItDigest.SHA1;
import static org.duraspace.bagit.BagItDigest.SHA256;
import static org.duraspace.bagit.BagItDigest.SHA512;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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

        final Map<String, String> bagitValues = new TreeMap<>();
        bagitValues.put("BagIt-Version", BAGIT_VERSION);
        bagitValues.put("Tag-File-Character-Encoding", "UTF-8");
        tagRegistry.put("bagit.txt", bagitValues);
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
        writeManifests("manifest", payloadRegistry);
        for (String tagFile : tagRegistry.keySet()) {
            writeTagFile(tagFile);
        }
        writeManifests("tagmanifest", tagFileRegistry);
    }

    private void writeManifests(final String prefix, final Map<BagItDigest, Map<File, String>> registry)
            throws IOException {
        final String delimiter = "  ";
        final char backslash = '\\';
        final char bagitSeparator = '/';
        final Path bag = bagDir.toPath();

        for (final BagItDigest algorithm : algorithms) {
            final Map<File, String> filemap = registry.get(algorithm);
            if (filemap != null) {
                final File f = new File(bagDir, prefix + "-" + algorithm.bagitName() + ".txt");
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                    for (final File payload : filemap.keySet()) {
                        // replace all occurrences of backslashes, which are not allowed per the bagit spec
                        final String relative = bag.relativize(payload.toPath()).toString()
                                                   .replace(backslash, bagitSeparator);
                        out.println(filemap.get(payload) + delimiter + relative);
                    }
                }
            }
        }
    }

    private void writeTagFile(final String key) throws IOException {
        final Map<String, String> values = tagRegistry.get(key);
        if (values != null) {
            final File f = new File(bagDir, key);

            MessageDigest md5 = null;
            MessageDigest sha1 = null;
            MessageDigest sha256 = null;
            MessageDigest sha512 = null;
            if (algorithms.contains(MD5)) {
                md5 = MD5.messageDigest();
            }
            if (algorithms.contains(SHA1)) {
                sha1 = SHA1.messageDigest();
            }
            if (algorithms.contains(SHA256)) {
                sha256 = SHA256.messageDigest();
            }
            if (algorithms.contains(SHA512)) {
                sha512 = SHA512.messageDigest();
            }

            try (OutputStream out = new FileOutputStream(f)) {
                for (final String field : values.keySet()) {
                    final byte[] bytes = (field + ": " + values.get(field) + "\n").getBytes();
                    out.write(bytes);

                    if (md5 != null) {
                        md5.update(bytes);
                    }
                    if (sha1 != null) {
                        sha1.update(bytes);
                    }
                    if (sha256 != null) {
                        sha256.update(bytes);
                    }
                    if (sha512 != null) {
                        sha512.update(bytes);
                    }
                }
            }

            addTagChecksum(MD5, f, md5);
            addTagChecksum(SHA1, f, sha1);
            addTagChecksum(SHA256, f, sha256);
            addTagChecksum(SHA512, f, sha512);
        }
    }

    private void addTagChecksum(final BagItDigest algorithm, final File f, final MessageDigest digest) {
        if (digest != null) {
            final Map<File, String> m = tagFileRegistry.computeIfAbsent(algorithm, key -> new HashMap<>());
            m.put(f, HexEncoder.toString(digest.digest()));
        }
    }
}
