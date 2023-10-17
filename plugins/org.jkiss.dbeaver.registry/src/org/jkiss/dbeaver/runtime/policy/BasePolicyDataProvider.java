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

/**
 * Base data policy provider designed to provide specific policy property value
 */
public class BasePolicyDataProvider extends AbstractPolicyDataProvider {

    private static final String POLICY_DATA_EXPORT = "policy.data.export.disabled"; //$NON-NLS-1$
    private static final String POLICY_DATA_COPY = "policy.data.copy.disabled"; //$NON-NLS-1$
    private static final String POLICY_SOFTWARE_INSTALL = "policy.software.install.disabled"; //$NON-NLS-1$
    private static final String POLICY_SOFTWARE_UPDATE = "policy.software.update.disabled"; //$NON-NLS-1$

    /**
     * The method return true if export data procedure disabled
     *
     * @return boolean value
     */
    public boolean isExportDataDisabled() {
        return isDataPolicyEnabled(POLICY_DATA_EXPORT);
    }

    /**
     * The method return true if copy data procedure disabled
     *
     * @return boolean value
     */
    public boolean isCopyDataDisabled() {
        return isDataPolicyEnabled(POLICY_DATA_COPY);
    }

    /**
     * The method return true if install new software procedure disabled
     *
     * @return boolean value
     */
    public boolean isInstallSoftwareDisabled() {
        return isDataPolicyEnabled(POLICY_SOFTWARE_INSTALL);
    }

    /**
     * The method return true if update new software procedure disabled
     *
     * @return boolean value
     */
    public boolean isUpdateSoftwareDisabled() {
        return isDataPolicyEnabled(POLICY_SOFTWARE_UPDATE);
    }
}
