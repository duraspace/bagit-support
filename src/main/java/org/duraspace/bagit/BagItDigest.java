package org.duraspace.bagit;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum BagItDigest {
    MD5("md5", "MD5"), SHA1("sha1", "SHA-1"), SHA256("sha256", "SHA-256"), SHA512("sha512", "SHA-512");

    private final String bagItName;
    private final String javaName;

    BagItDigest(final String bagItName, final String javaName) {
        this.bagItName = bagItName;
        this.javaName = javaName;
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
