package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Simple tests for the helper methods on BagItDigest
 */
public class BagItDigestTest {

    @Test
    public void testValid() {
        for (final BagItDigest digest : BagItDigest.values()) {
            final String bagitIdentifier = digest.bagitName();
            final String javaIdentifier = digest.messageDigest().getAlgorithm();

            assertThat(BagItDigest.from(bagitIdentifier)).isEqualTo(digest);
            assertThat(BagItDigest.from(javaIdentifier)).isEqualTo(digest);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailure() {
        BagItDigest.from("invalid-algorithm");
    }

}