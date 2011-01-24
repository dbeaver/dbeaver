/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.encode;

/**
 * EncryptionException
 */
public class EncryptionException extends Exception {
    public EncryptionException(Throwable t)
    {
        super(t);
    }

    public EncryptionException(String message)
    {
        super(message);
    }
}
