/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Optional;

public class WindowsBrowserRegistry {

    private static final String INTERNAL_BROWSER_PROPERTY = "org.eclipse.swt.browser.DefaultType";

    //Windows versions which could not have Edge as an available browser
    private static final String[] LEGACY_OS = {
        "Windows 98", //$NON-NLS-1$
        "Windows XP", //$NON-NLS-1$
        "Windows Vista", //$NON-NLS-1$
        "Windows 7", //$NON-NLS-1$
    };

    public enum BrowserSelection {
        EDGE("edge", "Microsoft Edge", 0),
        INTERNET_EXPLORER("ie", "Internet Explorer", 1);

        private final String type;
        private final String name;
        private final int index;

        BrowserSelection(@NotNull String type, @NotNull String name, int index) {
            this.type = type;
            this.name = name;
            this.index = index;
        }

        public static BrowserSelection get(int index) {
            Optional<BrowserSelection> first = Arrays.stream(values()).filter(it -> it.index == index).findFirst();
            return first.orElse(null);
        }

        public static BrowserSelection get(String type) {
            Optional<BrowserSelection> first = Arrays.stream(values()).filter(it -> it.type.equals(type)).findFirst();
            return first.orElse(null);
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * @return Get selected browser or default browser if none is selected
     */
    public static BrowserSelection getActiveBrowser() {
        DBPPreferenceStore preferences = ModelPreferences.getPreferences();
        String type = preferences.getString(ModelPreferences.CLIENT_BROWSER);
        if (CommonUtils.isEmpty(type)) {
            return getDefaultBrowser();
        } else {
            return BrowserSelection.get(type);
        }
    }

    /**
     * @return returns browser by default, depends on OS
     */
    public static BrowserSelection getDefaultBrowser() {
        if (RuntimeUtils.isWindows() && Arrays.stream(LEGACY_OS)
            .anyMatch(it -> it.equalsIgnoreCase(System.getProperty("os"
                + ".name")))) {
            return BrowserSelection.INTERNET_EXPLORER;
        } else {
            return BrowserSelection.EDGE;
        }
    }

    /**
     * Overrides default browser, we want to use Edge for newer version
     * if the user didn't specify otherwise
     */
    public static void overrideBrowser() {
        System.setProperty(INTERNAL_BROWSER_PROPERTY, getActiveBrowser().type);
    }

    public static void setActiveBrowser(BrowserSelection browser) {
        DBPPreferenceStore preferences = ModelPreferences.getPreferences();
        preferences.setValue(ModelPreferences.CLIENT_BROWSER, browser.type);
        System.setProperty(INTERNAL_BROWSER_PROPERTY, preferences.getString(ModelPreferences.CLIENT_BROWSER));
    }

}
