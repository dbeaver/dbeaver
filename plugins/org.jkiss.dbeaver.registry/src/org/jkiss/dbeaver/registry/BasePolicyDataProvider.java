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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.function.Function;

/**
 * Base data policy provider designed to provide specific policy property value.
 * The general logic catch policy value in .ini file as system property next in
 * windows registry under HKEY_CURRENT_USER and HKEY_LOCAL_MACHINE nodes.
 */
public class BasePolicyDataProvider {
    private static final Log log = Log.getLog(BasePolicyDataProvider.class);

    private static final String DBEAVER_REGISTRY_POLICY_NODE = "Software\\DBeaver Corp\\DBeaver\\policy"; //$NON-NLS-1$
    private static final BasePolicyDataProvider INSTANCE = new BasePolicyDataProvider();

    @NotNull
    public static BasePolicyDataProvider getInstance() {
        return INSTANCE;
    }

    private BasePolicyDataProvider() {
        // private constructor
    }

    /**
     * Return boolean value of policy data property
     *
     * @param propertyName - property name
     * @return - boolean value
     */
    public boolean isPolicyEnabled(@NotNull String propertyName) {
        return getPolicyValue(propertyName, Boolean::valueOf) == Boolean.TRUE;
    }

    @Nullable
    public String getPolicyValue(@NotNull String propertyName) {
        return getPolicyValue(propertyName, Function.identity());
    }

    /**
     * Retrieves policy data value from system environment or Windows registry
     *
     * @param property  policy data property
     * @param converter converter function
     * @return policy data value or {@code null} if not found
     */
    @Nullable
    public <T> T getPolicyValue(@NotNull String property, @NotNull Function<String, T> converter) {
        String value = System.getProperty(property);
        if (value != null) {
            return converter.apply(value);
        }

        value = getRegistryPolicyValue(WinReg.HKEY_CURRENT_USER, property);
        if (value != null) {
            return converter.apply(value);
        }

        value = getRegistryPolicyValue(WinReg.HKEY_LOCAL_MACHINE, property);
        if (value != null) {
            return converter.apply(value);
        }

        return null;
    }

    @Nullable
    private static String getRegistryPolicyValue(@NotNull WinReg.HKEY root, @NotNull String property) {
        if (!RuntimeUtils.isWindows()) {
            return null;
        }

        try {
            if (Advapi32Util.registryKeyExists(root, DBEAVER_REGISTRY_POLICY_NODE) &&
                Advapi32Util.registryValueExists(root, DBEAVER_REGISTRY_POLICY_NODE, property)
            ) {
                return Advapi32Util.registryGetStringValue(root, DBEAVER_REGISTRY_POLICY_NODE, property);
            }
        } catch (Throwable e) {
            log.error("Error reading Windows registry", e);
        }

        return null;
    }
}
