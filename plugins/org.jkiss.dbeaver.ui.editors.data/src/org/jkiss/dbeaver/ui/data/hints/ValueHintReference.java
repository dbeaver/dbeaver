/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.hints;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityType;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.data.DBDValueHintActionHandler;
import org.jkiss.dbeaver.ui.editors.object.struct.EditDictionaryPage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * A resultset value hint that allows navigation to a single or multiple referenced rows.
 */
record ValueHintReference(@NotNull List<Reference> references) implements DBDValueHint, DBDValueHintActionHandler {
    /**
     * A reference to a single row in a referenced table via an association.
     */
    record Reference(
        @NotNull DBDValueRow row,
        @NotNull DBSEntityAssociation association
    ) {
        @NotNull
        private String toDisplayString() {
            var entity = association.getAssociatedEntity();
            return entity != null
                ? NLS.bind("{0} ({1})", DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI), association.getName())
                : association.getName();
        }
    }

    ValueHintReference {
        if (references.isEmpty()) {
            throw new IllegalArgumentException("At least one reference must be provided");
        }

        references = List.copyOf(references);
    }

    @NotNull
    @Override
    public HintType getHintType() {
        return HintType.ACTION;
    }

    @Nullable
    @Override
    public String getHintText() {
        return null;
    }

    @NotNull
    @Override
    public String getHintDescription() {
        return "Navigate to referenced table row";
    }

    @NotNull
    @Override
    public DBPImage getHintIcon() {
        return UIIcon.LINK;
    }

    @NotNull
    @Override
    public String getActionText() {
        if (references.size() > 1) {
            return "Navigate to...";
        } else {
            return "Navigate to " + references.getFirst().toDisplayString();
        }
    }

    @Override
    public void performAction(@NotNull IResultSetController controller, @NotNull Point location, long state) {
        if (references.size() > 1) {
            showActionsMenu(controller, location, state);
        } else {
            performAction(references.getFirst(), controller, state);
        }
    }

    private static void performAction(@NotNull Reference reference, @NotNull IResultSetController controller, long state) {
        if (CommonUtils.isBitSet(state, SWT.ALT)) {
            EditDictionaryPage editDictionaryPage = new EditDictionaryPage(reference.association().getAssociatedEntity());
            if (editDictionaryPage.edit(controller.getControl().getShell())) {
                controller.refreshData(null);
            }
            return;
        }
        new AbstractJob("Navigate association") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                try {
                    controller.navigateAssociation(
                        monitor,
                        controller.getModel(),
                        reference.association(),
                        List.of(reference.row()),
                        CommonUtils.isBitSet(state, SWT.MOD1)
                    );
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private void showActionsMenu(@NotNull IResultSetController controller, @NotNull Point location, long state) {
        var manager = new MenuManager();
        manager.addMenuListener(new IMenuListener2() {
            @Override
            public void menuAboutToShow(@NotNull IMenuManager manager) {
                manager.add(new EmptyAction("Navigate to"));
                for (int i = 0; i < references.size(); i++) {
                    var reference = references.get(i);
                    var label = ActionUtils.getLabelWithIndexMnemonic(reference.toDisplayString(), i);
                    var image = DBeaverIcons.getImageDescriptor(DBSEntityType.TABLE.getIcon());
                    manager.add(new Action(label, image) {
                        @Override
                        public void run() {
                            performAction(reference, controller, state);
                        }
                    });
                }
            }

            @Override
            public void menuAboutToHide(@NotNull IMenuManager manager) {
                UIUtils.asyncExec(manager::dispose);
            }
        });

        var menu = manager.createContextMenu(controller.getControl());
        menu.setLocation(location);
        menu.setVisible(true);
    }
}