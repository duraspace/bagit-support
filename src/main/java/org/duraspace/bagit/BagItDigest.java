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
