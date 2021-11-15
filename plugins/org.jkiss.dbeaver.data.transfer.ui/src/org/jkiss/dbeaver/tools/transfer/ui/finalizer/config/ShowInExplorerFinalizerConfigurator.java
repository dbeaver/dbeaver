/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.finalizer.config;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferFinalizerConfigurator;

import java.util.Map;

public class ShowInExplorerFinalizerConfigurator implements IDataTransferFinalizerConfigurator {
    @Override
    public void createControl(@NotNull Composite parent, @NotNull Runnable propertyChangeListener) {
        // not implemented
    }

    @Override
    public void loadSettings(@NotNull Map<String, Object> settings) {
        // not implemented
    }

    @Override
    public void saveSettings(@NotNull Map<String, Object> settings) {
        // not implemented
    }

    @Override
    public void resetSettings(@NotNull Map<String, Object> settings) {
        // not implemented
    }

    @Override
    public boolean isComplete() {
        return true;
    }

    @Override
    public boolean isApplicable(@NotNull StreamConsumerSettings configuration) {
        return !configuration.isOutputClipboard();
    }

    @Override
    public boolean hasControl() {
        return false;
    }
}
