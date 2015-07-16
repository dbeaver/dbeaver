/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPPropertyManager;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.formatters.DefaultDataFormatter;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeEditorHelper;
import org.jkiss.dbeaver.ui.data.editors.DateTimeInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeStandaloneEditor;

import java.util.Date;

/**
 * JDBC string value handler
 */
public class DateTimeValueManager extends BaseValueManager implements DateTimeEditorHelper {

    protected static final Log log = Log.getLog(DateTimeValueManager.class);

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final IValueController controller)
        throws DBCException
    {
        manager.add(new Action(CoreMessages.model_jdbc_set_to_current_time, DBeaverIcons.getImageDescriptor(DBIcon.TYPE_DATETIME)) {
            @Override
            public void run() {
                controller.updateValue(new Date());
            }
        });
    }

    @Override
    public void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        propertySource.addProperty(
            "Date/Time",
            "format", //$NON-NLS-1$
            "Pattern",
            getFormatter(controller, controller.getValueType()).getPattern());
    }

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.INLINE, IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
            case PANEL:
                return new DateTimeInlineEditor(controller, this);
            case EDITOR:
                return new DateTimeStandaloneEditor(controller, this);
            default:
                return null;
        }
    }

    public DBDDataFormatter getFormatter(@NotNull IValueController controller, String typeId)
    {
        try {
            return controller.getExecutionContext().getDataSource().getContainer().getDataFormatterProfile().createFormatter(typeId);
        } catch (Exception e) {
            log.error("Can't create formatter for datetime value handler", e); //$NON-NLS-1$
            return DefaultDataFormatter.INSTANCE;
        }
    }

    @Override
    @NotNull
    public DBDDataFormatter getFormatter(@NotNull IValueController controller, DBSTypedObject column)
    {
        switch (column.getTypeID()) {
            case java.sql.Types.TIME:
                return getFormatter(controller, DBDDataFormatter.TYPE_NAME_TIME);
            case java.sql.Types.DATE:
                return getFormatter(controller, DBDDataFormatter.TYPE_NAME_DATE);
            default:
                return getFormatter(controller, DBDDataFormatter.TYPE_NAME_TIMESTAMP);
        }
    }

}