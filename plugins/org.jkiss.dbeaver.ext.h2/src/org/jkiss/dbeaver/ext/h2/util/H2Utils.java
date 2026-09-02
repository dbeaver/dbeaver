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
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ext.h2.internal.H2Constants;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

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

    public static void resetUserAllowedClasses() {
        ModelPreferences.getPreferences().setToDefault(H2Constants.PREF_ALLOWED_CLASSES);
    }

    @NotNull
    public static List<String> getSystemAllowedClasses() {
        return CommonUtils.splitString(System.getProperty("h2.allowedClasses"), ',');
    }

    public static void setSystemAllowedClasses(@NotNull List<String> allowedClasses) {
        // https://h2database.com/html/advanced.html#restricting_classes
        System.setProperty("h2.allowedClasses", String.join(",", allowedClasses));
    }
}
