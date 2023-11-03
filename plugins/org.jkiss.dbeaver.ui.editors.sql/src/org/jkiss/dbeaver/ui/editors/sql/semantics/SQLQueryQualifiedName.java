package org.jkiss.dbeaver.ui.editors.sql.semantics;

import java.util.List;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;

class SQLQueryQualifiedName { // qualifier
    public final SQLQuerySymbolEntry catalogName, schemaName, entityName;

    public SQLQueryQualifiedName(SQLQuerySymbolEntry entityName) {
        this(null, null, entityName);
    }

    public SQLQueryQualifiedName( SQLQuerySymbolEntry schemaName, SQLQuerySymbolEntry entityName) {
        this(null, schemaName, entityName);
    }

    public SQLQueryQualifiedName(SQLQuerySymbolEntry catalogName, SQLQuerySymbolEntry schemaName, SQLQuerySymbolEntry entityName) {
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.entityName = entityName;
    }
    
    public void setSymbolClass(SQLQuerySymbolClass symbolClass) {
        if (this.entityName != null) {
            this.entityName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.schemaName != null) {
            this.schemaName.getSymbol().setSymbolClass(symbolClass);
        }
        if (this.catalogName != null) {
            this.catalogName.getSymbol().setSymbolClass(symbolClass);
        }
    }

    public void setDefinition(DBSTable realTable) {
        if (this.entityName != null) {
            this.entityName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(realTable, SQLQuerySymbolClass.TABLE));
            if (this.schemaName != null) {
                DBSObject schema = realTable.getParentObject();
                if (schema != null) {
                    this.schemaName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(schema, SQLQuerySymbolClass.SCHEMA));
                } else {
                    this.schemaName.getSymbol().setSymbolClass(SQLQuerySymbolClass.SCHEMA);
                }
                if (this.catalogName != null) {
                    DBSObject catalog = realTable.getParentObject();
                    if (catalog != null) {
                        this.catalogName.setDefinition(new SQLQuerySymbolByDbObjectDefinition(catalog, SQLQuerySymbolClass.CATALOG));
                    } else {
                        this.catalogName.getSymbol().setSymbolClass(SQLQuerySymbolClass.CATALOG);
                    }
                }
            }
        }
    }

    public void setDefinition(SourceResolutionResult rr) {
        if (rr.aliasOrNull != null) {
            this.entityName.merge(rr.aliasOrNull);
        } else if (rr.tableOrNull instanceof SQLQueryTableDataModel tableModel){
            if (this.entityName != null) {
                this.entityName.setDefinition(tableModel);
            }
            if (this.schemaName != null) {
                this.entityName.setDefinition(tableModel);
            }
            if (this.catalogName != null) {
                this.entityName.setDefinition(tableModel);
            }
        }
    }
    
    public List<String> toListOfStrings() {
        if (catalogName != null) {
            return List.of(catalogName.getName(), schemaName.getName(), entityName.getName());
        } else if (schemaName != null) {
            return List.of(schemaName.getName(), entityName.getName());
        } else {
            return List.of(entityName.getName());
        }
    }
    
    @Override
    public String toString() {
        return super.toString() + "[" + String.join(".", this.toListOfStrings()) + "]";
    }
}
