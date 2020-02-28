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

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.StandardConstants;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Windows registry operations helper class.
 * I was unable to find and give credit to the original author of this code.
 * I've found it here: http://stackoverflow.com/questions/62289/read-write-to-windows-registry-using-java
 */
class WinRegistry11 extends WindowsRegistry {

    private static final Log log = Log.getLog(WinRegistry11.class);

    private static final int KEY_ALL_ACCESS = 0xf003f;
    private static final int KEY_READ = 0x20019;
    private static Preferences userRoot = Preferences.userRoot();
    private static Preferences systemRoot = Preferences.systemRoot();
    private static Class<? extends Preferences> userClass = userRoot.getClass();
    private static Method regOpenKey = null;
    private static Method regCloseKey = null;
    private static Method regQueryValueEx = null;
    private static Method regEnumValue = null;
    private static Method regQueryInfoKey = null;
    private static Method regEnumKeyEx = null;
    private static Method regCreateKeyEx = null;
    private static Method regSetValueEx = null;
    private static Method regDeleteKey = null;
    private static Method regDeleteValue = null;

    static {
        try {
            regOpenKey = userClass.getDeclaredMethod("WindowsRegOpenKey", long.class, byte[].class, int.class);
            regOpenKey.setAccessible(true);
            regCloseKey = userClass.getDeclaredMethod("WindowsRegCloseKey", long.class);
            regCloseKey.setAccessible(true);
            regQueryValueEx = userClass.getDeclaredMethod("WindowsRegQueryValueEx", long.class, byte[].class);
            regQueryValueEx.setAccessible(true);
            regEnumValue = userClass.getDeclaredMethod("WindowsRegEnumValue", long.class, int.class, int.class);
            regEnumValue.setAccessible(true);
            regQueryInfoKey = userClass.getDeclaredMethod("WindowsRegQueryInfoKey1", long.class);
            regQueryInfoKey.setAccessible(true);
            regEnumKeyEx = userClass.getDeclaredMethod("WindowsRegEnumKeyEx", long.class, int.class, int.class);
            regEnumKeyEx.setAccessible(true);
            regCreateKeyEx = userClass.getDeclaredMethod("WindowsRegCreateKeyEx", long.class, byte[].class);
            regCreateKeyEx.setAccessible(true);
            regSetValueEx = userClass.getDeclaredMethod("WindowsRegSetValueEx", long.class, byte[].class, byte[].class);
            regSetValueEx.setAccessible(true);
            regDeleteValue = userClass.getDeclaredMethod("WindowsRegDeleteValue", long.class, byte[].class);
            regDeleteValue.setAccessible(true);
            regDeleteKey = userClass.getDeclaredMethod("WindowsRegDeleteKey", long.class, byte[].class);
            regDeleteKey.setAccessible(true);
        } catch (Exception e) {
            log.error("Error initializing Windows registry", e);
        }
    }

