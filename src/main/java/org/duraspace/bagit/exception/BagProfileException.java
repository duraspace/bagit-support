/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.exception;

/**
 * Signal that an exception has occurred relating to the BagProfile. This can be indicative of a failure to use the
 * BagProfile properly and trying to perform an invalid operation with the profile (e.g. trying to use a serialization
 * format which is not supported).
 *
 * @author mikejritter
 */
public class BagProfileException extends Exception {

    /**
     * Construct a {@link BagProfileException} with a specified message
     * @param message the message detailing the error that occurred
     */
    public BagProfileException(final String message) {
        super(message);
    }

}
