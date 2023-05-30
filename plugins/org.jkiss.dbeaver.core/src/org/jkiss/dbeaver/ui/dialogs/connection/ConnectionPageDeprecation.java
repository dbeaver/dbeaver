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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.ShellUtils;

public class ConnectionPageDeprecation extends ConnectionWizardPage {
    private final DBPDataSourceContainer dataSource;

    public ConnectionPageDeprecation(@NotNull DBPDataSourceContainer dataSource) {
        super(ConnectionPageDeprecation.class.getName());
        this.dataSource = dataSource;

        setTitle("Deprecated driver");
        setDescription("This driver is deprecated and cannot be used");
    }

    @Override
    public void createControl(Composite parent) {
        final FormToolkit toolkit = new FormToolkit(parent.getDisplay());

        final Composite composite = toolkit.createComposite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout(SWT.HORIZONTAL | SWT.VERTICAL));

        final FormText text = new FormText(composite, SWT.NO_FOCUS);
        text.setFont("header", JFaceResources.getFont("org.eclipse.jface.headerfont"));
        text.setText(dataSource.getDriver().getDeprecationReason(), true, false);
        text.setHyperlinkSettings(toolkit.getHyperlinkGroup());
        text.addHyperlinkListener(IHyperlinkListener.linkActivatedAdapter(e -> ShellUtils.launchProgram(e.getHref().toString())));

        toolkit.adapt(text, false, true);

        setControl(composite);
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSourceDescriptor) {
        // do nothing
    }
}
