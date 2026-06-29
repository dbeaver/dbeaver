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
package org.jkiss.dbeaver.ui.forms.util;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.forms.UIObservable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * A common superclass for message bundle classes that can <b>dynamically be reloaded at runtime</b> in case of
 * a locale change. If you don't need dynamic reloading, consider using {@link org.eclipse.osgi.util.NLS} instead,
 * which is more lightweight and doesn't require any special setup.
 * <h3>Usage</h3>
 * Clients are expected to subclass this type and define message keys
 * of their message bundle as {@code public static String} keys, with
 * field name matching the key in the message bundle. Then, in a static initializer block,
 * they should call {@link UIReloadableNLS#initializeMessages(String, Class)}. It will load messages for the
 * {@link Locale#getDefault() default locale} and initialize all message keys defined as static fields in the subclass.
 * <p>
 * Optionally, to support seamless integration with {@link org.jkiss.dbeaver.ui.forms UI Forms},
 * it's also possible to define message keys as {@code public static UIObservable<String>}. In such
 * a case, UI elements that bind to these keys will automatically update their text or other properties
 * when the locale changes and messages are reloaded.
 * <p>
 * To change the locale at runtime, call {@link UIReloadableNLS#reloadMessages()} method. It will
 * automatically reload messages for all subclasses of {@link UIReloadableNLS} and update all
 * their {@link org.jkiss.dbeaver.ui.forms.UIObservable UIObservable} keys.
 *
 * <h3>Example</h3>
 * <p><p><b>MyMessages.properties</b>
 * {@snippet :
 * hello = Hello
 *}
 * <p><p><b>MyMessages_de.properties</b>
 * {@snippet :
 * hello = Hallo
 *}
 * <p><p><b>MyMessages.java</b>
 * {@snippet :
 * import org.jkiss.dbeaver.ui.forms.util.UIReloadableNLS;
 * import org.jkiss.dbeaver.ui.forms.UIObservable;
 *
 * class MyMessages extends UIReloadableNLS {
 *     public static UIObservable<String> hello;
 *
 *     static {
 *         UIReloadableNLS.initializeMessages("org.jkiss.dbeaver.samplebundle.internal.MyMessages", MyMessages.class);
 *     }
 * }
 *}
 *
 * @see org.eclipse.osgi.util.NLS
 */
public abstract class UIReloadableNLS {
    private static final Log log = Log.getLog(UIReloadableNLS.class);

    private static final String EXTENSION = ".properties"; //$NON-NLS-1$
    private static final Map<Class<?>, String> bundles = new WeakHashMap<>();

    protected static void initializeMessages(@NotNull String name, @NotNull Class<?> cls) {
        synchronized (UIReloadableNLS.bundles) {
            if (bundles.containsKey(cls)) {
                return;
            }
            load(name, cls);
            bundles.put(cls, name);
        }
    }

    public static void reloadMessages() {
        synchronized (UIReloadableNLS.class) {
            bundles.forEach((cls, name) -> load(name, cls));
        }
    }

    private static void load(@NotNull String bundleName, @NotNull Class<?> cls) {
        var fieldsArray = cls.getDeclaredFields();
        var fields = new HashMap<>(fieldsArray.length * 2);
        for (Field field : fieldsArray) {
            fields.put(field.getName(), field);
        }

        var isAccessible = (cls.getModifiers() & Modifier.PUBLIC) != 0;
        var loader = cls.getClassLoader();
        var variants = buildVariants(bundleName);
        for (String variant : variants) {
            try (var input = loader == null ? ClassLoader.getSystemResourceAsStream(variant) : loader.getResourceAsStream(variant)) {
                if (input == null) {
                    continue;
                }
                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                var properties = new MessagesProperties(fields, bundleName, isAccessible);
                var bundle = new PropertyResourceBundle(input);
                for (String key : bundle.keySet()) {
                    properties.put(key, bundle.getString(key));
                }
            } catch (IOException e) {
                log.error("Error loading " + variant, e);
            }
        }
    }

    /*
     * Build an array of property files to search.  The returned array contains
     * the property fields in order from most specific to most generic.
     * So, in the FR_fr locale, it will return file_fr_FR.properties, then
     * file_fr.properties, and finally file.properties.
     */
    @NotNull
    private static List<String> buildVariants(@NotNull String root) {
        var suffixes = buildSuffixes();
        var rootFinal = root.replace('.', '/');
        return suffixes.stream()
            .map(suffix -> rootFinal + suffix)
            .toList();
    }

    @NotNull
    private static ArrayList<String> buildSuffixes() {
        // Build list of suffixes for loading resource bundles
        var nl = Locale.getDefault().toString();
        var suffixes = new ArrayList<String>(4);
        while (true) {
            suffixes.add('_' + nl + EXTENSION);
            int separator = nl.lastIndexOf('_');
            if (separator < 0) {
                break;
            }
            nl = nl.substring(0, separator);
        }
        // Add the empty suffix last (most general)
        suffixes.add(EXTENSION);
        return suffixes;
    }

    /**
     * Class which subclasses {@link Properties} and uses the {@link #put(Object, Object)} method
     * to set field values rather than storing the values in the table.
     */
    private static final class MessagesProperties extends Properties {
        /**
         * This object is assigned to the value of a field map to indicate
         * that a translated message has already been assigned to that field.
         */
        private static final Object ASSIGNED = new Object();

        private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
        private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;

        private final Map<Object, Object> fields;
        private final String bundleName;
        private final boolean isAccessible;

        public MessagesProperties(@NotNull Map<Object, Object> fields, @NotNull String bundleName, boolean isAccessible) {
            this.fields = fields;
            this.bundleName = bundleName;
            this.isAccessible = isAccessible;
        }

        @Nullable
        @Override
        public synchronized Object put(@NotNull Object key, @NotNull Object value) {
            var object = fields.put(key, ASSIGNED);
            if (object == ASSIGNED) {
                // If already assigned, there is nothing to do
                return null;
            }
            if (object == null) {
                log.warn("NLS unused message: " + key + " in: " + bundleName);
                return null;
            }
            var field = (Field) object;
            if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED) {
                return null;
            }
            try {
                if (!isAccessible) {
                    field.setAccessible(true);
                }
                var string = new String(((String) value).toCharArray());
                if (field.getType() == String.class) {
                    field.set(null, string);
                } else if (field.getType() == UIObservable.class) {
                    @SuppressWarnings("unchecked")
                    var observable = (UIObservable<String>) field.get(null);
                    if (observable != null) {
                        observable.set(string);
                    } else {
                        field.set(null, UIObservable.of(string));
                    }
                } else {
                    log.warn("NLS field has unsupported type: " + field.getType().getName() + " for key: " + key + " in: " + bundleName);
                    return null;
                }
            } catch (Exception e) {
                log.error("Error setting value to field " + field.getName() + " in: " + bundleName, e);
            }
            return null;
        }
    }
}
