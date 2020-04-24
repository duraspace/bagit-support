/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit;

public abstract class BagProfileConstants {
    public static final String ALLOW_FETCH_TXT = "Allow-Fetch.txt";
    public static final String SERIALIZATION = "Serialization";
    public static final String ACCEPT_BAGIT_VERSION = "Accept-BagIt-Version";
    public static final String ACCEPT_SERIALIZATION = "Accept-Serialization";
    public static final String TAG_FILES_ALLOWED = "Tag-Files-Allowed";
    public static final String TAG_FILES_REQUIRED = "Tag-Files-Required";
    public static final String MANIFESTS_ALLOWED = "Manifests-Allowed";
    public static final String TAG_MANIFESTS_ALLOWED = "Tag-Manifests-Allowed";
    public static final String MANIFESTS_REQUIRED = "Manifests-Required";
    public static final String TAG_MANIFESTS_REQUIRED = "Tag-Manifests-Required";
    public static final String BAGIT_PROFILE_INFO = "BagIt-Profile-Info";
    public static final String BAG_INFO = "Bag-Info";
    public static final String OTHER_INFO = "Other-Info";

    // fields within Bag-Profile-Info
    public static final String PROFILE_VERSION = "Version";
    public static final String BAGIT_PROFILE_VERSION = "BagIt-Profile-Version";
    public static final String BAGIT_PROFILE_IDENTIFIER = "BagIt-Profile-Identifier";

    // misc
    public static final String BAGIT_TAG_SUFFIX = ".txt";
    public static final String BAGIT_MD5 = "md5";
    public static final String BAGIT_SHA1 = "sha1";
    public static final String BAGIT_SHA_256 = "sha256";
}