    WinRegistry11() {
    }

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
    public String readString(long hkey, String key, String valueName)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readString(systemRoot, hkey, key, valueName);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readString(userRoot, hkey, key, valueName);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

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
    public Map<String, String> readStringValues(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringValues(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringValues(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

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
    public List<String> readStringSubKeys(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        if (hkey == HKEY_LOCAL_MACHINE) {
            return readStringSubKeys(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            return readStringSubKeys(userRoot, hkey, key);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Create a key
     *
     * @param hkey HKEY_CURRENT_USER/HKEY_LOCAL_MACHINE
     * @param key
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void createKey(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        long[] ret;
        if (hkey == HKEY_LOCAL_MACHINE) {
            ret = createKey(systemRoot, hkey, key);
            regCloseKey.invoke(systemRoot, ret[0]);
        } else if (hkey == HKEY_CURRENT_USER) {
            ret = createKey(userRoot, hkey, key);
            regCloseKey.invoke(userRoot, ret[0]);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
        if (ret[1] != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + ret[1] + "  key=" + key);
        }
    }

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
    public void writeStringValue(long hkey, String key, String valueName, String value)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        if (hkey == HKEY_LOCAL_MACHINE) {
            writeStringValue(systemRoot, hkey, key, valueName, value);
        } else if (hkey == HKEY_CURRENT_USER) {
            writeStringValue(userRoot, hkey, key, valueName, value);
        } else {
            throw new IllegalArgumentException("hkey=" + hkey);
        }
    }

    /**
     * Delete a given key
     *
     * @param hkey
     * @param key
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void deleteKey(long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        int rc = -1;
        if (hkey == HKEY_LOCAL_MACHINE) {
            rc = deleteKey(systemRoot, hkey, key);
        } else if (hkey == HKEY_CURRENT_USER) {
            rc = deleteKey(userRoot, hkey, key);
        }
        if (rc != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + rc + "  key=" + key);
        }
    }

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
    public void deleteValue(long hkey, String key, String value)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        long rc = -1;
        if (hkey == HKEY_LOCAL_MACHINE) {
            rc = deleteValue(systemRoot, hkey, key, value);
        } else if (hkey == HKEY_CURRENT_USER) {
            rc = deleteValue(userRoot, hkey, key, value);
        }
        if (rc != REG_SUCCESS) {
            throw new IllegalArgumentException("rc=" + rc + "  key=" + key + "  value=" + value);
        }
    }

    // =====================

    private long deleteValue(Preferences root, long hkey, String key, String value)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        long[] handles = (long[]) regOpenKey.invoke(null, hkey, toCstr(key), KEY_ALL_ACCESS);
        if (handles[1] != REG_SUCCESS) {
            return handles[1];  // can be REG_NOTFOUND, REG_ACCESSDENIED
        }
        int rc = (Integer) regDeleteValue.invoke(null,
            handles[0], toCstr(value)
        );
        regCloseKey.invoke(null, handles[0]);
        return rc;
    }

    private int deleteKey(Preferences root, long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        return (Integer) regDeleteKey.invoke(null,
            hkey, toCstr(key));  // can REG_NOTFOUND, REG_ACCESSDENIED, REG_SUCCESS
    }

    private String readString(Preferences root, long hkey, String key, String value)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        long[] handles = (long[]) regOpenKey.invoke(null, hkey, toCstr(key), KEY_READ);
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        byte[] valb = (byte[]) regQueryValueEx.invoke(null, handles[0], toCstr(value));
        regCloseKey.invoke(null, handles[0]);
        return (valb != null ? new String(valb).trim() : null);
    }

    private Map<String, String> readStringValues(Preferences root, long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        HashMap<String, String> results = new HashMap<>();
        long[] handles = (long[]) regOpenKey.invoke(null, hkey, toCstr(key), KEY_READ);
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        long[] info = (long[]) regQueryInfoKey.invoke(null, handles[0]);

        long count = info[2]; // count
        int maxlen = 256; // value length max
        // 256 is hardcoded value
        // initially info[3] was here
        for (int index = 0; index < count; index++) {
            byte[] name = (byte[]) regEnumValue.invoke(null, handles[0], index, maxlen + 1);
            if (name != null) {
                String value = readString(hkey, key, new String(name));
                results.put(new String(name).trim(), value);
            }
        }
        regCloseKey.invoke(null, handles[0]);
        return results;
    }

    private List<String> readStringSubKeys(Preferences root, long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException {
        List<String> results = new ArrayList<>();
        long[] handles = (long[]) regOpenKey.invoke(null, hkey, toCstr(key), KEY_READ);
        if (handles[1] != REG_SUCCESS) {
            return null;
        }
        long[] info = (long[]) regQueryInfoKey.invoke(null, handles[0]);

        long count = info[2]; // count
        int maxlen = (int) info[3]; // value length max
        for (int index = 0; index < Integer.MAX_VALUE; index++) {
            byte[] name = (byte[]) regEnumKeyEx.invoke(null, handles[0], index, maxlen + 1);
            if (name == null) {
                break;
            }
            results.add(new String(name).trim());
        }
        regCloseKey.invoke(null, handles[0]);
        return results;
    }

    private long[] createKey(Preferences root, long hkey, String key)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        return (long[]) regCreateKeyEx.invoke(null, hkey, toCstr(key));
    }

    private void writeStringValue(Preferences root, long hkey, String key, String valueName, String value)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        long[] handles = (long[]) regOpenKey.invoke(null, hkey, toCstr(key), KEY_ALL_ACCESS);

        regSetValueEx.invoke(null, handles[0], toCstr(valueName), toCstr(value));
        regCloseKey.invoke(null, handles[0]);
    }

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException {
        System.out.println(System.getProperty(StandardConstants.ENV_OS_NAME));
//        System.out.println(Platform.getOS());
        WinRegistry11 winRegistry11 = new WinRegistry11();
        System.out.println(winRegistry11.readString(HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE", "VOBHOME2.0"));
        System.out.println(winRegistry11.readStringSubKeys(HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE"));
        System.out.println(winRegistry11.readStringValues(HKEY_LOCAL_MACHINE, "SOFTWARE\\ORACLE\\KEY_OraClient10g_home1"));
    }
}