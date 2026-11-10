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
package org.jkiss.dbeaver.model.security.linux;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Arrays;

/** JNA bridge to libsecret. Native libraries are loaded only when the Linux provider is used. */
final class LinuxKeyring {
    private static final String COLLECTION = "default";
    private static final SecretSchema SCHEMA = new SecretSchema("com.dbeaver.secure-storage");
    private static final SecretSchema LEGACY_SCHEMA = new SecretSchema("org.eclipse.equinox");

    private LinuxKeyring() {
    }

    static boolean isAvailable() {
        try {
            return withCollection(false, collection -> {
                // An unlocked keyring works without a display. A locked one may require
                // an interactive Secret Service prompt, so use the fallback in headless sessions.
                boolean locked = Native.COLLECTION_LOCKED.invokeInt(new Object[] { collection }) != 0;
                return !locked || System.getenv("DISPLAY") != null || System.getenv("WAYLAND_DISPLAY") != null;
            });
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    @Nullable
    static byte[] getPassword() {
        return withCollection(true, collection -> {
            byte[] password = lookup(SCHEMA);
            if (password != null) {
                return password;
            }
            // Copy the existing Equinox password without changing the old item: both
            // providers must still decrypt values written before this upgrade.
            byte[] legacy = lookup(LEGACY_SCHEMA);
            if (legacy != null) {
                try {
                    store(legacy);
                } catch (RuntimeException e) {
                    Arrays.fill(legacy, (byte) 0);
                    throw e;
                }
            }
            return legacy;
        });
    }

    @NotNull
    static byte[] addPassword(@NotNull byte[] password) {
        return withCollection(true, collection -> {
            byte[] existing = lookup(SCHEMA);
            if (existing != null) {
                Arrays.fill(password, (byte) 0);
                return existing;
            }
            // Do not replace the old Equinox item: it may still be needed to read
            // previously encrypted secure-preferences entries.
            byte[] value = password;
            byte[] legacy = lookup(LEGACY_SCHEMA);
            if (legacy != null) {
                Arrays.fill(password, (byte) 0);
                value = legacy;
            }
            try {
                store(value);
            } catch (RuntimeException e) {
                if (legacy != null) {
                    Arrays.fill(legacy, (byte) 0);
                }
                throw e;
            }
            return value;
        });
    }

    @Nullable
    private static byte[] lookup(@NotNull SecretSchema schema) {
        PointerByReference error = new PointerByReference();
        Pointer result = Native.SECRET_LOOKUP.invokePointer(new Object[] { schema, null, error, null });
        check(error, "read");
        if (result == null) {
            return null;
        }
        try {
            long length = result.indexOf(0, (byte) 0);
            if (length < 0 || length > 4096) {
                throw new IllegalStateException("Invalid Linux keyring password length");
            }
            return result.getByteArray(0, (int) length);
        } finally {
            Native.SECRET_PASSWORD_FREE.invokeVoid(new Object[] { result });
        }
    }

    private static void store(@NotNull byte[] password) {
        // libsecret expects a UTF-8 NUL-terminated password, not a Java String.
        Memory value = new Memory(password.length + 1L);
        try {
            value.write(0, password, 0, password.length);
            value.setByte(password.length, (byte) 0);
            PointerByReference error = new PointerByReference();
            boolean stored = Native.SECRET_STORE.invokeInt(new Object[] {
                SCHEMA, COLLECTION, "DBeaver secure storage password", value, null, error, null
            }) != 0;
            check(error, "store");
            if (!stored) {
                throw new IllegalStateException("Linux keyring refused to store the password");
            }
        } finally {
            value.clear();
        }
    }

    private static <T> T withCollection(boolean unlock, @NotNull CollectionAction<T> action) {
        PointerByReference error = new PointerByReference();
        Pointer service = Native.SERVICE_GET.invokePointer(new Object[] { 4, null, error });
        check(error, "open Secret Service");
        if (service == null) {
            throw new IllegalStateException("Linux Secret Service is unavailable");
        }
        try {
            Pointer collection = Native.COLLECTION_FOR_ALIAS.invokePointer(new Object[] {
                service, COLLECTION, 0, null, error
            });
            check(error, "open default collection");
            if (collection == null) {
                throw new IllegalStateException("Linux keyring has no default collection");
            }
            try {
                if (unlock && Native.COLLECTION_LOCKED.invokeInt(new Object[] { collection }) != 0) {
                    unlock(service, collection);
                }
                return action.run(collection);
            } finally {
                Native.OBJECT_UNREF.invokeVoid(new Object[] { collection });
            }
        } finally {
            Native.OBJECT_UNREF.invokeVoid(new Object[] { service });
        }
    }

    private static void unlock(@NotNull Pointer service, @NotNull Pointer collection) {
        Pointer list = Native.LIST_APPEND.invokePointer(new Object[] { null, collection });
        PointerByReference unlocked = new PointerByReference();
        PointerByReference error = new PointerByReference();
        try {
            Native.SERVICE_UNLOCK.invokeInt(new Object[] { service, list, null, unlocked, error });
            check(error, "unlock collection");
            if (unlocked.getValue() == null) {
                throw new IllegalStateException("Linux keyring collection is still locked");
            }
        } finally {
            // The returned GList owns references to its unlocked GObjects.
            Pointer node = unlocked.getValue();
            while (node != null) {
                Native.OBJECT_UNREF.invokeVoid(new Object[] { node.getPointer(0) });
                node = node.getPointer(com.sun.jna.Native.POINTER_SIZE);
            }
            if (unlocked.getValue() != null) {
                Native.LIST_FREE.invokeVoid(new Object[] { unlocked.getValue() });
            }
            if (list != null) {
                Native.LIST_FREE.invokeVoid(new Object[] { list });
            }
        }
    }

    private static void check(@NotNull PointerByReference error, @NotNull String operation) {
        Pointer failure = error.getValue();
        if (failure != null) {
            try {
                // GError: GQuark domain, int code, char *message.
                throw new IllegalStateException("Cannot " + operation + " Linux keyring: " + failure.getPointer(8).getString(0));
            } finally {
                Native.ERROR_FREE.invokeVoid(new Object[] { failure });
            }
        }
    }

    @NotNull
    private static NativeLibrary load(@NotNull String name, @NotNull String runtimeSoname) {
        try {
            return NativeLibrary.getInstance(name);
        } catch (UnsatisfiedLinkError e) {
            // Minimal Linux installations often have the runtime .so.0 but not
            // the unversioned linker name supplied by development packages.
            return NativeLibrary.getInstance(runtimeSoname);
        }
    }

    private interface CollectionAction<T> {
        T run(@NotNull Pointer collection);
    }

    @Structure.FieldOrder({"name", "flags", "attributes", "reserved", "reserved1", "reserved2", "reserved3",
        "reserved4", "reserved5", "reserved6", "reserved7"})
    public static class SecretSchema extends Structure {
        public String name;
        public int flags;
        public Attribute[] attributes = (Attribute[]) new Attribute().toArray(32);
        public int reserved;
        public Pointer reserved1;
        public Pointer reserved2;
        public Pointer reserved3;
        public Pointer reserved4;
        public Pointer reserved5;
        public Pointer reserved6;
        public Pointer reserved7;

        public SecretSchema(String name) {
            this.name = name;
            write();
        }
    }

    @Structure.FieldOrder({"name", "type"})
    public static class Attribute extends Structure {
        public String name;
        public int type;
    }

    private static final class Native {
        private static final NativeLibrary SECRET = load("secret-1", "libsecret-1.so.0");
        private static final NativeLibrary GLIB = load("glib-2.0", "libglib-2.0.so.0");
        private static final NativeLibrary GOBJECT = load("gobject-2.0", "libgobject-2.0.so.0");
        private static final Function SERVICE_GET = SECRET.getFunction("secret_service_get_sync");
        private static final Function COLLECTION_FOR_ALIAS = SECRET.getFunction("secret_collection_for_alias_sync");
        private static final Function COLLECTION_LOCKED = SECRET.getFunction("secret_collection_get_locked");
        private static final Function SERVICE_UNLOCK = SECRET.getFunction("secret_service_unlock_sync");
        private static final Function SECRET_LOOKUP = SECRET.getFunction("secret_password_lookup_sync");
        private static final Function SECRET_STORE = SECRET.getFunction("secret_password_store_sync");
        private static final Function SECRET_PASSWORD_FREE = SECRET.getFunction("secret_password_free");
        private static final Function LIST_APPEND = GLIB.getFunction("g_list_append");
        private static final Function LIST_FREE = GLIB.getFunction("g_list_free");
        private static final Function ERROR_FREE = GLIB.getFunction("g_error_free");
        private static final Function OBJECT_UNREF = GOBJECT.getFunction("g_object_unref");
    }
}
