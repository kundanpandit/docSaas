package com.docsaas.api.processing;

/** A processing failure whose message is safe to show to the user. */
public class ProcessingException extends RuntimeException {

    private final String code;

    public ProcessingException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
