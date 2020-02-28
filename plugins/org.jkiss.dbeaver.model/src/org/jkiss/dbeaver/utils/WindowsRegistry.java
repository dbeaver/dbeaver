/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.utils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * Windows registry operations helper class.
 * I was unable to find and give credit to the original author of this code.
 * I've found it here: http://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java
 */
public abstract class WindowsRegistry {
    public static int HKEY_CURRENT_USER = 0x80000001;
    public static int HKEY_LOCAL_MACHINE = 0x80000002;
    public static int REG_SUCCESS = 0;
    public static int REG_NOTFOUND = 2;
    public static int REG_ACCESSDENIED = 5;

    /**
     * Read a value from key and value name
     *
     * @param hkey      HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @param valueName
     * @return the value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public String readString(long hkey, String key, String valueName)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;

    /**
     * Read value(s) and value name(s) form given key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @return the value name(s) plus the value(s)
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public Map<String, String> readStringValues(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;

    /**
     * Read the value name(s) from a given key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @return the value name(s)
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public List<String> readStringSubKeys(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;

    /**
     * Create a key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public void createKey(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;

    /**
     * Write a value in a given key/value name
     *
     * @param hkey
     * @param key
     * @param valueName
     * @param value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public void writeStringValue(long hkey, String key, String valueName, String value)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException;

    /**
     * Delete a given key
     *
     * @param hkey
     * @param key
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public void deleteKey(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;

    /**
     * delete a value from a given key/value name
     *
     * @param hkey
     * @param key
     * @param value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    abstract public void deleteValue(long hkey, String key, String value)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException;


    // utility
    protected byte[] toCstr(String str) {
        byte[] result = new byte[str.length() + 1];

        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        result[str.length()] = 0;
        return result;
    }


    private static WindowsRegistry INSTANCE;

    public static synchronized WindowsRegistry getInstance() {
        if (INSTANCE == null) {
            boolean isJava8 = System.getProperty("java.version").startsWith("1.8");
            if (isJava8) {
                INSTANCE = new WinRegistry8();
            } else {
                INSTANCE = new WinRegistry11();
            }
        }
        return INSTANCE;
    }
}