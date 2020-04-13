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

import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;

import java.util.IdentityHashMap;
import java.util.Map;

public class ResultSetLabelProviderDefault implements IResultSetLabelProvider {

    private final ResultSetViewer viewer;
    private Color foregroundNull;
    private final Map<DBPDataKind, Color> dataTypesForegrounds = new IdentityHashMap<>();
    private boolean colorizeDataTypes = true;
    private Color backgroundError;

    public ResultSetLabelProviderDefault(ResultSetViewer viewer) {
        this.viewer = viewer;
    }

    @Nullable
    @Override
    public DBPImage getCellImage(DBDAttributeBinding attribute, ResultSetRow row) {
        return null;
    }

    @Nullable
    @Override
    public Color getCellForeground(DBDAttributeBinding attribute, ResultSetRow row) {
        if (row.colorInfo != null) {
            if (row.colorInfo.cellFgColors != null) {
                Color cellFG = row.colorInfo.cellFgColors[attribute.getOrdinalPosition()];
                if (cellFG != null) {
                    return cellFG;
                }
            }
            if (row.colorInfo.rowForeground != null) {
                return row.colorInfo.rowForeground;
            }
        }

        Object value = viewer.getModel().getCellValue(attribute, row);
        if (DBUtils.isNullValue(value)) {
            return foregroundNull;
        } else {
            if (colorizeDataTypes) {
                Color color = dataTypesForegrounds.get(attribute.getDataKind());
                if (color != null) {
                    return color;
                }
            }
            return null;
        }
    }

    @Nullable
    @Override
    public Color getCellBackground(DBDAttributeBinding attribute, ResultSetRow row) {
        if (row.colorInfo != null) {
            if (row.colorInfo.cellBgColors != null) {
                Color cellBG = row.colorInfo.cellBgColors[attribute.getOrdinalPosition()];
                if (cellBG != null) {
                    return cellBG;
                }
            }
            if (row.colorInfo.rowBackground != null) {
                return row.colorInfo.rowBackground;
            }
        }

        Object value = viewer.getModel().getCellValue(attribute, row);
        if (value != null && value.getClass() == DBDValueError.class) {
            return backgroundError;
        }

        return null;
    }

    protected void applyThemeSettings(ITheme currentTheme) {
        this.colorizeDataTypes = viewer.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_COLORIZE_DATA_TYPES);

        final ColorRegistry colorRegistry = currentTheme.getColorRegistry();

        this.foregroundNull = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NULL_FOREGROUND);
        this.backgroundError = colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_CELL_ERROR_BACK);

        this.dataTypesForegrounds.put(DBPDataKind.BINARY, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BINARY_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.BOOLEAN, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_BOOLEAN_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.DATETIME, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_DATETIME_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.NUMERIC, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_NUMERIC_FOREGROUND));
        this.dataTypesForegrounds.put(DBPDataKind.STRING, colorRegistry.get(ThemeConstants.COLOR_SQL_RESULT_STRING_FOREGROUND));
    }


}
