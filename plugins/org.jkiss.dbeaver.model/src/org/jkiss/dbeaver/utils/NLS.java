/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * This is a direct copy of {@link org.eclipse.osgi.util.NLS} that
 * includes <a href="https://github.com/eclipse-equinox/equinox/pull/709">the following fix</a>.
 * <p>
 * Must be removed and replaced with the original class once migrated to Eclipse 2025-03.
 */
public abstract class NLS {

    private static final Log log = Log.getLog(NLS.class);

    private static final Object[] EMPTY_ARGS = new Object[0];
    private static final String EXTENSION = ".properties"; //$NON-NLS-1$
    private static String[] nlSuffixes;

    /*
     * This object is assigned to the value of a field map to indicate
     * that a translated message has already been assigned to that field.
     */
    static final Object ASSIGNED = new Object();

    /**
     * Creates a new NLS instance.
     */
    protected NLS() {
        super();
    }

    /**
     * Bind the given message's substitution locations with the given string value.
     *
     * @param message the message to be manipulated
     * @param binding the object to be inserted into the message
     * @return the manipulated String
     * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
     */
    public static String bind(String message, Object binding) {
        return internalBind(message, null, String.valueOf(binding), null);
    }

    /**
     * Bind the given message's substitution locations with the given string values.
     *
     * @param message  the message to be manipulated
     * @param binding1 An object to be inserted into the message
     * @param binding2 A second object to be inserted into the message
     * @return the manipulated String
     * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
     */
    public static String bind(String message, Object binding1, Object binding2) {
        return internalBind(message, null, String.valueOf(binding1), String.valueOf(binding2));
    }

    /**
     * Bind the given message's substitution locations with the given string values.
     *
     * @param message  the message to be manipulated
     * @param bindings An array of objects to be inserted into the message
     * @return the manipulated String
     * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
     */
    public static String bind(String message, Object[] bindings) {
        return internalBind(message, bindings, null, null);
    }

    /**
     * Initialize the given class with the values from the message properties specified by the
     * base name.  The base name specifies a fully qualified base name to a message properties file,
     * including the package where the message properties file is located.  The class loader of the
     * specified class will be used to load the message properties resources.
     * <p>
     * For example, if the locale is set to en_US and <code>org.eclipse.example.nls.messages</code>
     * is used as the base name then the following resources will be searched using the class
     * loader of the specified class:
     * </p>
     * <pre>
     *   org/eclipse/example/nls/messages_en_US.properties
     *   org/eclipse/example/nls/messages_en.properties
     *   org/eclipse/example/nls/messages.properties
     * </pre>
     *
     * @param baseName the base name of a fully qualified message properties file.
     * @param clazz    the class where the constants will exist
     */
    public static void initializeMessages(final String baseName, final Class<?> clazz) {
        load(baseName, clazz);
    }

