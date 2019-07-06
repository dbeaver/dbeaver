/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.references;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.resultset.*;

public class ReferencesResultsContainer implements IResultSetContainer {

    private final IResultSetPresentation presentation;
    private DBSDataContainer dataContainer;
    private ResultSetViewer referencesViewer;
    private final Composite mainComposite;

    public ReferencesResultsContainer(Composite parent, IResultSetPresentation presentation) {
        this.presentation = presentation;

        mainComposite = UIUtils.createComposite(parent, 1);

        CSmartCombo<DBSEntityAssociation> fkCombo = new CSmartCombo<>(mainComposite, SWT.DROP_DOWN | SWT.READ_ONLY, new LabelProvider() {

        });
        fkCombo.addItem(null);
        fkCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        {
            Composite viewerContainer = new Composite(mainComposite, SWT.NONE);
            viewerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
            viewerContainer.setLayout(new FillLayout());
            this.referencesViewer = new ResultSetViewer(viewerContainer, presentation.getController().getSite(), this);
        }
    }

    public IResultSetPresentation getOwnerPresentation() {
        return presentation;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return presentation.getController().getExecutionContext();
    }

    @Override
    public IResultSetController getResultSetController() {
        return referencesViewer;
    }

    @Override
    public DBSDataContainer getDataContainer() {
        return this.dataContainer;
    }

    public void setDataContainer(DBSDataContainer dataContainer) {
        this.dataContainer = dataContainer;
        this.referencesViewer.refresh();
    }

    @Override
    public boolean isReadyToRun() {
        return true;
    }

    @Override
    public void openNewContainer(DBRProgressMonitor monitor, @NotNull DBSDataContainer dataContainer, @NotNull DBDDataFilter newFilter) {

    }

    @Override
    public IResultSetDecorator createResultSetDecorator() {
        return new ReferencesResultsDecorator(this);
    }

    public Control getControl() {
        return mainComposite;
    }
}
