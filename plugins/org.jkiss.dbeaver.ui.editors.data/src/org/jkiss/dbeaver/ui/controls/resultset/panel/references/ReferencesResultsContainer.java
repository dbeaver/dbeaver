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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CSmartCombo;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

class ReferencesResultsContainer implements IResultSetContainer {

    private static final Log log = Log.getLog(ReferencesResultsContainer.class);

    private final IResultSetController parentController;
    private final Composite mainComposite;
    private final CSmartCombo<ReferenceKey> fkCombo;
    private ResultSetViewer dataViewer;

    private DBSDataContainer parentDataContainer;
    private DBSDataContainer dataContainer;

    private final List<ReferenceKey> referenceKeys = new ArrayList<>();
    private ReferenceKey activeReferenceKey;

    private List<ResultSetRow> lastSelectedRows;

    ReferencesResultsContainer(Composite parent, IResultSetController parentController) {
        this.parentController = parentController;

        this.mainComposite = UIUtils.createComposite(parent, 1);

        Composite keySelectorPanel = UIUtils.createComposite(this.mainComposite, 2);
        keySelectorPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        UIUtils.createControlLabel(keySelectorPanel, "Reference");
        fkCombo = new CSmartCombo<>(keySelectorPanel, SWT.DROP_DOWN | SWT.READ_ONLY, new RefKeyLabelProvider());
        fkCombo.addItem(null);
        fkCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fkCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                activeReferenceKey = fkCombo.getSelectedItem();
                refreshKeyValues(true);
            }
        });

        {
            Composite viewerContainer = new Composite(mainComposite, SWT.NONE);
            viewerContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
            viewerContainer.setLayout(new FillLayout());
            this.dataViewer = new ResultSetViewer(viewerContainer, parentController.getSite(), this);
        }
    }

    public ReferenceKey getActiveReferenceKey() {
        return activeReferenceKey;
    }

    public IResultSetPresentation getOwnerPresentation() {
        return parentController.getActivePresentation();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return DBUtils.getDefaultContext(dataContainer, false);
    }

    @Override
    public IResultSetController getResultSetController() {
        return dataViewer;
    }

    @Override
    public DBSDataContainer getDataContainer() {
        return this.dataContainer;
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

    public Composite getControl() {
        return mainComposite;
    }

    public void refreshReferences() {
        dataViewer.resetHistory();
        DBSDataContainer newParentContainer = this.parentController.getDataContainer();
        if (newParentContainer != parentDataContainer) {
            refreshReferenceKeyList();
        } else if (dataContainer != null) {
            refreshKeyValues(false);
        }
    }

    /**
     * Load list of referencing keys
     */
    private void refreshReferenceKeyList() {
        activeReferenceKey = null;
        referenceKeys.clear();

        UIUtils.syncExec(() -> {
            dataViewer.clearData();
            fkCombo.removeAll();
            dataViewer.showEmptyPresentation();
        });
        List<DBDAttributeBinding> visibleAttributes = parentController.getModel().getVisibleAttributes();
        if (visibleAttributes.isEmpty()) {
            return;
        }

        parentDataContainer = parentController.getDataContainer();
        Set<DBSEntity> allEntities = new LinkedHashSet<>();
        for (DBDAttributeBinding attr : visibleAttributes) {
            DBSEntityAttribute entityAttribute = attr.getEntityAttribute();
            if (entityAttribute != null) {
                allEntities.add(entityAttribute.getParentObject());
            }
        }

        if (!allEntities.isEmpty()) {
            new AbstractJob("Load reference keys") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        List<ReferenceKey> refs = new ArrayList<>();
                        for (DBSEntity entity : allEntities) {
                            // Foreign keys
                            Collection<? extends DBSEntityAssociation> associations = DBVUtils.getAllAssociations(monitor, entity);
                            for (DBSEntityAssociation assoc : associations) {
                                if (assoc instanceof DBSEntityReferrer) {
                                    List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) assoc).getAttributeReferences(monitor);
                                    if (!CommonUtils.isEmpty(attrs)) {
                                        ReferenceKey referenceKey = new ReferenceKey(false, assoc.getAssociatedEntity(), assoc, attrs);
                                        refs.add(referenceKey);
                                    }
                                }
                            }

                            // References
                            Collection<? extends DBSEntityAssociation> references = entity.getReferences(monitor);
                            if (references != null) {
                                for (DBSEntityAssociation assoc : references) {
                                    if (assoc instanceof DBSEntityReferrer) {
                                        List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) assoc).getAttributeReferences(monitor);
                                        if (!CommonUtils.isEmpty(attrs)) {
                                            ReferenceKey referenceKey = new ReferenceKey(true, entity, assoc, attrs);
                                            refs.add(referenceKey);
                                        }
                                    }
                                }
                            }
                        }
                        synchronized (referenceKeys) {
                            referenceKeys.clear();
                            referenceKeys.addAll(refs);
                            if (!referenceKeys.isEmpty()) {
                                activeReferenceKey = referenceKeys.get(0);
                            }
                        }
                        UIUtils.syncExec(() -> fillKeysCombo());
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    private void fillKeysCombo() {
        fkCombo.removeAll();
        if (referenceKeys.isEmpty()) {
            fkCombo.addItem(null);
        }
        for (ReferenceKey key : referenceKeys) {
            fkCombo.addItem(key);
            if (key == activeReferenceKey) {
                fkCombo.select(key);
            }
        }

        if (activeReferenceKey != null) {
            refreshKeyValues(true);
        }
    }

    /**
     * Refresh data
     */
    private void refreshKeyValues(boolean force) {
        if (activeReferenceKey == null) {
            //log.error("No active reference key");
            return;
        }
        if (!(activeReferenceKey.refEntity instanceof DBSDataContainer)) {
            log.error("Referencing entity is not a data container");
            return;
        }
        dataContainer = (DBSDataContainer) activeReferenceKey.refEntity;
        try {
            List<ResultSetRow> selectedRows = parentController.getSelection().getSelectedRows();
            if (!force && CommonUtils.equalObjects(lastSelectedRows, selectedRows)) {
                return;
            }
            lastSelectedRows = selectedRows;
            if (selectedRows.isEmpty()) {
                this.dataViewer.clearData();
                this.dataViewer.showEmptyPresentation();
            } else {
                if (activeReferenceKey.isReference) {
                    this.dataViewer.navigateReference(
                        new VoidProgressMonitor(),
                        parentController.getModel(),
                        activeReferenceKey.refAssociation,
                        selectedRows,
                        false);
                } else {
                    this.dataViewer.navigateAssociation(
                        new VoidProgressMonitor(),
                        parentController.getModel(),
                        activeReferenceKey.refAssociation,
                        selectedRows, false);

                }
            }
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Can't shwo references", "Error opening '" + dataContainer.getName() + "' references", e);
        }
    }

    static class ReferenceKey {
        final boolean isReference;
        final DBSEntity refEntity;
        final DBSEntityAssociation refAssociation;
        final List<? extends DBSEntityAttributeRef> refAttributes;

        public ReferenceKey(boolean isReference, DBSEntity refEntity, DBSEntityAssociation refAssociation, List<? extends DBSEntityAttributeRef> refAttributes) {
            this.isReference = isReference;
            this.refEntity = refEntity;
            this.refAssociation = refAssociation;
            this.refAttributes = refAttributes;
        }
    }

    private static class RefKeyLabelProvider extends LabelProvider {

        @Override
        public Image getImage(Object element) {
            if (element == null) {
                return DBeaverIcons.getImage(DBIcon.TREE_ASSOCIATION);
            }
            ReferenceKey key = (ReferenceKey) element;
            return DBeaverIcons.getImage(
                key.isReference ? UIIcon.ARROW_LEFT_ALL : UIIcon.ARROW_RIGHT_ALL);
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return "<No references>";
            }
            ReferenceKey key = (ReferenceKey) element;
            if (key.isReference) {
                return key.refAssociation.getParentObject().getName() + " (" + key.refAssociation.getName() + ")";
            } else {
                return key.refAssociation.getReferencedConstraint().getParentObject().getName() + " (" + key.refAssociation.getName() + ")";
            }
        }

    }

}
