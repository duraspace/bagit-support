/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testFailure() {
        assertThrows(IllegalArgumentException.class,
            ()->{
                BagItDigest.from("invalid-algorithm");
            });
    }
}