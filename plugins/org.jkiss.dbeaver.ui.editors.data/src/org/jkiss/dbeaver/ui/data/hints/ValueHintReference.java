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
package org.jkiss.dbeaver.ui.data.hints;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValueRow;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.data.DBDValueHintActionHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Collections;

/**
 * ValueHintText
 */
public class ValueHintReference implements DBDValueHint, DBDValueHintActionHandler {

    @NotNull
    private final DBDAttributeBinding attribute;
    @NotNull
    private final DBDValueRow row;
    @NotNull
    private final DBSEntityAssociation association;

    public ValueHintReference(
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDValueRow row,
        @NotNull DBSEntityAssociation association
    ) {
        this.attribute = attribute;
        this.row = row;
        this.association = association;
    }

    @Override
    public HintType getHintType() {
        return HintType.ACTION;
    }

    @Override
    public String getHintText() {
        return null;
    }

    @Override
    public String getHintDescription() {
        return "Navigate to referenced table row";
    }

    @Override
    public DBPImage getHintIcon() {
        return UIIcon.LINK;
    }

    @Override
    public String getActionText() {
        DBSEntity associatedEntity = association.getAssociatedEntity();
        if (associatedEntity == null) {
            return null;
        }
        return "Navigate to '" + DBUtils.getObjectFullName(associatedEntity, DBPEvaluationContext.UI) + "'";
    }

    @Override
    public void performAction(@NotNull IResultSetController controller, long state) throws DBException {
        boolean newWindow;
        if (RuntimeUtils.isMacOS()) {
            newWindow = (state & SWT.COMMAND) == SWT.COMMAND;
        } else {
            newWindow = (state & SWT.CTRL) == SWT.CTRL;
        }

        new AbstractJob("Navigate association") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    controller.navigateAssociation(
                        monitor,
                        controller.getModel(),
                        association,
                        Collections.singletonList(row),
                        newWindow);
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }
}