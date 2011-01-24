/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry.encode;

/**
 * Password encoder
 */
public interface PasswordEncrypter {

    String encrypt(String password) throws EncryptionException;

    String decrypt(String password) throws EncryptionException;

}
