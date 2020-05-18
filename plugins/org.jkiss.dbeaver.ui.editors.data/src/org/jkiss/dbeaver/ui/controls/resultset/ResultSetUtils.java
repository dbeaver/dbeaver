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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Utils
 */
public class ResultSetUtils
{
    private static final Log log = Log.getLog(ResultSetUtils.class);

    private static volatile IDialogSettings viewerSettings;

    @NotNull
    public static IDialogSettings getViewerSettings(String section) {
        if (viewerSettings == null) {
            viewerSettings = UIUtils.getDialogSettings(ResultSetViewer.class.getSimpleName());
        }
        return UIUtils.getSettingsSection(viewerSettings, section);
    }

    @Nullable
    public static Object getAttributeValueFromClipboard(DBDAttributeBinding attribute) throws DBCException
    {
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), attribute, "Copy from clipboard")) {
            String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
            return attribute.getValueHandler().getValueFromObject(
                session, attribute.getAttribute(), strValue, true, false);
        } finally {
            clipboard.dispose();
        }
    }

    public static void copyToClipboard(String string) {
        if (string != null && string.length() > 0) {
            Clipboard clipboard = new Clipboard(Display.getCurrent());
            try {
                TextTransfer textTransfer = TextTransfer.getInstance();
                clipboard.setContents(
                    new Object[]{string},
                    new Transfer[]{textTransfer});
            } finally {
                clipboard.dispose();
            }
        }
    }

    public static boolean isServerSideFiltering(IResultSetController controller)
    {
        return
            controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE) &&
                (controller.isHasMoreData() || !CommonUtils.isEmpty(controller.getModel().getDataFilter().getOrder()));
    }

    // Use linear interpolation to make gradient color in a range
    // It is dummy but simple and fast
    public static RGB makeGradientValue(RGB c1, RGB c2, double minValue, double maxValue, double value) {
        if (value <= minValue) {
            return c1;
        }
        if (value >= maxValue) {
            return c2;
        }
        double range = maxValue - minValue;
        double p = (value - minValue) / range;

        return new RGB(
            (int)(c2.red * p + c1.red * (1 - p)),
            (int)(c2.green * p + c1.green * (1 - p)),
            (int)(c2.blue * p + c1.blue * (1 - p)));
    }

    public static DBSEntityReferrer getEnumerableConstraint(DBDAttributeBinding binding) {
        try {
            DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
            if (entityAttribute != null) {
                List<DBSEntityReferrer> refs = DBUtils.getAttributeReferrers(new VoidProgressMonitor(), entityAttribute, true);
                DBSEntityReferrer constraint = refs.isEmpty() ? null : refs.get(0);

                DBSEntity associatedEntity = getAssociatedEntity(constraint);

                if (associatedEntity instanceof DBSDictionary) {
                    final DBSDictionary dictionary = (DBSDictionary)associatedEntity;
                    if (dictionary.supportsDictionaryEnumeration()) {
                        return constraint;
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e);
        }
        return null;
    }

    public static DBSEntity getAssociatedEntity(DBSEntityConstraint constraint) {
        DBSEntity[] associatedEntity = new DBSEntity[1];
        if (DBExecUtils.BROWSE_LAZY_ASSOCIATIONS && constraint instanceof DBSEntityAssociationLazy) {
            try {
                UIUtils.runInProgressService(monitor -> {
                    try {
                        associatedEntity[0] = ((DBSEntityAssociationLazy) constraint).getAssociatedEntity(monitor);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // Ignore
            }
        } else if (constraint instanceof DBSEntityAssociation) {
            associatedEntity[0] = ((DBSEntityAssociation) constraint).getAssociatedEntity();
        }
        return associatedEntity[0];
    }

    static String formatRowCount(long rows) {
        return rows < 0 ? "0" : String.valueOf(rows);
    }
}
