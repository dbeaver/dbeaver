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

import java.util.prefs.Preferences;

/**
 * Abstract specification of policy data provider
 */
public abstract class AbstractPolicyDataProvider implements PolicyDataProvider {

    private static final String DBEAVER_APPLICATION_POLICY_NODE = "dbeaver/application/policy"; //$NON-NLS-1$

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

    @Override
    public boolean getDataPolicyFromSystem(String property) {
        String isDisabledPropValue = System.getProperty(property);
        if (isDisabledPropValue != null && !isDisabledPropValue.isEmpty()) {
            return Boolean.valueOf(isDisabledPropValue);
        }
        return false;
    }

    @Override
    public boolean getDataPolicyFromRegistry(String property) {
        Preferences preference = Preferences.userRoot().node(DBEAVER_APPLICATION_POLICY_NODE);
        if (preference != null) {
            return preference.getBoolean(property, false);
        }
        return false;
    }

}
