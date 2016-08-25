/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLSelectItem;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.*;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Utils
 */
public class ResultSetUtils
{
    private static final Log log = Log.getLog(ResultSetUtils.class);

    public static void bindAttributes(
        DBCSession session,
        DBCResultSet resultSet,
        DBDAttributeBindingMeta[] bindings,
        List<Object[]> rows)
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.beginTask("Discover resultset metadata", 3);
        try {
            SQLQuery sqlQuery = null;
            DBSEntity entity = null;
            DBCStatement sourceStatement = resultSet.getSourceStatement();
            if (sourceStatement != null && sourceStatement.getStatementSource() != null) {
                DBCExecutionSource executionSource = sourceStatement.getStatementSource();

                monitor.subTask("Discover owner entity");
                DBSDataContainer dataContainer = executionSource.getDataContainer();
                if (dataContainer instanceof DBSEntity) {
                    entity = (DBSEntity)dataContainer;
                }
                DBCEntityMetaData entityMeta = null;
                if (entity == null) {
                    // Discover from entity metadata
                    Object sourceDescriptor = executionSource.getSourceDescriptor();
                    if (sourceDescriptor instanceof SQLQuery) {
                        sqlQuery = (SQLQuery) sourceDescriptor;
                        entityMeta = sqlQuery.getSingleSource();
                    }
                    if (entityMeta != null) {
                        entity = getEntityFromMetaData(session, entityMeta);
                    }
                }
            }

            final Map<DBSEntity, DBDRowIdentifier> locatorMap = new IdentityHashMap<>();

            monitor.subTask("Discover attributes");
            for (DBDAttributeBindingMeta binding : bindings) {
                monitor.subTask("Discover attribute '" + binding.getName() + "'");
                DBCAttributeMetaData attrMeta = binding.getMetaAttribute();
                // We got table name and column name
                // To be editable we need this resultset contain set of columns from the same table
                // which construct any unique key
                DBSEntity attrEntity = null;
                if (attrMeta.getEntityMetaData() != null) {
                    if (entity != null && entity instanceof DBSTable && ((DBSTable) entity).isView()) {
                        // If this is a view then don't try to detect entity for each attribute
                        // MySQL returns rouce table name instead of view name. That's crazy.
                        attrEntity = entity;
                    } else {
                        attrEntity = getEntityFromMetaData(session, attrMeta.getEntityMetaData());
                    }
                }
                if (attrEntity == null) {
                    attrEntity = entity;
                }
                if (attrEntity != null) {
                    DBSEntityAttribute tableColumn;
                    if (attrMeta.getPseudoAttribute() != null) {
                        tableColumn = attrMeta.getPseudoAttribute().createFakeAttribute(attrEntity, attrMeta);
                    } else {
                        tableColumn = attrEntity.getAttribute(monitor, attrMeta.getName());
                    }
                    if (sqlQuery != null) {
                        if (tableColumn != null && tableColumn.getTypeID() != attrMeta.getTypeID()) {
                            // !! Do not try to use table column handlers for custom queries if source data type
                            // differs from table data type.
                            // Query may have expressions with the same alias as underlying table column
                            // and this expression may return very different data type. It breaks fetch completely.
                            // There should be a better solution but for now let's just disable this too smart feature.
                            continue;
                        }
/*
                        final SQLSelectItem selectItem = sqlQuery.getSelectItem(attrMeta.getName());
                        if (selectItem != null && !selectItem.isPlainColumn()) {
                            // It is not a column.
                            // It maybe an expression, function or anything else
                            continue;
                        }
*/
                    }

                    if (tableColumn != null && binding.setEntityAttribute(tableColumn)) {
                        // We have new type and new value handler.
                        // We have to fix already fetched values.
                        // E.g. we fetched strings and found out that we should handle them as LOBs or enums.
                        try {
                            int pos = attrMeta.getOrdinalPosition();
                            for (Object[] row : rows) {
                                row[pos] = binding.getValueHandler().getValueFromObject(session, tableColumn, row[pos], false);
                            }
                        } catch (DBCException e) {
                            log.warn("Error resolving attribute '" + binding.getName() + "' values", e);
                        }
                    }
                }
            }
            monitor.worked(1);

            // Init row identifiers
            monitor.subTask("Detect unique identifiers");
            for (DBDAttributeBindingMeta binding : bindings) {
                //monitor.subTask("Find attribute '" + binding.getName() + "' identifier");
                DBSEntityAttribute attr = binding.getEntityAttribute();
                if (attr == null) {
                    continue;
                }
                DBSEntity attrEntity = attr.getParentObject();
                if (attrEntity != null) {
                    DBDRowIdentifier rowIdentifier = locatorMap.get(attrEntity);
                    if (rowIdentifier == null) {
                        DBSEntityReferrer entityIdentifier = getBestIdentifier(monitor, attrEntity, bindings);
                        if (entityIdentifier != null) {
                            rowIdentifier = new DBDRowIdentifier(
                                attrEntity,
                                entityIdentifier);
                            locatorMap.put(attrEntity, rowIdentifier);
                        }
                    }
                    binding.setRowIdentifier(rowIdentifier);
                }
            }
            monitor.worked(1);

            monitor.subTask("Late bindings");
            // Read nested bindings
            for (DBDAttributeBinding binding : bindings) {
                binding.lateBinding(session, rows);
            }
            monitor.subTask("Complete metadata load");
            // Reload attributes in row identifiers
            for (DBDRowIdentifier rowIdentifier : locatorMap.values()) {
                rowIdentifier.reloadAttributes(monitor, bindings);
            }
        }
        catch (Throwable e) {
            log.error("Can't extract column identifier info", e);
        }
        finally {
            monitor.done();
        }
    }

    private static DBSEntity getEntityFromMetaData(DBCSession session, DBCEntityMetaData entityMeta) throws DBException {
        final DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, session.getDataSource());
        if (objectContainer != null) {
            DBSEntity entity = getEntityFromMetaData(session, objectContainer, entityMeta, false);
            if (entity == null) {
                entity = getEntityFromMetaData(session, objectContainer, entityMeta, true);
            }
            if (entity == null) {
                log.debug("Table '" + DBUtils.getSimpleQualifiedName(entityMeta.getCatalogName(), entityMeta.getSchemaName(), entityMeta.getEntityName()) + "' not found in metadata catalog");
            }
            return entity;
        } else {
            return null;
        }
    }
    private static DBSEntity getEntityFromMetaData(DBCSession session, DBSObjectContainer objectContainer, DBCEntityMetaData entityMeta, boolean transformName) throws DBException {
        final DBPDataSource dataSource = session.getDataSource();
        String catalogName = entityMeta.getCatalogName();
        String schemaName = entityMeta.getSchemaName();
        String entityName = entityMeta.getEntityName();
        if (transformName) {
            catalogName = DBObjectNameCaseTransformer.transformName(dataSource, catalogName);
            schemaName = DBObjectNameCaseTransformer.transformName(dataSource, schemaName);
            entityName = DBObjectNameCaseTransformer.transformName(dataSource, entityName);
        }
        Class<? extends DBSObject> scChildType = objectContainer.getChildType(session.getProgressMonitor());
        DBSObject entityObject;
        if (!CommonUtils.isEmpty(catalogName) && scChildType != null &&
            (DBSSchema.class.isAssignableFrom(scChildType) || DBSTable.class.isAssignableFrom(scChildType))) {
            // Do not use catalog name
            // Some data sources do not load catalog list but result set meta data contains one (e.g. DB2 and SQLite)
            entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, null, schemaName, entityName);
        } else {
            if (CommonUtils.isEmpty(catalogName) && !CommonUtils.isEmpty(schemaName) && scChildType != null && DBSCatalog.class.isAssignableFrom(scChildType)) {
                // No catalog name specified but metadata supports catalogs (e.g. PostgreSQL)
                DBSObject selectedObject = DBUtils.getSelectedObject(objectContainer, false);
                if (selectedObject != null && selectedObject instanceof DBSCatalog) {
                    objectContainer = (DBSCatalog)selectedObject;
                }
                // Catalog specified instead of schema. This may happen if metadata provided by SQL query parser
                // which doesn't know a difference between catalogs and schemas
                entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, schemaName, null, entityName);
                if (entityObject == null) {
                    entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, null, schemaName, entityName);
                }
            } else {
                entityObject = DBUtils.getObjectByPath(session.getProgressMonitor(), objectContainer, catalogName, schemaName, entityName);
            }
        }
        if (entityObject == null) {
            return null;
        } else if (entityObject instanceof DBSEntity) {
            return (DBSEntity) entityObject;
        } else {
            log.debug("Unsupported table class: " + entityObject.getClass().getName());
            return null;
        }
    }

    private static DBSEntityReferrer getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBinding[] bindings)
        throws DBException
    {
        List<DBSEntityReferrer> identifiers = new ArrayList<>(2);
        // Check for pseudo attrs (ROWID)
        for (DBDAttributeBinding column : bindings) {
            DBDPseudoAttribute pseudoAttribute = column.getMetaAttribute().getPseudoAttribute();
            if (pseudoAttribute != null && pseudoAttribute.getType() == DBDPseudoAttributeType.ROWID) {
                identifiers.add(new DBDPseudoReferrer(table, column));
                break;
            }
        }

        if (table instanceof DBSTable && ((DBSTable) table).isView()) {
            // Skip physical identifiers for views. There are nothing anyway

        } else if (identifiers.isEmpty()) {

            // Check indexes first.
            if (table instanceof DBSTable) {
                try {
                    Collection<? extends DBSTableIndex> indexes = ((DBSTable)table).getIndexes(monitor);
                    if (!CommonUtils.isEmpty(indexes)) {
                        for (DBSTableIndex index : indexes) {
                            if (DBUtils.isIdentifierIndex(monitor, index)) {
                                identifiers.add(index);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Indexes are not supported or not available
                    // Just skip them
                    log.debug(e);
                }
            }
            if (identifiers.isEmpty()) {
                // Check constraints
                Collection<? extends DBSEntityConstraint> constraints = table.getConstraints(monitor);
                if (constraints != null) {
                    for (DBSEntityConstraint constraint : constraints) {
                        if (DBUtils.isIdentifierConstraint(monitor, constraint)) {
                            identifiers.add((DBSEntityReferrer) constraint);
                        }
                    }
                }
            }

        }
        if (CommonUtils.isEmpty(identifiers)) {
            // No physical identifiers
            // Make new or use existing virtual identifier
            DBVEntity virtualEntity = DBVUtils.findVirtualEntity(table, true);
            identifiers.add(virtualEntity.getBestIdentifier());
        }
        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBSEntityReferrer uniqueId = null;
            for (DBSEntityReferrer referrer : identifiers) {
                if (isGoodReferrer(monitor, bindings, referrer)) {
                    if (referrer.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        return referrer;
                    } else if (referrer.getConstraintType().isUnique() ||
                        (referrer instanceof DBSTableIndex && ((DBSTableIndex) referrer).isUnique()))
                    {
                        uniqueId = referrer;
                    }
                }
            }
            return uniqueId;
        }
        return null;
    }

    private static boolean isGoodReferrer(DBRProgressMonitor monitor, DBDAttributeBinding[] bindings, DBSEntityReferrer referrer) throws DBException
    {
        if (referrer instanceof DBDPseudoReferrer) {
            return true;
        }
        Collection<? extends DBSEntityAttributeRef> references = referrer.getAttributeReferences(monitor);
        if (references == null || references.isEmpty()) {
            return referrer instanceof DBVEntityConstraint;
        }
        for (DBSEntityAttributeRef ref : references) {
            for (DBDAttributeBinding binding : bindings) {
                if (binding.matches(ref.getAttribute(), false)) {
                    return true;
                }
            }
        }
        return true;
    }

    public static boolean equalAttributes(DBCAttributeMetaData attr1, DBCAttributeMetaData attr2) {
        return
            CommonUtils.equalObjects(attr1.getLabel(), attr2.getLabel()) &&
            CommonUtils.equalObjects(attr1.getEntityMetaData(), attr2.getEntityMetaData()) &&
            attr1.getOrdinalPosition() == attr2.getOrdinalPosition() &&
            attr1.isRequired() == attr2.isRequired() &&
            attr1.getMaxLength() == attr2.getMaxLength() &&
            CommonUtils.equalObjects(attr1.getName(), attr2.getName()) &&
            attr1.getPrecision() == attr2.getPrecision() &&
            attr1.getScale() == attr2.getScale() &&
            attr1.getTypeID() == attr2.getTypeID() &&
            CommonUtils.equalObjects(attr1.getTypeName(), attr2.getTypeName());
    }

    @Nullable
    public static Object getAttributeValueFromClipboard(DBDAttributeBinding attribute) throws DBCException
    {
        DBPDataSource dataSource = attribute.getDataSource();
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try (DBCSession session = DBUtils.openUtilSession(VoidProgressMonitor.INSTANCE, dataSource, "Copy from clipboard")) {
            String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
            return attribute.getValueHandler().getValueFromObject(
                session, attribute.getAttribute(), strValue, true);
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
            controller.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE) &&
                (controller.isHasMoreData() || !CommonUtils.isEmpty(controller.getModel().getDataFilter().getOrder()));
    }
}
