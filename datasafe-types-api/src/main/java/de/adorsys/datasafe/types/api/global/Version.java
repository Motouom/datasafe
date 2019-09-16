package de.adorsys.datasafe.types.api.global;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This class carries logical Datasafe version (i.e. dictates user profile structure).
 */
@Getter
@RequiredArgsConstructor
public enum Version {

    /**
     * The first version before major changes happened.
     */
    BASELINE("v0");

    private final String id;

    public static Version current() {
        return BASELINE;
    }
}
