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

    private HexEncoder() {
    }

    protected static String toString(final byte[] bytes) {
        final StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(hexDigits[(b >> 4) & 0xf]).append(hexDigits[b & 0xf]);
        }
        return sb.toString();
    }
}