    /*
     * Perform the string substitution on the given message with the specified args.
     * See the class comment for exact details.
     */
    private static String internalBind(String message, Object[] args, String argZero, String argOne) {
        if (message == null)
            return "No message available."; //$NON-NLS-1$
        if (args == null || args.length == 0)
            args = EMPTY_ARGS;

        int length = message.length();
        // estimate correct size of string buffer to avoid growth
        int bufLen = length + (args.length * 5);
        if (argZero != null)
            bufLen += argZero.length() - 3;
        if (argOne != null)
            bufLen += argOne.length() - 3;
        StringBuilder buffer = new StringBuilder(Math.max(bufLen, 0));
        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);
            switch (c) {
                case '{':
                    int index = message.indexOf('}', i);
                    // if we don't have a matching closing brace then...
                    if (index == -1) {
                        buffer.append(c);
                        break;
                    }
                    i++;
                    if (i >= length) {
                        buffer.append(c);
                        break;
                    }
                    // look for a substitution
                    int number = -1;
                    try {
                        number = Integer.parseInt(message.substring(i, index));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(e);
                    }
                    if (number == 0 && argZero != null)
                        buffer.append(argZero);
                    else if (number == 1 && argOne != null)
                        buffer.append(argOne);
                    else {
                        if (number >= args.length || number < 0) {
                            buffer.append("<missing argument>"); //$NON-NLS-1$
                            i = index;
                            break;
                        }
                        buffer.append(args[number]);
                    }
                    i = index;
                    break;
                case '\'':
                    // if a single quote is the last char on the line then skip it
                    int nextIndex = i + 1;
                    if (nextIndex >= length) {
                        buffer.append(c);
                        break;
                    }
                    char next = message.charAt(nextIndex);
                    // if the next char is another single quote then write out one
                    if (next == '\'') {
                        i++;
                        buffer.append(c);
                        break;
                    }
                    // otherwise we want to read until we get to the next single quote
                    index = message.indexOf('\'', nextIndex);
                    // if there are no more in the string, then skip it
                    if (index == -1) {
                        buffer.append(c);
                        break;
                    }
                    // otherwise write out the chars inside the quotes
                    buffer.append(message, nextIndex, index);
                    i = index;
                    break;
                default:
                    buffer.append(c);
            }
        }
        return buffer.toString();
    }

    /*
     * Build an array of property files to search.  The returned array contains
     * the property fields in order from most specific to most generic.
     * So, in the FR_fr locale, it will return file_fr_FR.properties, then
     * file_fr.properties, and finally file.properties.
     */
    private static String[] buildVariants(String root) {
        if (nlSuffixes == null) {
            // build list of suffixes for loading resource bundles
            String nl = Locale.getDefault().toString();
            List<String> result = new ArrayList<>(4);
            int lastSeparator;
            while (true) {
                result.add('_' + nl + EXTENSION);
                String additional = getAdditionalSuffix(nl);
                if (additional != null) {
                    result.add('_' + additional + EXTENSION);
                }
                lastSeparator = nl.lastIndexOf('_');
                if (lastSeparator == -1)
                    break;
                nl = nl.substring(0, lastSeparator);
            }
            // add the empty suffix last (most general)
            result.add(EXTENSION);
            nlSuffixes = result.toArray(new String[result.size()]);
        }
        root = root.replace('.', '/');
        String[] variants = new String[nlSuffixes.length];
        for (int i = 0; i < variants.length; i++)
            variants[i] = root + nlSuffixes[i];
        return variants;
    }

    /*
     * This is a fix due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=579215
     * Ideally, this needs to be removed once the Eclipse minimum support moves to Java 17
     */
    private static String getAdditionalSuffix(String nl) {
        String additional = null;
        if (nl != null) {
            if ("he".equals(nl)) { //$NON-NLS-1$
                additional = "iw"; //$NON-NLS-1$
            } else if (nl.startsWith("he_")) { //$NON-NLS-1$
                additional = "iw_" + nl.substring(3); //$NON-NLS-1$
            }
        }

        return additional;
    }

    private static void computeMissingMessages(
        String bundleName,
        Class<?> clazz,
        Map<Object, Object> fieldMap,
        Field[] fieldArray,
        boolean isAccessible
    ) {
        // iterate over the fields in the class to make sure that there aren't any empty ones
        final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
        final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
        final int numFields = fieldArray.length;
        for (int i = 0; i < numFields; i++) {
            Field field = fieldArray[i];
            if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
                continue;
            // if the field has a a value assigned, there is nothing to do
            if (fieldMap.get(field.getName()) == ASSIGNED)
                continue;
            try {
                // Set a value for this empty field. We should never get an exception here because
                // we know we have a public static non-final field. If we do get an exception, silently
                // log it and continue. This means that the field will (most likely) be un-initialized and
                // will fail later in the code and if so then we will see both the NPE and this error.
                String value = "NLS missing message: " + field.getName() + " in: " + bundleName; //$NON-NLS-1$ //$NON-NLS-2$
                log.warn(value);
                if (!isAccessible)
                    field.setAccessible(true);
                field.set(null, value);
            } catch (Exception e) {
                log.error("Error setting the missing message value for: " + field.getName(), e); //$NON-NLS-1$
            }
        }
    }

    /*
     * Load the given resource bundle using the specified class loader.
     */
    static void load(final String bundleName, Class<?> clazz) {
        final Field[] fieldArray = clazz.getDeclaredFields();
        ClassLoader loader = clazz.getClassLoader();

        boolean isAccessible = (clazz.getModifiers() & Modifier.PUBLIC) != 0;

        // build a map of field names to Field objects
        Map<Object, Object> fields = new HashMap<>(fieldArray.length * 2);
        for (Field field : fieldArray) {
            fields.put(field.getName(), field);
        }

        // search the variants from most specific to most general, since
        // the MessagesProperties.put method will mark assigned fields
        // to prevent them from being assigned twice
        final String[] variants = buildVariants(bundleName);
        for (String variant : variants) {
            // loader==null if we're launched off the Java boot classpath
            final InputStream input = loader == null ? ClassLoader.getSystemResourceAsStream(variant) : loader.getResourceAsStream(variant);
            try (input) {
                if (input == null) {
                    continue;
                }
                final MessagesProperties properties = new MessagesProperties(fields, bundleName, isAccessible);
                final PropertyResourceBundle bundle = new PropertyResourceBundle(input);
                for (String key : bundle.keySet()) {
                    properties.put(key, bundle.getString(key));
                }
            } catch (IOException e) {
                log.error("Error loading " + variant, e); //$NON-NLS-1$
            }
            // ignore
        }
        computeMissingMessages(bundleName, clazz, fields, fieldArray, isAccessible);
    }

    /*
     * Class which sub-classes java.util.Properties and uses the #put method
     * to set field values rather than storing the values in the table.
     */
    private static class MessagesProperties extends Properties {

        private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
        private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;

        private final String bundleName;
        private final Map<Object, Object> fields;
        private final boolean isAccessible;

        public MessagesProperties(Map<Object, Object> fieldMap, String bundleName, boolean isAccessible) {
            super();
            this.fields = fieldMap;
            this.bundleName = bundleName;
            this.isAccessible = isAccessible;
        }

        /* (non-Javadoc)
         * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
         */
        @Override
        public synchronized Object put(Object key, Object value) {
            Object fieldObject = fields.put(key, ASSIGNED);
            // if already assigned, there is nothing to do
            if (fieldObject == ASSIGNED)
                return null;
            if (fieldObject == null) {
                final String msg = "NLS unused message: " + key + " in: " + bundleName;//$NON-NLS-1$ //$NON-NLS-2$
                // keys with '.' are ignored by design (bug 433424)
                if (key instanceof String && ((String) key).indexOf('.') < 0) {
                    log.warn(msg);
                }
                return null;
            }
            final Field field = (Field) fieldObject;
            // can only set value of public static non-final fields
            if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
                return null;
            try {
                // Check to see if we are allowed to modify the field. If we aren't (for instance
                // if the class is not public) then change the accessible attribute of the field
                // before trying to set the value.
                if (!isAccessible)
                    field.setAccessible(true);
                // Set the value into the field. We should never get an exception here because
                // we know we have a public static non-final field. If we do get an exception, silently
                // log it and continue. This means that the field will (most likely) be un-initialized and
                // will fail later in the code and if so then we will see both the NPE and this error.

                // Extra care is taken to be sure we create a String with its own backing char[] (bug 287183)
                // This is to ensure we do not keep the key chars in memory.
                field.set(null, new String(((String) value).toCharArray()));
            } catch (Exception e) {
                log.error("Exception setting field value.", e); //$NON-NLS-1$
            }
            return null;
        }
    }
}