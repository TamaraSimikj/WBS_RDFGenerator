package com.example.rdfgenerator.model;


public class ValidationResult {
    private String message;
    private boolean conforms;

    public ValidationResult(String message,boolean conforms) {
        this.message = message;
        this.conforms = conforms;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean getConforms() {
        return conforms;
    }

    public void setConforms(boolean conforms) {
        this.conforms = conforms;
    }
}