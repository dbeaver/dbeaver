/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
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

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Utils
 */
public class ResultSetUtils
{
    private static final Log log = Log.getLog(ResultSetUtils.class);

    private static final boolean BROWSE_LAZY_ASSOCIATIONS = false;

    private static volatile IDialogSettings viewerSettings;

    @NotNull
    public static IDialogSettings getViewerSettings(String section) {
        if (viewerSettings == null) {
            viewerSettings = UIUtils.getDialogSettings(ResultSetViewer.class.getSimpleName());
        }
        return UIUtils.getSettingsSection(viewerSettings, section);
    }

    public static void bindAttributes(
        @NotNull DBCSession session,
        @Nullable DBSEntity sourceEntity,
        @Nullable DBCResultSet resultSet,
        @NotNull DBDAttributeBinding[] bindings,
        @Nullable List<Object[]> rows) throws DBException
    {
        final DBRProgressMonitor monitor = session.getProgressMonitor();
        final DBPDataSource dataSource = session.getDataSource();
        boolean readMetaData = dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_READ_METADATA);
        if (!readMetaData && sourceEntity == null) {
            // Do not read metadata if source entity is not known
            return;
        }
        boolean readReferences = dataSource.getContainer().getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_READ_REFERENCES);

        final Map<DBCEntityMetaData, DBSEntity> entityBindingMap = new IdentityHashMap<>();

        monitor.beginTask("Discover resultset metadata", 3);
        try {
            SQLQuery sqlQuery = null;
            DBSEntity entity = null;
            if (sourceEntity != null) {
                entity = sourceEntity;
            } else if (resultSet != null) {
                DBCStatement sourceStatement = resultSet.getSourceStatement();
                if (sourceStatement != null && sourceStatement.getStatementSource() != null) {
                    DBCExecutionSource executionSource = sourceStatement.getStatementSource();

                    monitor.subTask("Discover owner entity");
                    DBSDataContainer dataContainer = executionSource.getDataContainer();
                    if (dataContainer instanceof DBSEntity) {
                        entity = (DBSEntity) dataContainer;
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
                            entity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), entityMeta);
                            if (entity != null) {
                                entityBindingMap.put(entityMeta, entity);
                            }
                        }
                    }
                }
            }

            final Map<DBSEntity, DBDRowIdentifier> locatorMap = new IdentityHashMap<>();

            monitor.subTask("Discover attributes");
            for (DBDAttributeBinding binding : bindings) {
                monitor.subTask("Discover attribute '" + binding.getName() + "'");
                DBCAttributeMetaData attrMeta = binding.getMetaAttribute();
                if (attrMeta == null) {
                    continue;
                }
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
                            attrEntity = DBUtils.getEntityFromMetaData(monitor, session.getExecutionContext(), attrEntityMeta);
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
                } else if (binding instanceof DBDAttributeBindingMeta){
                    DBDAttributeBindingMeta bindingMeta = (DBDAttributeBindingMeta) binding;
                    DBDPseudoAttribute pseudoAttribute = DBUtils.getPseudoAttribute(attrEntity, attrMeta.getName());
                    if (pseudoAttribute != null) {
                        bindingMeta.setPseudoAttribute(pseudoAttribute);
                    }

                    DBSEntityAttribute tableColumn;
                    if (bindingMeta.getPseudoAttribute() != null) {
                        tableColumn = bindingMeta.getPseudoAttribute().createFakeAttribute(attrEntity, attrMeta);
                    } else {
                        tableColumn = attrEntity.getAttribute(monitor, attrMeta.getName());
                    }

                    if (tableColumn != null &&
                        (sqlQuery == null || tableColumn.getTypeID() != attrMeta.getTypeID()) &&
                        bindingMeta.setEntityAttribute(tableColumn, true) &&
                        rows != null)
                    {
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

            {
                // Init row identifiers
                monitor.subTask("Detect unique identifiers");
                for (DBDAttributeBinding binding : bindings) {
                    if (!(binding instanceof DBDAttributeBindingMeta)) {
                        continue;
                    }
                    //monitor.subTask("Find attribute '" + binding.getName() + "' identifier");
                    DBSEntityAttribute attr = binding.getEntityAttribute();
                    if (attr == null) {
                        continue;
                    }
                    DBSEntity attrEntity = attr.getParentObject();
                    if (attrEntity != null) {
                        DBDRowIdentifier rowIdentifier = locatorMap.get(attrEntity);
                        if (rowIdentifier == null) {
                            DBSEntityConstraint entityIdentifier = getBestIdentifier(monitor, attrEntity, bindings, readMetaData);
                            if (entityIdentifier != null) {
                                rowIdentifier = new DBDRowIdentifier(
                                    attrEntity,
                                    entityIdentifier);
                                locatorMap.put(attrEntity, rowIdentifier);
                            }
                        }
                        ((DBDAttributeBindingMeta)binding).setRowIdentifier(rowIdentifier);
                    }
                }
                monitor.worked(1);
            }

            if (readMetaData && readReferences && rows != null) {
                monitor.subTask("Read results metadata");
                // Read nested bindings
                for (DBDAttributeBinding binding : bindings) {
                    binding.lateBinding(session, rows);
                }
            }

/*
            monitor.subTask("Load transformers");
            // Load transformers
            for (DBDAttributeBinding binding : bindings) {
                binding.loadTransformers(session, rows);
            }
*/

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

    public static DBSEntityAssociation getAssociationByAttribute(DBDAttributeBinding attr) throws DBException {
        List<DBSEntityReferrer> referrers = attr.getReferrers();
        if (referrers != null) {
            for (final DBSEntityReferrer referrer : referrers) {
                if (referrer instanceof DBSEntityAssociation) {
                    return (DBSEntityAssociation) referrer;
                }
            }
        }
        throw new DBException("Association not found in attribute [" + attr.getName() + "]");
    }

    private static DBSEntityConstraint getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBinding[] bindings, boolean readMetaData)
        throws DBException
    {
        List<DBSEntityConstraint> identifiers = new ArrayList<>(2);
        List<DBSEntityConstraint> nonIdentifyingConstraints = null;

        if (readMetaData) {
            if (table instanceof DBSTable && ((DBSTable) table).isView()) {
                // Skip physical identifiers for views. There are nothing anyway

            } else {
                // Check indexes first.
                if (table instanceof DBSTable) {
                    try {
                        Collection<? extends DBSTableIndex> indexes = ((DBSTable) table).getIndexes(monitor);
                        if (!CommonUtils.isEmpty(indexes)) {
                            // First search for primary index
                            for (DBSTableIndex index : indexes) {
                                if (index.isPrimary() && DBUtils.isIdentifierIndex(monitor, index)) {
                                    identifiers.add(index);
                                    break;
                                }
                            }
                            // Then search for unique index
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
                {
                    // Check constraints
                    Collection<? extends DBSEntityConstraint> constraints = table.getConstraints(monitor);
                    if (constraints != null) {
                        for (DBSEntityConstraint constraint : constraints) {
                            if (DBUtils.isIdentifierConstraint(monitor, constraint)) {
                                identifiers.add(constraint);
                            } else {
                                if (nonIdentifyingConstraints == null) nonIdentifyingConstraints = new ArrayList<>();
                                nonIdentifyingConstraints.add(constraint);
                            }
                        }
                    }
                }

            }
        }
        if (CommonUtils.isEmpty(identifiers)) {
            // Check for pseudo attrs (ROWID)
            // Do this after natural identifiers search (see #3829)
            for (DBDAttributeBinding column : bindings) {
                DBDPseudoAttribute pseudoAttribute = column instanceof DBDAttributeBindingMeta ? ((DBDAttributeBindingMeta) column).getPseudoAttribute() : null;
                if (pseudoAttribute != null && pseudoAttribute.getType() == DBDPseudoAttributeType.ROWID) {
                    identifiers.add(new DBDPseudoReferrer(table, column));
                    break;
                }
            }
        }

        if (CommonUtils.isEmpty(identifiers)) {
            if (nonIdentifyingConstraints != null) {
                identifiers.addAll(nonIdentifyingConstraints);
            }
        }

        if (CommonUtils.isEmpty(identifiers)) {
            // No physical identifiers or row ids
            // Make new or use existing virtual identifier
            DBVEntity virtualEntity = DBVUtils.getVirtualEntity(table, true);
            identifiers.add(virtualEntity.getBestIdentifier());
        }

        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBSEntityConstraint uniqueId = null;
            for (DBSEntityConstraint constraint : identifiers) {
                if (constraint instanceof DBSEntityReferrer) {
                    DBSEntityReferrer referrer = (DBSEntityReferrer) constraint;
                    if (isGoodReferrer(monitor, bindings, referrer)) {
                        if (referrer.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                            return referrer;
                        } else if (uniqueId == null &&
                            (referrer.getConstraintType().isUnique() ||
                                (referrer instanceof DBSTableIndex && ((DBSTableIndex) referrer).isUnique()))) {
                            uniqueId = referrer;
                        }
                    }
                } else {
                    uniqueId = constraint;
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
            attr1 != null && attr2 != null &&
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
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try (DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), attribute, "Copy from clipboard")) {
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
            controller.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_ORDER_SERVER_SIDE) &&
                (controller.isHasMoreData() || !CommonUtils.isEmpty(controller.getModel().getDataFilter().getOrder()));
    }

    public static double makeNumericValue(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof Date) {
            return ((Date) value).getTime();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        } else {
            return 0;
        }
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
        if (BROWSE_LAZY_ASSOCIATIONS && constraint instanceof DBSEntityAssociationLazy) {
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
