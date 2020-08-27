/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A digest algorithm for use in a BagIt Bag in order provide validation of payload and tag files.
 */
public enum BagItDigest {
    MD5("md5", "MD5"), SHA1("sha1", "SHA-1"), SHA256("sha256", "SHA-256"), SHA512("sha512", "SHA-512");

    private final String bagItName;
    private final String javaName;

    BagItDigest(final String bagItName, final String javaName) {
        this.bagItName = bagItName;
        this.javaName = javaName;
    }

    /**
     * Retrieve a {@link BagItDigest} for a given algorithm name
     *
     * @param name the name of the algorithm, in either BagIt or Java format
     * @throws IllegalArgumentException if the algorithm is not supported
     * @return the {@link BagItDigest}
     */
    public static BagItDigest from(final String name) {
        switch (name.toLowerCase()) {
            case "md5":
                return MD5;
            case "sha1":
            case "sha-1":
                return SHA1;
            case "sha256":
            case "sha-256":
                return SHA256;
            case "sha512":
            case "sha-512":
                return SHA512;
            default:
                throw new IllegalArgumentException("Unsupported digest algorithm: " + name);
        }
    }

    /**
     * Retrieve the bagit formatted version of the algorithm
     *
     * @return the algorithm name
     */
    public String bagitName() {
        return bagItName;
    }

    /**
     * Retrieve a {@link MessageDigest} for the given algorithm
     *
     * @return the MessageDigest
     */
    public MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance(javaName);
        } catch (NoSuchAlgorithmException e) {
            // this should never happen with known digest types
            throw new AssertionError(e.getMessage(), e);
        }
    }
}
