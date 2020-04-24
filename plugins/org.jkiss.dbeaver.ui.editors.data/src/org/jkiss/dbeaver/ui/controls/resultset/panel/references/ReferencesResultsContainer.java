/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
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
    private static final String V_PROP_ACTIVE_ASSOCIATIONS = "ref-panel-associations";

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
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.verticalIndent = 5;
        keySelectorPanel.setLayoutData(gd);
        UIUtils.createControlLabel(keySelectorPanel, "Reference");
        fkCombo = new CSmartCombo<>(keySelectorPanel, SWT.BORDER | SWT.DROP_DOWN | SWT.READ_ONLY, new RefKeyLabelProvider());
        fkCombo.addItem(null);
        fkCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fkCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                activeReferenceKey = fkCombo.getSelectedItem();
                if (activeReferenceKey == null) {
                    return;
                }
                refreshKeyValues(true);

                // Save active keys in virtual entity props
                {
                    DBVEntity vEntityOwner = DBVUtils.getVirtualEntity(parentDataContainer, true);
                    List<Map<String, Object>> activeAssociations = new ArrayList<>();
                    activeAssociations.add(activeReferenceKey.createMemo());
                    Object curActiveAssociations = vEntityOwner.getProperty(V_PROP_ACTIVE_ASSOCIATIONS);
                    if (!CommonUtils.equalObjects(curActiveAssociations, activeAssociations)) {
                        vEntityOwner.setProperty(V_PROP_ACTIVE_ASSOCIATIONS, activeAssociations);
                        vEntityOwner.persistConfiguration();
                    }
                }

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

    @Nullable
    @Override
    public DBPProject getProject() {
        DBSDataContainer dataContainer = getDataContainer();
        return dataContainer == null || dataContainer.getDataSource() == null ? null : dataContainer.getDataSource().getContainer().getProject();
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
        if (parentDataContainer == null) {
            return;
        }
        Set<DBSEntity> allEntities = new LinkedHashSet<>();
        for (DBDAttributeBinding attr : visibleAttributes) {
            DBSEntityAttribute entityAttribute = attr.getEntityAttribute();
            if (entityAttribute != null) {
                allEntities.add(entityAttribute.getParentObject());
            }
        }
        if (allEntities.isEmpty() && parentDataContainer instanceof DBSEntity) {
            allEntities.add((DBSEntity) parentDataContainer);
        }

        List<ReferenceKeyMemo> refKeyMemos = new ArrayList<>();
        {
            DBVEntity vEntityOwner = DBVUtils.getVirtualEntity(parentDataContainer, false);
            if (vEntityOwner != null) {
                Object activeAssociations = vEntityOwner.getProperty(V_PROP_ACTIVE_ASSOCIATIONS);
                if (activeAssociations instanceof Collection) {
                    for (Object refKeyMemoMap : (Collection)activeAssociations) {
                        if (refKeyMemoMap instanceof Map) {
                            refKeyMemos.add(new ReferenceKeyMemo((Map) refKeyMemoMap));
                        }
                    }
                }
            }
        }

        if (!allEntities.isEmpty()) {
            new AbstractJob("Load reference keys") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        List<ReferenceKey> refs = new ArrayList<>();
                        for (DBSEntity entity : allEntities) {
                            if (entity instanceof DBVEntity) {
                                // Skip virtual entities
                                continue;
                            }
                            // Foreign keys
                            Collection<? extends DBSEntityAssociation> associations = DBVUtils.getAllAssociations(monitor, entity);
                            for (DBSEntityAssociation assoc : associations) {
                                if (assoc instanceof DBSEntityReferrer) {
                                    List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) assoc).getAttributeReferences(monitor);
                                    if (!CommonUtils.isEmpty(attrs)) {
                                        ReferenceKey referenceKey = new ReferenceKey(monitor, false, assoc.getAssociatedEntity(), assoc, attrs);
                                        refs.add(referenceKey);
                                    }
                                }
                            }

                            // References
                            Collection<? extends DBSEntityAssociation> references = DBVUtils.getAllReferences(monitor, entity);
                            {
                                for (DBSEntityAssociation assoc : references) {
                                    if (assoc instanceof DBSEntityReferrer) {
                                        List<? extends DBSEntityAttributeRef> attrs = ((DBSEntityReferrer) assoc).getAttributeReferences(monitor);
                                        if (!CommonUtils.isEmpty(attrs)) {
                                            ReferenceKey referenceKey = new ReferenceKey(monitor, true, entity, assoc, attrs);
                                            refs.add(referenceKey);
                                        }
                                    }
                                }
                            }
                        }
                        synchronized (referenceKeys) {
                            referenceKeys.clear();
                            referenceKeys.addAll(refs);

                            // Detect active ref key from memo
                            if (!referenceKeys.isEmpty()) {
                                if (!refKeyMemos.isEmpty()) {
                                    for (ReferenceKey key : referenceKeys) {
                                        for (ReferenceKeyMemo memo : refKeyMemos) {
                                            if (key.matches(memo)) {
                                                activeReferenceKey = key;
                                                break;
                                            }
                                        }
                                        if (activeReferenceKey != null) break;
                                    }
                                }
                                if (activeReferenceKey == null) {
                                    activeReferenceKey = referenceKeys.get(0);
                                }
                            }
                        }
                        UIUtils.syncExec(() -> fillKeysCombo());
                    } catch (DBException e) {
                        log.debug("Error reading references", e);
                        // Do not show errors. References or FKs may be unsupported by current database
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    private void fillKeysCombo() {
        if (fkCombo.isDisposed()) {
            return;
        }
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
        new AbstractJob("Read references") {
            {
                //setUser(true);
                //setSystem(false);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    DBSEntity realEntity = DBVUtils.getRealEntity(monitor, activeReferenceKey.refEntity);
                    if (!(realEntity instanceof DBSDataContainer)) {
                        log.error("Referencing entity is not a data container");
                        return Status.OK_STATUS;
                    }
                    dataContainer = (DBSDataContainer) realEntity;

                    List<ResultSetRow> selectedRows = parentController.getSelection().getSelectedRows();
                    if (!force && CommonUtils.equalObjects(lastSelectedRows, selectedRows)) {
                        return Status.OK_STATUS;
                    }
                    lastSelectedRows = selectedRows;
                    if (selectedRows.isEmpty()) {
                        UIUtils.asyncExec(() -> {
                            dataViewer.clearData();
                            dataViewer.showEmptyPresentation();
                        });
                    } else {
                        if (activeReferenceKey.isReference) {
                            dataViewer.navigateReference(
                                monitor,
                                parentController.getModel(),
                                activeReferenceKey.refAssociation,
                                selectedRows,
                                false);
                        } else {
                            dataViewer.navigateAssociation(
                                monitor,
                                parentController.getModel(),
                                activeReferenceKey.refAssociation,
                                selectedRows, false);

                        }
                    }
                } catch (Exception e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    static class ReferenceKey {
        private final boolean isReference;
        private final DBSEntity refEntity;
        private final DBSEntityAssociation refAssociation;
        private final List<? extends DBSEntityAttributeRef> refAttributes;
        private DBSEntity targetEntity;

        ReferenceKey(DBRProgressMonitor monitor, boolean isReference, DBSEntity refEntity, DBSEntityAssociation refAssociation, List<? extends DBSEntityAttributeRef> refAttributes) {
            this.isReference = isReference;
            this.refEntity = refEntity;
            this.refAssociation = refAssociation;
            this.refAttributes = refAttributes;

            if (refAssociation != null) {
                if (isReference) {
                    targetEntity = refAssociation.getParentObject();
                } else {
                    DBSEntityConstraint refConstraint = refAssociation.getReferencedConstraint();
                    if (refConstraint != null) {
                        targetEntity = refConstraint.getParentObject();
                    }
                }
            }
            if (targetEntity instanceof DBVEntity) {
                try {
                    DBSEntity realEntity = ((DBVEntity) targetEntity).getRealEntity(monitor);
                    if (realEntity != null) {
                        targetEntity = realEntity;
                    }
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }

        public boolean isReference() {
            return isReference;
        }

        boolean matches(ReferenceKeyMemo memo) {
            return isReference == memo.isReference &&
                CommonUtils.equalObjects(DBUtils.getObjectFullName(refEntity, DBPEvaluationContext.UI), memo.refEntityName) &&
                CommonUtils.equalObjects(refAssociation.getName(), memo.refAssociationName);
        }

        Map<String, Object> createMemo() {
            Map<String, Object> memo = new LinkedHashMap<>();
            memo.put("ref", isReference);
            memo.put("entity", DBUtils.getObjectFullName(refEntity, DBPEvaluationContext.UI));
            memo.put("name", refAssociation.getName());
            return memo;
        }
    }

    static class ReferenceKeyMemo {
        final boolean isReference;
        final String refEntityName;
        final String refAssociationName;

        ReferenceKeyMemo(Map<String, Object> map) {
            this.isReference = CommonUtils.toBoolean(map.get("ref"));
            this.refEntityName = CommonUtils.toString(map.get("entity"));
            this.refAssociationName = CommonUtils.toString(map.get("name"));
        }
    }

    private class RefKeyLabelProvider extends LabelProvider {

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
            String title;
            DBSObject targetEntity = key.targetEntity;
            title = targetEntity.getName() + " (" + key.refAssociation.getName() + ")";
            if (parentController.getDataContainer() != null && parentController.getDataContainer().getDataSource() != targetEntity.getDataSource()) {
                title += " [" + targetEntity.getDataSource().getContainer().getName() + "]";
            }

            return title;
        }

    }

}
