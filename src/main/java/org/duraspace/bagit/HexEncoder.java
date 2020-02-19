package org.duraspace.bagit;

/**
 * Simple encoder to convert a byte array to bytes.
 *
 * From:
 * https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
 * https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/HashCode.java
 *
 * If we pull in a dependency which does hex encoding, this can be removed
 *
 * @author mikejritter
 * @since 2020-02-18
 */
public class HexEncoder {

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    protected static String toString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }
}
