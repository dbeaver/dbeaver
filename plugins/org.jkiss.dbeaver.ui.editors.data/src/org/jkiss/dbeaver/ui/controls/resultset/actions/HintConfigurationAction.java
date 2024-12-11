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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.data.hints.ValueHintProviderDescriptor;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class HintConfigurationAction extends AbstractResultSetViewerAction {
    private final ValueHintProviderDescriptor descriptor;
    private final UIPropertyConfiguratorDescriptor configurator;

    public HintConfigurationAction(
        ResultSetViewer resultSetViewer,
        ValueHintProviderDescriptor hd,
        UIPropertyConfiguratorDescriptor configurator
    ) {
        super(resultSetViewer, hd.getLabel());
        this.descriptor = hd;
        this.configurator = configurator;
        setToolTipText("Configure " + hd.getDescription());
    }

    @Override
    public boolean isChecked() {
        return descriptor.isEnabled();
    }

    @Override
    public void run() {
        //getResultSetViewer().refreshData(null);
    }

}
