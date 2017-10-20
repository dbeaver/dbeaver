/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

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

    public static void bindAttributes(
        DBCSession session,
        DBCResultSet resultSet,
        DBDAttributeBindingMeta[] bindings,
        List<Object[]> rows) throws DBException {
        final DBRProgressMonitor monitor = session.getProgressMonitor();
        final DBPDataSource dataSource = session.getDataSource();
        boolean readMetaData = dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_READ_METADATA);
        if (!readMetaData) {
            return;
        }
        boolean readReferences = dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_READ_REFERENCES);

        final Map<DBCEntityMetaData, DBSEntity> entityBindingMap = new IdentityHashMap<>();

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
                        entity = getEntityFromMetaData(monitor, dataSource, entityMeta);
                        if (entity != null) {
                            entityBindingMap.put(entityMeta, entity);
                        }
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
                final DBCEntityMetaData attrEntityMeta = attrMeta.getEntityMetaData();
                if (attrEntityMeta != null) {
                    attrEntity = entityBindingMap.get(attrEntityMeta);
                    if (attrEntity == null) {
                        if (entity != null && entity instanceof DBSTable && ((DBSTable) entity).isView()) {
                            // If this is a view then don't try to detect entity for each attribute
                            // MySQL returns source table name instead of view name. That's crazy.
                            attrEntity = entity;
                        } else {
                            attrEntity = getEntityFromMetaData(monitor, dataSource, attrEntityMeta);
                        }
                    }
                    if (attrEntity != null) {
                        entityBindingMap.put(attrEntityMeta, attrEntity);
                    }
                }
                if (attrEntity == null) {
                    attrEntity = entity;
                }
                if (attrEntity == null) {
                    if (attrEntityMeta != null) {
                        log.debug("Table '" + DBUtils.getSimpleQualifiedName(attrEntityMeta.getCatalogName(), attrEntityMeta.getSchemaName(), attrEntityMeta.getEntityName()) + "' not found in metadata catalog");
                    }
                } else {
                    DBDPseudoAttribute pseudoAttribute = DBUtils.getPseudoAttribute(attrEntity, attrMeta.getName());
                    if (pseudoAttribute != null) {
                        binding.setPseudoAttribute(pseudoAttribute);
                    }

                    DBSEntityAttribute tableColumn;
                    if (binding.getPseudoAttribute() != null) {
                        tableColumn = binding.getPseudoAttribute().createFakeAttribute(attrEntity, attrMeta);
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
                            binding.setEntityAttribute(tableColumn, false);
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

                    if (tableColumn != null && binding.setEntityAttribute(tableColumn, true)) {
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

            if (readReferences) {
                monitor.subTask("Late bindings");
                // Read nested bindings
                for (DBDAttributeBinding binding : bindings) {
                    binding.lateBinding(session, rows);
                }
            }
            monitor.subTask("Complete metadata load");
            // Reload attributes in row identifiers
            for (DBDRowIdentifier rowIdentifier : locatorMap.values()) {
                rowIdentifier.reloadAttributes(monitor, bindings);
            }
        }
        finally {
            monitor.done();
        }
    }

    private static DBSEntity getEntityFromMetaData(DBRProgressMonitor monitor, DBPDataSource dataSource, DBCEntityMetaData entityMeta) throws DBException {
        final DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
        if (objectContainer != null) {
            DBSEntity entity = getEntityFromMetaData(monitor, objectContainer, entityMeta, false);
            if (entity == null) {
                entity = getEntityFromMetaData(monitor, objectContainer, entityMeta, true);
            }
            return entity;
        } else {
            return null;
        }
    }
    private static DBSEntity getEntityFromMetaData(DBRProgressMonitor monitor, DBSObjectContainer objectContainer, DBCEntityMetaData entityMeta, boolean transformName) throws DBException {
        final DBPDataSource dataSource = objectContainer.getDataSource();
        String catalogName = entityMeta.getCatalogName();
        String schemaName = entityMeta.getSchemaName();
        String entityName = entityMeta.getEntityName();
        if (transformName) {
            catalogName = DBObjectNameCaseTransformer.transformName(dataSource, catalogName);
            schemaName = DBObjectNameCaseTransformer.transformName(dataSource, schemaName);
            entityName = DBObjectNameCaseTransformer.transformName(dataSource, entityName);
        }
        DBSObject entityObject = DBUtils.getObjectByPath(monitor, objectContainer, catalogName, schemaName, entityName);
        if (entityObject == null) {
            return null;
        } else if (entityObject instanceof DBSEntity) {
            return (DBSEntity) entityObject;
        } else {
            log.debug("Unsupported table class: " + entityObject.getClass().getName());
            return null;
        }
    }

    private static DBSEntityReferrer getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBindingMeta[] bindings)
        throws DBException
    {
        List<DBSEntityReferrer> identifiers = new ArrayList<>(2);
        // Check for pseudo attrs (ROWID)
        for (DBDAttributeBindingMeta column : bindings) {
            DBDPseudoAttribute pseudoAttribute = column.getPseudoAttribute();
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
            SQLUtils.compareAliases(attr1.getLabel(), attr2.getLabel()) &&
            SQLUtils.compareAliases(attr1.getName(), attr2.getName()) &&
            CommonUtils.equalObjects(attr1.getEntityMetaData(), attr2.getEntityMetaData()) &&
            attr1.getOrdinalPosition() == attr2.getOrdinalPosition() &&
            attr1.isRequired() == attr2.isRequired() &&
            attr1.getMaxLength() == attr2.getMaxLength() &&
            CommonUtils.equalObjects(attr1.getPrecision(), attr2.getPrecision()) &&
            CommonUtils.equalObjects(attr1.getScale(), attr2.getScale()) &&
            attr1.getTypeID() == attr2.getTypeID() &&
            CommonUtils.equalObjects(attr1.getTypeName(), attr2.getTypeName());
    }

    @Nullable
    public static Object getAttributeValueFromClipboard(DBDAttributeBinding attribute) throws DBCException
    {
        DBPDataSource dataSource = attribute.getDataSource();
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataSource, "Copy from clipboard")) {
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
