package com.example.main.exception;

public class SessionNotFoundException extends Exception{
    public SessionNotFoundException(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
