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
package org.jkiss.dbeaver.ext.h2.util;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.h2.internal.H2Constants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class H2Utils {
    private H2Utils() {
    }

    @NotNull
    public static List<String> getUserAllowedClasses() {
        return CommonUtils.splitString(ModelPreferences.getPreferences().getString(H2Constants.PREF_ALLOWED_CLASSES), ',');
    }

    public static void setUserAllowedClasses(@NotNull List<String> allowedClasses) {
        var store = ModelPreferences.getPreferences();
        store.setValue(H2Constants.PREF_ALLOWED_CLASSES, String.join(",", allowedClasses));
        PrefUtils.savePreferenceStore(store);
    }

    public static void setDefaultUserAllowedClasses(@NotNull List<String> allowedClasses) {
        ModelPreferences.getPreferences().setDefault(H2Constants.PREF_ALLOWED_CLASSES, String.join(",", allowedClasses));
    }

    @NotNull
    public static List<String> getDefaultUserAllowedClasses() {
        return CommonUtils.splitString(
            ModelPreferences.getPreferences().getDefaultString(H2Constants.PREF_ALLOWED_CLASSES),
            ','
        );
    }

    @NotNull
    public static List<String> getSystemAllowedClasses() {
        return CommonUtils.splitString(System.getProperty("h2.allowedClasses"), ',');
    }

    public static void setSystemAllowedClasses(@NotNull List<String> allowedClasses) {
        // https://h2database.com/html/advanced.html#restricting_classes
        System.setProperty("h2.allowedClasses", String.join(",", allowedClasses));
    }

    public static boolean isClassLoadingRestricted() {
        return !getSystemAllowedClasses().contains("*");
    }

    public static boolean isJavaSourceDefinition(@NotNull Connection connection, @NotNull String query) throws SQLException {
        // H2 is loaded dynamically, so inspect its parsed command without introducing a driver dependency.
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            return containsJavaSource(connection, getRequiredField(statement, "command"));
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new SQLException("Unable to inspect parsed H2 query", e);
        }
    }

    private static boolean containsJavaSource(@NotNull Connection connection, @NotNull Object command)
            throws ReflectiveOperationException, SQLException {
        return switch (command.getClass().getName()) {
            case "org.h2.command.CommandContainer" ->
                containsJavaSource(connection, getRequiredField(command, "prepared"));
            case "org.h2.command.CommandList" -> {
                if (containsJavaSource(connection, getRequiredField(command, "command"))) {
                    yield true;
                }
                try {
                    for (Object prepared : (List<?>) getRequiredField(command, "commands")) {
                        if (containsJavaSource(connection, prepared)) {
                            yield true;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // Some H2 1.x versions keep subsequent statements only in 'remaining'.
                }
                String remaining = (String) getField(command, "remaining");
                if (remaining != null) {
                    try {
                        yield isJavaSourceDefinition(connection, remaining);
                    } catch (SQLException e) {
                        if (e.getCause() instanceof ReflectiveOperationException ||
                            e.getCause() instanceof RuntimeException
                        ) {
                            throw e;
                        }
                        // It may depend on a preceding statement and cannot be prepared separately.
                    }
                }
                yield false;
            }
            case "org.h2.command.ddl.CreateFunctionAlias" -> getField(command, "source") != null;
            case "org.h2.command.ddl.CreateTrigger" -> getField(command, "triggerSource") != null;
            default -> false;
        };
    }

    @NotNull
    private static Object getRequiredField(@NotNull Object object, @NotNull String name)
            throws ReflectiveOperationException {
        Object value = getField(object, name);
        if (value == null) {
            throw new IllegalStateException("H2 field '" + name + "' is null");
        }
        return value;
    }

    @Nullable
    private static Object getField(@NotNull Object object, @NotNull String name) throws ReflectiveOperationException {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e) {
                // Continue with the superclass.
            }
        }
        throw new NoSuchFieldException(name);
    }
}
