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
import org.jkiss.dbeaver.DBException;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * IObjectPropertyConfiguratorProvider
 */
public interface IObjectPropertyConfiguratorProvider<OBJECT, SETTINGS, PARAMETER, CONFIGURATOR extends IObjectPropertyConfigurator<OBJECT, SETTINGS>> {

    @SuppressWarnings("unchecked")
    @Nullable
    CONFIGURATOR createConfigurator(OBJECT object, PARAMETER parameter) throws DBException;

    static <OBJECT, SETTINGS>  IObjectPropertyConfigurator<OBJECT, SETTINGS> createPlaceholdingConfigurator(BiConsumer<Composite, OBJECT> uiBuilder) {
        return new IObjectPropertyConfigurator<OBJECT, SETTINGS>() {
            @Override
            public void createControl(@NotNull Composite parent, OBJECT object, @NotNull Runnable propertyChangeListener) {
                uiBuilder.accept(parent, object);
            }

            @Override
            public void loadSettings(@NotNull SETTINGS settings) {
            }

            @Override
            public void saveSettings(@NotNull SETTINGS settings) {
            }

            @Override
            public void resetSettings(@NotNull SETTINGS settings) {
            }

            @Override
            public boolean isComplete() {
                return true;
            }
        };
    }
}
