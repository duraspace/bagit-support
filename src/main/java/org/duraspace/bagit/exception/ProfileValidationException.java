/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.exception;

/**
 * A profile validation exception.
 *
 * @author Daniel Bernstein
 * @since Dec 14, 2016
 */
public class ProfileValidationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Default Constructor
     *
     * @param message The error message
     */
    public ProfileValidationException(final String message) {
        super(message);
    }
}
