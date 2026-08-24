package com.adobe.aem.modernizer.util;

/**
 * Dedicated runtime exception for JSON serialization and deserialization failures.
 */
public class JsonSerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public JsonSerializationException(String message) {
        super(message);
    }

    public JsonSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
