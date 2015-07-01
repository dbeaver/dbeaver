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
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Utils
 */
public class ResultSetUtils
{
    static final Log log = Log.getLog(ResultSetUtils.class);

    public static void findValueLocators(
        DBCSession session,
        DBDAttributeBindingMeta[] bindings,
        List<Object[]> rows)
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.beginTask("Discover resultset metadata", 3);
        Map<DBSEntity, DBDRowIdentifier> locatorMap = new HashMap<DBSEntity, DBDRowIdentifier>();
        try {
            monitor.subTask("Discover attributes");
            for (DBDAttributeBindingMeta binding : bindings) {
                monitor.subTask("Discover attribute '" + binding.getName() + "'");
                DBCAttributeMetaData attrMeta = binding.getMetaAttribute();
                DBCEntityMetaData entityMeta = attrMeta.getEntityMetaData();
                Object metaSource = attrMeta.getSource();
                if (entityMeta == null && metaSource instanceof SQLQuery) {
                    entityMeta = ((SQLQuery)metaSource).getSingleSource();
                }
                DBSEntity entity = null;
                if (metaSource instanceof DBSEntity) {
                    entity = (DBSEntity)metaSource;
                } else if (entityMeta != null) {

                    DBPDataSource dataSource = session.getDataSource();
                    final DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
                    if (objectContainer != null) {
                        String catalogName = DBObjectNameCaseTransformer.transformName(dataSource, entityMeta.getCatalogName());
                        String schemaName = DBObjectNameCaseTransformer.transformName(dataSource, entityMeta.getSchemaName());
                        String entityName = DBObjectNameCaseTransformer.transformName(dataSource, entityMeta.getEntityName());
                        Class<? extends DBSObject> scChildType = objectContainer.getChildType(monitor);
                        DBSObject entityObject;
                        if (!CommonUtils.isEmpty(catalogName) && scChildType != null &&
                            (DBSSchema.class.isAssignableFrom(scChildType) || DBSTable.class.isAssignableFrom(scChildType)))
                        {
                            // Do not use catalog name
                            // Some data sources do not load catalog list but result set meta data contains one (e.g. DB2 and SQLite)
                            entityObject = DBUtils.getObjectByPath(monitor, objectContainer, null, schemaName, entityName);
                        } else {
                            entityObject = DBUtils.getObjectByPath(monitor, objectContainer, catalogName, schemaName, entityName);
                        }
                        if (entityObject == null) {
                            log.debug("Table '" + DBUtils.getSimpleQualifiedName(catalogName, schemaName, entityName) + "' not found in metadata catalog");
                        } else if (entityObject instanceof DBSEntity) {
                            entity = (DBSEntity) entityObject;
                        } else {
                            log.debug("Unsupported table class: " + entityObject.getClass().getName());
                        }
                    }
                }
                // We got table name and column name
                // To be editable we need this result   set contain set of columns from the same table
                // which construct any unique key
                if (entity != null) {
                    DBSEntityAttribute tableColumn;
                    if (attrMeta.getPseudoAttribute() != null) {
                        tableColumn = attrMeta.getPseudoAttribute().createFakeAttribute(entity, attrMeta);
                    } else {
                        tableColumn = entity.getAttribute(monitor, attrMeta.getName());
                    }

                    binding.setEntityAttribute(tableColumn);
                }
            }
            monitor.worked(1);

            // Init row identifiers
            monitor.subTask("Early bindings");
            for (DBDAttributeBindingMeta binding : bindings) {
                monitor.subTask("Bind attribute '" + binding.getName() + "'");
                DBSEntityAttribute attr = binding.getEntityAttribute();
                if (attr == null) {
                    continue;
                }
                DBSEntity entity = attr.getParentObject();
                DBDRowIdentifier rowIdentifier = locatorMap.get(entity);
                if (rowIdentifier == null) {
                    DBSEntityReferrer entityIdentifier = getBestIdentifier(monitor, entity, bindings);
                    if (entityIdentifier != null) {
                        rowIdentifier = new DBDRowIdentifier(
                            entity,
                            entityIdentifier);
                        locatorMap.put(entity, rowIdentifier);
                    }
                }
                binding.setRowIdentifier(rowIdentifier);
            }
            monitor.worked(1);

            monitor.subTask("Late bindings");
            // Read nested bindings
            for (DBDAttributeBinding binding : bindings) {
                monitor.subTask("Late bind attribute '" + binding.getName() + "'");
                binding.lateBinding(session, rows);
            }
            // Reload attributes in row identifiers
            for (DBDRowIdentifier rowIdentifier : locatorMap.values()) {
                rowIdentifier.reloadAttributes(monitor, bindings);
            }
        }
        catch (DBException e) {
            log.error("Can't extract column identifier info", e);
        }
        finally {
            monitor.done();
        }
    }

    private static DBSEntityReferrer getBestIdentifier(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity table, DBDAttributeBinding[] bindings)
        throws DBException
    {
        List<DBSEntityReferrer> identifiers = new ArrayList<DBSEntityReferrer>(2);
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

            // Check constraints
            Collection<? extends DBSEntityConstraint> constraints = table.getConstraints(monitor);
            if (constraints != null) {
                for (DBSEntityConstraint constraint : constraints) {
                    if (constraint instanceof DBSEntityReferrer && constraint.getConstraintType().isUnique()) {
                        identifiers.add((DBSEntityReferrer)constraint);
                    }
                }
            }
            if (identifiers.isEmpty() && table instanceof DBSTable) {
                try {
                    // Check indexes only if no unique constraints found
                    Collection<? extends DBSTableIndex> indexes = ((DBSTable)table).getIndexes(monitor);
                    if (!CommonUtils.isEmpty(indexes)) {
                        for (DBSTableIndex index : indexes) {
                            if (index.isUnique()) {
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
        }
        if (CommonUtils.isEmpty(identifiers)) {
            // No physical identifiers
            // Make new or use existing virtual identifier
            DBVEntity virtualEntity = table.getDataSource().getContainer().getVirtualModel().findEntity(table, true);
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

    public static boolean equalAttributes(DBSAttributeBase attr1, DBSAttributeBase attr2) {
        if (attr1 instanceof DBCAttributeMetaData) {
            // Check attribute metadata props
            if (attr2 instanceof DBCAttributeMetaData) {
                if (!CommonUtils.equalObjects(((DBCAttributeMetaData) attr1).getLabel(), ((DBCAttributeMetaData) attr2).getLabel())) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return
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
        DBCSession session = dataSource.getDefaultContext(false).openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.UTIL, "Copy from clipboard");
        Clipboard clipboard = new Clipboard(Display.getCurrent());
        try {
            String strValue = (String) clipboard.getContents(TextTransfer.getInstance());
            return attribute.getValueHandler().getValueFromObject(
                session, attribute.getAttribute(), strValue, true);
        } finally {
            session.close();
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
