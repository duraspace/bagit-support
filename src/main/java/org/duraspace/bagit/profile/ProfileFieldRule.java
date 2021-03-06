/*
 * The contents of this file are subject to the license and copyright detailed
 * in the LICENSE and NOTICE files at the root of the source tree.
 */
package org.duraspace.bagit.profile;

import java.util.Set;

/**
 * Rules which can be applied to the Bag-Info and Other-Info sections of a Bag Profile. Currently supports the
 * parameters specified in version 1.3.0 of the bagit-profiles specification, in addition to the recommended parameter
 * which is brought in from the Beyond the Repository bagit specification.
 *
 * @author mikejritter
 * @since 2020-01-20
 */
public class ProfileFieldRule {

    private final boolean required;
    private final boolean repeatable;
    private final boolean recommended;
    private final String description;
    private final Set<String> values;

    /**
     * Constructor for a ProfileFieldRule. Takes the 4 possible json fields from a BagIt Profile *-Info field.
     *
     * @param required boolean value stating if this rule is required
     * @param repeatable boolean value allowing a field to be repeated
     * @param recommended boolean value stating if this rule is recommended
     * @param description a text description of this rule
     * @param values a set of string values which a field is allowed to be set to
     */
    public ProfileFieldRule(final boolean required,
                            final boolean repeatable,
                            final boolean recommended,
                            final String description,
                            final Set<String> values) {
        this.required = required;
        this.repeatable = repeatable;
        this.recommended = recommended;
        this.description = description;
        this.values = values;
    }

    /**
     *
     * @return if the field for this rule is required to exist
     */
    public boolean isRequired() {
        return required;
    }

    /**
     *
     * @return if the field is allowed to be repeated
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     *
     * @return if the field for this rule is recommended to exist
     */
    public boolean isRecommended() {
        return recommended;
    }

    /**
     *
     * @return the description of this rule
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @return the allowed values for fields matching this rule
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * String representation of a ProfileFieldRule
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "ProfileFieldRule{" +
               "\nrequired=" + required +
               ",\nrecommended=" + recommended +
               ",\ndescription='" + description + '\'' +
               ",\nvalues=" + values +
               "\n}";
    }
}
