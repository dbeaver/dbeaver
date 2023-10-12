/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.runtime.policy;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * Abstract specification of policy data provider
 */
public abstract class AbstractPolicyDataProvider implements PolicyDataProvider {

    private static final String DBEAVER_REGESTRY_POLICY_NODE = "Software\\DBeaver Corp\\DBeaver\\policy"; //$NON-NLS-1$

    @Override
    public boolean isDataPolicyEnabled(String propertyName) {
        boolean policyEnabled = false;
        policyEnabled = getDataPolicyFromSystem(propertyName);
        if (policyEnabled) {
            return policyEnabled;
        }
        policyEnabled = getDataPolicyFromRegistry(propertyName);
        return policyEnabled;
    }

    public boolean getDataPolicyFromSystem(String property) {
        String isDisabledPropValue = System.getProperty(property);
        if (isDisabledPropValue != null && !isDisabledPropValue.isEmpty()) {
            return Boolean.valueOf(isDisabledPropValue);
        }
        return false;
    }

    public boolean getDataPolicyFromRegistry(String property) {
        if (RuntimeUtils.isWindows()) {
            boolean isPolicyEnabled = getBooleanFromWinRegistryNode(WinReg.HKEY_CURRENT_USER, property);
            if (isPolicyEnabled) {
                return isPolicyEnabled;
            }
            isPolicyEnabled = getBooleanFromWinRegistryNode(WinReg.HKEY_LOCAL_MACHINE, property);
            if (isPolicyEnabled) {
                return isPolicyEnabled;
            }
        }
        return false;
    }

    private boolean getBooleanFromWinRegistryNode(WinReg.HKEY root, String property) {
        if (Advapi32Util.registryKeyExists(root, DBEAVER_REGESTRY_POLICY_NODE) &&
            Advapi32Util.registryValueExists(root, DBEAVER_REGESTRY_POLICY_NODE, property)) {
            String propRegisrtryValue = Advapi32Util.registryGetStringValue(
                root,
                DBEAVER_REGESTRY_POLICY_NODE,
                property);
            return Boolean.valueOf(propRegisrtryValue);
        }
        return false;
    }

}
