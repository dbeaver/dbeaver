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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.EmptyAction;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * References
 */
public class ResultSetReferenceMenu
{

    static Action NOREFS_ACTION, REFS_TITLE_ACTION, NOFKS_ACTION, FKS_TITLE_ACTION;

    static {
        NOREFS_ACTION = new EmptyAction(ResultSetMessages.controls_resultset_ref_menu_no_references);
        REFS_TITLE_ACTION = new EmptyAction(ResultSetMessages.controls_resultset_ref_menu_references);
        NOFKS_ACTION = new EmptyAction(ResultSetMessages.controls_resultset_ref_menu_no_associations);
        FKS_TITLE_ACTION = new EmptyAction(ResultSetMessages.controls_resultset_ref_menu_associations);
    }


    static void fillRefTablesActions(@Nullable DBRProgressMonitor extMonitor, ResultSetViewer viewer, List<? extends DBDValueRow> rows, DBSEntity singleSource, IMenuManager manager, boolean openInNewWindow) {

        final List<DBSEntityAssociation> references = new ArrayList<>();
        final List<DBSEntityAssociation> associations = new ArrayList<>();

        DBRRunnableWithProgress refCollector = monitor -> {
            try {
                monitor.beginTask("Read references", 1);
                Collection<? extends DBSEntityAssociation> refs = DBVUtils.getAllReferences(monitor, singleSource);
                {
                    monitor.beginTask("Check references", refs.size());
                    for (DBSEntityAssociation ref : refs) {
                        if (monitor.isCanceled()) {
                            return;
                        }
                        monitor.subTask("Check references " + ref.getName());
                        boolean allMatch = true;
                        DBSEntityConstraint ownConstraint = ref.getReferencedConstraint();
                        if (ownConstraint instanceof DBSEntityReferrer) {
                            List<? extends DBSEntityAttributeRef> attributeReferences = ((DBSEntityReferrer) ownConstraint).getAttributeReferences(monitor);
                            if (attributeReferences != null) {
                                for (DBSEntityAttributeRef ownAttrRef : attributeReferences) {
                                    if (viewer.getModel().getAttributeBinding(ownAttrRef.getAttribute()) == null) {
                                        // Attribute is not in the list - skip this association
                                        allMatch = false;
                                        break;
                                    }
                                }
                            } else {
                                allMatch = false;
                            }
                        }
                        if (allMatch) {
                            references.add(ref);
                        }
                        monitor.worked(1);
                    }
                }
                monitor.done();
                if (monitor.isCanceled()) {
                    return;
                }
                monitor.beginTask("Read associations", 1);
                Collection<? extends DBSEntityAssociation> fks = singleSource.getAssociations(monitor);
                if (fks != null) {
                    monitor.beginTask("Check associations", fks.size());
                    for (DBSEntityAssociation fk : fks) {
                        if (monitor.isCanceled()) {
                            return;
                        }
                        monitor.subTask("Check association " + fk.getName());
                        boolean allMatch = true;
                        if (fk instanceof DBSEntityReferrer && fk.getReferencedConstraint() != null) {
                            for (DBSEntityAttributeRef ownAttr : ((DBSEntityReferrer) fk).getAttributeReferences(monitor)) {
                                if (viewer.getModel().getAttributeBinding(ownAttr.getAttribute()) == null) {
                                    // Attribute is not in the list - skip this association
                                    allMatch = false;
                                    break;
                                }
                            }
                        }
                        if (allMatch) {
                            associations.add(fk);
                        }
                        monitor.worked(1);
                    }
                }
                monitor.done();
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        };
        try {
            if (extMonitor != null) {
                refCollector.run(extMonitor);
            } else {
                UIUtils.runInProgressService(refCollector);
            }
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Table References", "Error reading referencing tables for '" + singleSource.getName() + "'", e.getTargetException());
            return;
        } catch (InterruptedException e) {
            return;
        }
        manager.removeAll();

        if (CommonUtils.isEmpty(associations)) {
            manager.add(NOFKS_ACTION);
        } else {
            manager.add(FKS_TITLE_ACTION);
            manager.add(new Separator());
            ArrayList<Action> entries = new ArrayList<>(associations.size());
            for (DBSEntityAssociation association : associations) {
                DBSEntity refTable = association.getReferencedConstraint().getParentObject();
                entries.add(new Action(
                    DBUtils.getObjectFullName(refTable, DBPEvaluationContext.UI) + " (" + association.getName() + ")",
                    DBeaverIcons.getImageDescriptor(DBSEntityType.TABLE.getIcon())) {
                    @Override
                    public void run() {
                        new AbstractJob("Navigate association") {
                            @Override
                            protected IStatus run(DBRProgressMonitor monitor) {
                                try {
                                    viewer.navigateAssociation(new VoidProgressMonitor(), viewer.getModel(), association, rows, openInNewWindow);
                                } catch (DBException e) {
                                    return GeneralUtils.makeExceptionStatus(e);
                                }
                                return Status.OK_STATUS;
                            }
                        }.schedule();
                    }
                });
            }
            entries.sort(Comparator.comparing(Action::getText));
            entries.forEach(manager::add);
        }

        manager.add(new Separator());

        if (CommonUtils.isEmpty(references)) {
            manager.add(NOREFS_ACTION);
        } else {
            manager.add(REFS_TITLE_ACTION);
            manager.add(new Separator());
            ArrayList<Action> entries = new ArrayList<>(references.size());
            for (DBSEntityAssociation refAssociation : references) {
                DBSEntity refTable = refAssociation.getParentObject();
                entries.add(new Action(
                    DBUtils.getObjectFullName(refTable, DBPEvaluationContext.UI) + " (" + refAssociation.getName() + ")",
                    DBeaverIcons.getImageDescriptor(DBSEntityType.TABLE.getIcon())) {
                    @Override
                    public void run() {
                        new AbstractJob("Navigate reference") {
                            @Override
                            protected IStatus run(DBRProgressMonitor monitor) {
                                try {
                                    viewer.navigateReference(
                                        new VoidProgressMonitor(),
                                        viewer.getModel(),
                                        refAssociation,
                                        rows,
                                        openInNewWindow);
                                } catch (DBException e) {
                                    return GeneralUtils.makeExceptionStatus(e);
                                }
                                return Status.OK_STATUS;
                            }
                        }.schedule();
                    }
                });
            }
            entries.sort(Comparator.comparing(Action::getText));
            entries.forEach(manager::add);
        }

    }

}
