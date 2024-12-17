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
package org.jkiss.dbeaver.ui;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * IObjectPropertyConfigurator
 */
public interface IObjectPropertyConfigurator<OBJECT, SETTINGS> {
    /**
     * @param parent                 Parent composite
     * @param object                 Object
     * @param propertyChangeListener Can be called upon UI control change to update page completeness and other things.
     */
    void createControl(@NotNull Composite parent, OBJECT object, @NotNull Runnable propertyChangeListener);

    void loadSettings(@NotNull SETTINGS settings);

    void saveSettings(@NotNull SETTINGS settings);

    void resetSettings(@NotNull SETTINGS settings);

    boolean isComplete();

    @Nullable
    default String getErrorMessage() {
        return null;
    }

}
