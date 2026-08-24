package com.adobe.aem.modernizer;

/**
 * Dedicated exception for AEM Modernizer operations.
 */
public class ModernizerException extends Exception {

    private static final long serialVersionUID = 1L;

    public ModernizerException(String message) {
        super(message);
    }

    public ModernizerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModernizerException(Throwable cause) {
        super(cause);
    }
}
