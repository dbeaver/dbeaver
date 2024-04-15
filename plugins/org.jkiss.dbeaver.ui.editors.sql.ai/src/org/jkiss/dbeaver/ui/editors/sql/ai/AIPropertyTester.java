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
package org.jkiss.dbeaver.ui.editors.sql.ai;

import org.eclipse.core.expressions.PropertyTester;
import org.jkiss.dbeaver.model.ai.AISettingsRegistry;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.function.Consumer;

public class AIPropertyTester extends PropertyTester {

    public static final String NAMESPACE = "org.jkiss.dbeaver.ui.editors.sql.ai";
    public static final String PROP_IS_DISABLED = "isDisabled";

    private final Consumer<AISettingsRegistry> settingsChangedListener = s -> {
        UIUtils.asyncExec(() -> firePropertyChange(PROP_IS_DISABLED));
    };

    public AIPropertyTester() {
        AISettingsRegistry.getInstance().addChangedListener(this.settingsChangedListener);
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        switch (property) {
            case PROP_IS_DISABLED: {
                return AISettingsRegistry.getInstance().getSettings().isAiDisabled();
            }
        }
        return false;
    }

    public static void firePropertyChange(String propName) {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }
}
