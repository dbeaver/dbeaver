/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.cubrid.ui.internal.CubridMessages;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.BeanUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CubridOIDSearch {

    private JDBCSession session;

    public CubridOIDSearch(JDBCSession session) {
        this.session = session;
    }

    public void searchOID(@Nullable String oidString, @Nullable Tree resultTree) {
        try {
            Connection conn = session.getOriginal();
            ClassLoader loader = conn.getClass().getClassLoader();

            Object oidObject = loader
                .loadClass("cubrid.sql.CUBRIDOIDImpl")
                .getMethod("getNewInstance", loader.loadClass("cubrid.jdbc.driver.CUBRIDConnection"), String.class)
                .invoke(null, conn, oidString);

            String tableName = (String) BeanUtils.invokeObjectMethod(oidObject, "getTableName");

            DatabaseMetaData metaData = conn.getMetaData();
            List<String> columns = new ArrayList<>();
            try (ResultSet result = metaData.getColumns(null, null, tableName, null)) {
                while (result.next()) {
                    columns.add(result.getString("COLUMN_NAME"));
                }
            }
            String[] attrName = columns.toArray(new String[0]);

            TreeItem parentTree = new TreeItem(resultTree, SWT.NONE);
            parentTree.setText(oidString);
            Display.getDefault().asyncExec(() -> parentTree.setExpanded(true));
            TreeItem child = new TreeItem(parentTree, SWT.NONE);
            child.setText("table name: " + tableName);

            try (ResultSet result = (ResultSet) BeanUtils.invokeObjectMethod(
                oidObject, "getValues",
                new Class<?>[] {String[].class},
                new Object[] {attrName})
            ) {
                while (result.next()) {
                    for (int i = 1; i <= attrName.length; i++) {
                         String column = attrName[i - 1];
                         Object value = result.getObject(i);
                         child = new TreeItem(parentTree, SWT.NONE);
                         child.setText(column + ": " + value);
                    }
                }
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showMessageBox(
                CubridMessages.cubrid_oid_search_error_title,
                CubridMessages.cubrid_oid_search_error_message,
                true
            );
        }
    }
}
