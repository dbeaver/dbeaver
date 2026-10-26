/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.macos;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFDataRef;
import com.sun.jna.platform.mac.CoreFoundation.CFIndex;
import com.sun.jna.platform.mac.CoreFoundation.CFMutableDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.ptr.PointerByReference;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Arrays;

/** JNA bridge to the macOS Keychain. */
final class MacOSKeychain {
    private static final String SERVICE = "com.dbeaver.SecureStorage";
    private static final String LEGACY_SERVICE = "org.jkiss.dbeaver.secure.storage";
    private static final String ACCOUNT = "master-password";
    private static final int ERR_SEC_SUCCESS = 0;
    private static final int ERR_SEC_ITEM_NOT_FOUND = -25300;
    private static final int ERR_SEC_DUPLICATE_ITEM = -25299;
    private static final int ERR_SEC_MISSING_ENTITLEMENT = -34018;

    private static final CoreFoundation CF = CoreFoundation.INSTANCE;
    private static final NativeLibrary SECURITY_LIBRARY = NativeLibrary.getInstance("Security");
    private static final NativeLibrary CORE_FOUNDATION_LIBRARY = NativeLibrary.getInstance("CoreFoundation");
    private static final Function SEC_ITEM_ADD = SECURITY_LIBRARY.getFunction("SecItemAdd");
    private static final Function SEC_ITEM_COPY_MATCHING = SECURITY_LIBRARY.getFunction("SecItemCopyMatching");
    private static final Function CF_DICTIONARY_SET_VALUE = CORE_FOUNDATION_LIBRARY.getFunction("CFDictionarySetValue");

    private MacOSKeychain() {
    }

    @Nullable
    static byte[] getPassword() {
        byte[] password = getPassword(SERVICE);
        // Existing secure preferences still need the password stored under the old service.
        return password != null ? password : getPassword(LEGACY_SERVICE);
    }

    @Nullable
    private static byte[] getPassword(@NotNull String service) {
        byte[] password = getPassword(service, true);
        return password != null ? password : getPassword(service, false);
    }

    @Nullable
    private static byte[] getPassword(@NotNull String service, boolean dataProtection) {
        CFMutableDictionaryRef query = createQuery(service, dataProtection);
        try {
            set(query, "kSecReturnData", booleanValue("kCFBooleanTrue"));
            PointerByReference result = new PointerByReference();
            int status = SEC_ITEM_COPY_MATCHING.invokeInt(new Object[] { query, result });
            if (status == ERR_SEC_ITEM_NOT_FOUND || (dataProtection && status == ERR_SEC_MISSING_ENTITLEMENT)) {
                return null;
            }
            checkStatus("read", status);
            CFDataRef data = new CFDataRef(result.getValue());
            try {
                return data.getBytePtr().getByteArray(0, data.getLength());
            } finally {
                data.release();
            }
        } finally {
            query.release();
        }
    }

    @NotNull
    static byte[] addPassword(@NotNull byte[] password) {
        int status = addPassword(password, true);
        if (status == ERR_SEC_MISSING_ENTITLEMENT) {
            // Unsigned launchers cannot access the data protection keychain. The login
            // keychain does not require the entitlement and still encrypts the password.
            status = addPassword(password, false);
        }
        if (status == ERR_SEC_DUPLICATE_ITEM) {
            // Another secure-storage operation created the item first. Never overwrite
            // its encryption password, even if the generated passwords differ.
            byte[] existing = getPassword();
            if (existing != null) {
                Arrays.fill(password, (byte) 0);
                return existing;
            }
        }
        checkStatus("create", status);
        return password;
    }

    private static int addPassword(@NotNull byte[] password, boolean dataProtection) {
        CFMutableDictionaryRef attributes = createQuery(SERVICE, dataProtection);
        try {
            CFDataRef value = createData(password);
            try {
                set(attributes, "kSecValueData", value.getPointer());
                if (dataProtection) {
                    set(attributes, "kSecAttrAccessible", securityValue("kSecAttrAccessibleWhenUnlocked"));
                }
                return SEC_ITEM_ADD.invokeInt(new Object[] { attributes, null });
            } finally {
                value.release();
            }
        } finally {
            attributes.release();
        }
    }

    @NotNull
    private static CFMutableDictionaryRef createQuery(@NotNull String service, boolean dataProtection) {
        CFMutableDictionaryRef query = CF.CFDictionaryCreateMutable(
            null,
            new CFIndex(0),
            callbacks("kCFTypeDictionaryKeyCallBacks"),
            callbacks("kCFTypeDictionaryValueCallBacks")
        );
        set(query, "kSecClass", securityValue("kSecClassGenericPassword"));
        if (dataProtection) {
            set(query, "kSecUseDataProtectionKeychain", booleanValue("kCFBooleanTrue"));
        }
        putString(query, "kSecAttrService", service);
        putString(query, "kSecAttrAccount", ACCOUNT);
        return query;
    }

    private static void putString(@NotNull CFMutableDictionaryRef dictionary, @NotNull String key, @NotNull String value) {
        CFStringRef string = CFStringRef.createCFString(value);
        try {
            set(dictionary, key, string.getPointer());
        } finally {
            string.release();
        }
    }

    private static void set(@NotNull CFMutableDictionaryRef dictionary, @NotNull String key, @NotNull Pointer value) {
        CF_DICTIONARY_SET_VALUE.invokeVoid(new Object[] { dictionary, securityValue(key), value });
    }

    @NotNull
    private static CFDataRef createData(@NotNull byte[] password) {
        Memory bytes = new Memory(password.length);
        try {
            bytes.write(0, password, 0, password.length);
            return CF.CFDataCreate(null, bytes, new CFIndex(password.length));
        } finally {
            bytes.clear();
        }
    }

    @NotNull
    private static Pointer securityValue(@NotNull String symbol) {
        // Keychain constants are exported as addresses of CFStringRef variables.
        return SECURITY_LIBRARY.getGlobalVariableAddress(symbol).getPointer(0);
    }

    @NotNull
    private static Pointer booleanValue(@NotNull String symbol) {
        return CORE_FOUNDATION_LIBRARY.getGlobalVariableAddress(symbol).getPointer(0);
    }

    @NotNull
    private static Pointer callbacks(@NotNull String symbol) {
        // The callback globals are structures, not pointers to structures.
        return CORE_FOUNDATION_LIBRARY.getGlobalVariableAddress(symbol);
    }

    private static void checkStatus(@NotNull String operation, int status) {
        if (status != ERR_SEC_SUCCESS) {
            throw new IllegalStateException("Cannot " + operation + " macOS Keychain password (OSStatus " + status + ")");
        }
    }
}
