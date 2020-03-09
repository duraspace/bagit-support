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

/**
 * Utility to write BagIt bags.
 *
 * @author escowles
 * @since 2016-12-15
 */
public class BagWriter {

    private File bagDir;
    private File dataDir;
    private Set<String> algorithms;

    private Map<String, Map<File, String>> payloadRegistry;
    private Map<String, Map<File, String>> tagFileRegistry;
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
    public BagWriter(final File bagDir, final Set<String> algorithms) {
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
    public void registerChecksums(final String algorithm, final Map<File, String> filemap) {
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

    private void writeManifests(final String prefix, final Map<String, Map<File, String>> registry)
            throws IOException {
        final String delimiter = "  ";
        final String bagitSeparator = "/";
        final String osSeparator = File.separator;
        final Path bag = bagDir.toPath();

        for (final String algorithm : algorithms) {
            final Map<File, String> filemap = registry.get(algorithm);
            if (filemap != null) {
                final File f = new File(bagDir, prefix + "-" + algorithm + ".txt");
                try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)))) {
                    for (final File payload : filemap.keySet()) {
                        final String relative = bag.relativize(payload.toPath()).toString()
                                                   .replaceAll(osSeparator, bagitSeparator);
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
            if (algorithms.contains(MD5.bagitName())) {
                md5 = MD5.messageDigest();
            }
            if (algorithms.contains(SHA1.bagitName())) {
                sha1 = SHA1.messageDigest();
            }
            if (algorithms.contains(SHA256.bagitName())) {
                sha256 = SHA256.messageDigest();
            }
            if (algorithms.contains(SHA512.bagitName())) {
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

            addTagChecksum(MD5.bagitName(), f, md5);
            addTagChecksum(SHA1.bagitName(), f, sha1);
            addTagChecksum(SHA256.bagitName(), f, sha256);
            addTagChecksum(SHA512.bagitName(), f, sha512);
        }
    }

    private void addTagChecksum(final String algorithm, final File f, final MessageDigest digest) {
        if (digest != null) {
            final Map<File, String> m = tagFileRegistry.computeIfAbsent(algorithm, key -> new HashMap<>());
            m.put(f, HexEncoder.toString(digest.digest()));
        }
    }
}
