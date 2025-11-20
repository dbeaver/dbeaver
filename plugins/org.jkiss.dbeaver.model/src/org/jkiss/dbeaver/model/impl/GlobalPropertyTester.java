/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

/**
 * GlobalPropertyTester
 */
public class GlobalPropertyTester extends PropertyTester {
    public static final String NAMESPACE = "org.jkiss.dbeaver.core.global";
    public static final String PROP_STANDALONE = "standalone";
    public static final String PROP_DISTRIBUTED = "distributed";
    public static final String PROP_BUNDLE_INSTALLED = "bundleInstalled";
    public static final String PROP_HAS_PREFERENCE = "hasPreference";
    public static final String PROP_HAS_ENV_VARIABLE = "hasEnvVariable";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        return switch (property) {
            case PROP_STANDALONE -> DBWorkbench.getPlatform().getApplication().isStandalone();
            case PROP_DISTRIBUTED -> DBWorkbench.isDistributed();
            case PROP_BUNDLE_INSTALLED -> Platform.getBundle((String) args[0]) != null;
            case PROP_HAS_PREFERENCE -> {
                String prefName = CommonUtils.toString(expectedValue);
                String prefValue = DBWorkbench.getPlatform().getPreferenceStore().getString(prefName);
                if (CommonUtils.isEmpty(prefValue)) {
                    prefValue = System.getProperty(prefName);
                    if (prefValue != null && prefValue.isEmpty()) {
                        prefValue = Boolean.TRUE.toString();
                    }
                }
                yield CommonUtils.toBoolean(prefValue);
            }
            case PROP_HAS_ENV_VARIABLE -> {
                String prefName = CommonUtils.toString(expectedValue);
                String prefValue = System.getenv(prefName);
                yield (prefValue != null && prefValue.isEmpty()) || CommonUtils.toBoolean(prefValue);
            }
            default -> false;
        };
    }

}
