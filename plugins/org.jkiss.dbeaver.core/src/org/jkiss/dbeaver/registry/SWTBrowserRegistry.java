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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Objects;


public class SWTBrowserRegistry {

    private static final String INTERNAL_BROWSER_PROPERTY = "org.eclipse.swt.browser.DefaultType";

    // Windows versions which don't have Edge as an available browser, or need to install it manually

    private static final String[] LEGACY_WINDOWS_VERSIONS = {
        "Windows 98", //$NON-NLS-1$
        "Windows XP", //$NON-NLS-1$
        "Windows Vista", //$NON-NLS-1$
        "Windows 7", //$NON-NLS-1$
        "Windows 8", //$NON-NLS-1$
        "Windows 8.1", //$NON-NLS-1$
        "Windows Server 2008", //$NON-NLS-1$
        "Windows Server 2008 R2", //$NON-NLS-1$
        "Windows Server 2012", //$NON-NLS-1$
        "Windows Server 2012 R2", //$NON-NLS-1$
        "Windows Server 2016", //$NON-NLS-1$
        "Windows Server 2019", //$NON-NLS-1$
    };

    public enum BrowserSelection {
        EDGE("Microsoft Edge"),
        IE("Internet Explorer");

        private final String name;

        BrowserSelection(@NotNull String name) {
            this.name = name;
        }

        @NotNull
        public String getFullName() {
            return name;
        }

    }

    private SWTBrowserRegistry() {
        //prevents construction
    }

    /**
     * Returns selected via combo browser or default browser if none is selected
     */
    @NotNull
    public static BrowserSelection getActiveBrowser() {
        DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        String type = preferences.getString(ModelPreferences.CLIENT_BROWSER);
        if (CommonUtils.isEmpty(type)) {
            return getDefaultBrowser();
        } else {
            return Objects.requireNonNull(CommonUtils.valueOf(BrowserSelection.class, type));
        }
    }

    /**
     * Returns default browser depends on the OS
     */
    @NotNull
    public static BrowserSelection getDefaultBrowser() {

        if (RuntimeUtils.isWindows()
            && ArrayUtils.containsIgnoreCase(LEGACY_WINDOWS_VERSIONS, System.getProperty("os.name")
        )) {
            return BrowserSelection.IE;
        } else {
            return BrowserSelection.EDGE;
        }
    }

    /**
     * Overrides default browser, we want to use Edge for newer version
     * if the user didn't specify otherwise
     */
    public static void overrideBrowser() {
        System.setProperty(INTERNAL_BROWSER_PROPERTY, getActiveBrowser().name());
    }

    /**
     * By passing down BrowserSelection sets browser to be used by eclipse
     *
     * @param browser selected browser
     */
    public static void setActiveBrowser(@NotNull BrowserSelection browser) {
        DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        preferences.setValue(ModelPreferences.CLIENT_BROWSER, browser.name());
        System.setProperty(INTERNAL_BROWSER_PROPERTY, preferences.getString(ModelPreferences.CLIENT_BROWSER));
    }

}
