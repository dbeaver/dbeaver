/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;


import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.editors.sql.semantics.*;
import org.jkiss.dbeaver.ui.editors.sql.semantics.context.SQLQueryDataContext;

import java.util.List;
import java.util.stream.Collectors;


public class SQLQueryRowsTableDataModel extends SQLQueryRowsSourceModel implements SQLQuerySymbolDefinition { 
    private final SQLQueryQualifiedName name;
    private DBSEntity table = null;
   
    public SQLQueryRowsTableDataModel(@NotNull SQLQueryQualifiedName name) {
        this.name = name;
    }

    public SQLQueryQualifiedName getName() {
        return this.name;
    }
    
    public DBSEntity getTable() {
        return this.table;
    }
    
    @NotNull
    @Override
    public SQLQuerySymbolClass getSymbolClass() {
        return this.table != null ? SQLQuerySymbolClass.TABLE : SQLQuerySymbolClass.ERROR; // TODO depends on connection availability
    }

    @NotNull
    private SQLQuerySymbol prepareColumnSymbol(@NotNull SQLQueryDataContext context, @NotNull DBSEntityAttribute attr) {
        SQLDialect dialect = context.getDialect();
        String name = dialect.mustBeQuoted(attr.getName(), false) 
            ? dialect.getQuotedIdentifier(attr.getName(), false, false) 
            : attr.getName().toLowerCase();
        SQLQuerySymbol symbol = new SQLQuerySymbol(name);
        symbol.setDefinition(new SQLQuerySymbolByDbObjectDefinition(attr, SQLQuerySymbolClass.COLUMN));
        return symbol;

    }

    @NotNull
    @Override
    protected SQLQueryDataContext propagateContextImpl(@NotNull SQLQueryDataContext context, @NotNull SQLQueryRecognitionContext statistics) {
        this.table = context.findRealTable(name.toListOfStrings());
                
        if (this.table != null) { 
            this.name.setDefinition(table);
            context = context.extendWithRealTable(this.table, this);
            try {
                List<? extends DBSEntityAttribute> attributes = this.table.getAttributes(new VoidProgressMonitor());
                if (attributes != null) {
                    final SQLQueryDataContext attrsContext = context;
                    List<SQLQuerySymbol> columns = attributes.stream()
                        .filter(a -> !DBUtils.isHiddenObject(a))
                        .map(a -> this.prepareColumnSymbol(attrsContext, a))
                        .collect(Collectors.toList());
                    context = context.overrideResultTuple(columns);
                }
            } catch (DBException ex) {
                statistics.appendError(this.name.entityName, "Failed to resolve table", ex);
            }
        } else {
            this.name.setSymbolClass(SQLQuerySymbolClass.ERROR);
            statistics.appendError(this.name.entityName, "Table not found");
        }
        return context;
    }
}