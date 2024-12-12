/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.virtual;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.expressions.ExpressionNamespaceDescriptor;
import org.jkiss.dbeaver.registry.expressions.ExpressionRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Virtual model serialize
 */
public abstract class DBVUtils {

    private static final Log log = Log.getLog(DBVUtils.class);

    // Entities for unmapped attributes (custom queries, pseudo attributes, etc)
    private static final Map<String, DBVEntity> orphanVirtualEntities = new HashMap<>();

    @Nullable
    public static DBVTransformSettings getTransformSettings(@NotNull DBDAttributeBinding binding, boolean create) {
        if (DBUtils.isDynamicAttribute(binding.getAttribute()) && (binding.getParentObject() == null || binding.getParentObject().getDataKind() != DBPDataKind.DOCUMENT)) {
            return null;
        }
        DBVEntity vEntity = getVirtualEntity(binding, create);
        if (vEntity != null) {
            DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(binding, create);
            if (vAttr != null) {
                return getTransformSettings(vAttr, create);
            }
        }
        return null;
    }

    public static DBVEntity getVirtualEntity(@NotNull DBDAttributeBinding binding, boolean create) {
        DBSEntityAttribute entityAttribute = binding.getEntityAttribute();
        DBVEntity vEntity;
        if (entityAttribute != null) {
            vEntity = getVirtualEntity(entityAttribute.getParentObject(), create);
        } else {
            vEntity = getVirtualEntity(binding.getDataContainer(), create);

        }
        return vEntity;
    }

    @Nullable
    public static DBVEntity getVirtualEntity(@NotNull DBSEntity source, boolean create)
    {
        if (source instanceof DBVEntity) {
            return (DBVEntity) source;
        }
        DBPDataSource dataSource = source.getDataSource();
        return dataSource == null ? null : dataSource.getContainer().getVirtualModel().findEntity(source, create);
    }

    public static DBVEntity getVirtualEntity(@NotNull DBSDataContainer dataContainer, boolean create) {
        if (dataContainer instanceof IAdaptable) {
            // Data container can be a wrapper around another data container (e.g. ResultSetDataContainer). Virtual entity is linked to the nested one.
            DBSDataContainer nestedDC = ((IAdaptable) dataContainer).getAdapter(DBSDataContainer.class);
            if (nestedDC != null) {
                dataContainer = nestedDC;
            }
        }
        if (dataContainer instanceof DBSEntity) {
            return getVirtualEntity((DBSEntity)dataContainer, create);
        }
        // Not an entity. Most likely a custom query. Use local cache for such attributes.
        // There shouldn't be too many such settings as they are defined by user manually
        // so we shouldn't eay too much memory for that
        String attrKey = DBUtils.getObjectFullId(dataContainer);
        synchronized (orphanVirtualEntities) {
            DBPDataSource dataSource = dataContainer.getDataSource();
            if (dataSource == null) {
                return null;
            }
            DBVEntity vEntity = orphanVirtualEntities.get(attrKey);
            if (vEntity == null && create) {
                vEntity = new DBVEntity(
                    dataSource.getContainer().getVirtualModel(),
                    dataContainer.getName(),
                    "");
                orphanVirtualEntities.put(attrKey, vEntity);
            }
            return vEntity;
        }
    }

    @Nullable
    public static DBVTransformSettings getTransformSettings(@NotNull DBVEntityAttribute attribute, boolean create) {
        if (attribute.getTransformSettings() != null) {
            return attribute.getTransformSettings();
        } else if (create) {
            attribute.setTransformSettings(new DBVTransformSettings());
            return attribute.getTransformSettings();
        }
        for (DBVObject object = attribute.getParentObject(); object != null; object = object.getParentObject()) {
            if (object.getTransformSettings() != null) {
                return object.getTransformSettings();
            }
        }
        return null;
    }

    @NotNull
    public static Map<String, Object> getAttributeTransformersOptions(@NotNull DBDAttributeBinding binding) {
        if (DBUtils.isDynamicAttribute(binding.getAttribute())) {
            return Collections.emptyMap();
        }
        Map<String, Object> options = null;
        final DBVTransformSettings transformSettings = getTransformSettings(binding, false);
        if (transformSettings != null) {
            options = transformSettings.getTransformOptions();
        }
        if (options != null) {
            return options;
        }
        return Collections.emptyMap();
    }

    @Nullable
    public static DBDAttributeTransformer[] findAttributeTransformers(@NotNull DBDAttributeBinding binding, @Nullable Boolean custom)
    {
        DBPDataSource dataSource = binding.getDataSource();
        List<? extends DBDAttributeTransformerDescriptor> tdList =
            DBWorkbench.getPlatform().getValueHandlerRegistry().findTransformers(dataSource, binding.getAttribute(), custom);
        if (tdList == null || tdList.isEmpty()) {
            return null;
        }
        {
            boolean filtered = false;
            final DBVTransformSettings transformSettings = getTransformSettings(binding, false);
            if (transformSettings != null) {
                filtered = transformSettings.filterTransformers(tdList);
            }

            if (!filtered) {
                // Leave only default transformers
                for (int i = 0; i < tdList.size(); ) {
                    if (tdList.get(i).isCustom() || !tdList.get(i).isApplicableByDefault()) {
                        tdList.remove(i);
                    } else {
                        i++;
                    }
                }
            }
            if (tdList.isEmpty()) {
                return null;
            }
        }
        DBDAttributeTransformer[] result = new DBDAttributeTransformer[tdList.size()];
        for (int i = 0; i < tdList.size(); i++) {
            result[i] = tdList.get(i).getInstance();
        }
        return result;
    }

    public static String getDictionaryDescriptionColumns(DBRProgressMonitor monitor, DBSEntityAttribute attribute) throws DBException {
        DBVEntity dictionary = DBVUtils.getVirtualEntity(attribute.getParentObject(), false);
        String descColumns = null;
        if (dictionary != null) {
            descColumns = dictionary.getDescriptionColumnNames();
        }
        if (descColumns == null) {
            descColumns = DBVEntity.getDefaultDescriptionColumn(monitor, attribute);
        }
        return descColumns;
    }

    @NotNull
    public static List<DBDLabelValuePair> readDictionaryRows(
        @NotNull DBCSession session,
        @NotNull List<DBSEntityAttribute> valueAttributes,
        @NotNull List<DBDValueHandler> valueHandlers,
        @NotNull DBCResultSet dbResult,
        boolean formatValues,
        boolean containsCount) throws DBCException
    {
        List<DBDLabelValuePair> values = new ArrayList<>();
        List<? extends DBCAttributeMetaData> metaColumns = dbResult.getMeta().getAttributes();
        List<DBDValueHandler> colHandlers = new ArrayList<>(metaColumns.size());
        for (DBCAttributeMetaData col : metaColumns) {
            colHandlers.add(DBUtils.findValueHandler(session, col));
        }
        boolean hasNulls = false;

        String columnDivider = session.getDataSource().getContainer().getPreferenceStore().getString(ModelPreferences.DICTIONARY_COLUMN_DIVIDER);

        // Extract enumeration values and (optionally) their descriptions
        while (dbResult.nextRow()) {
            // Check monitor
            if (session.getProgressMonitor().isCanceled()) {
                break;
            }
            // Get value and description
            Object[] keyValues = new Object[valueAttributes.size()];
            for (int i = 0; i < valueAttributes.size(); i++) {
                Object keyValue = valueHandlers.get(i).fetchValueObject(session, dbResult, valueAttributes.get(i), i);
                if (DBUtils.isNullValue(keyValue)) {
                    if (hasNulls) {
                        continue;
                    }
                    hasNulls = true;
                }
                if (formatValues && keyValue instanceof Date) {
                    // Convert dates into string to avoid collisions
                    keyValue = valueHandlers.get(i).getValueDisplayString(valueAttributes.get(i), keyValue, DBDDisplayFormat.UI);
                }
                keyValues[i] = keyValue;
            }
            String keyLabel;
            long keyCount = 0;
            {
                StringBuilder keyLabel2 = new StringBuilder();
                for (int i = valueAttributes.size(); i < colHandlers.size(); i++) {
                    Object descValue = colHandlers.get(i).fetchValueObject(session, dbResult, metaColumns.get(i), i);
                    if (containsCount && i == colHandlers.size() - 1) {
                        // The last one column is the `count(*)`
                        keyCount = CommonUtils.toLong(descValue);
                        break;
                    }
                    if (!keyLabel2.isEmpty()) {
                        keyLabel2.append(columnDivider);
                    }
                    keyLabel2.append(colHandlers.get(i).getValueDisplayString(metaColumns.get(i), descValue, DBDDisplayFormat.NATIVE));
                }
                keyLabel = keyLabel2.toString();
            }
            // Little trick - return first key value inline
            Object finalKeyValue = keyValues.length == 1 ? keyValues[0] : keyValues;
            if (containsCount && keyCount > 0) {
                values.add(new DBDLabelValuePairExt(keyLabel, finalKeyValue, keyCount));
            } else {
                values.add(new DBDLabelValuePair(keyLabel, finalKeyValue));
            }
        }
        return values;
    }

    @NotNull
    public static List<DBVEntityAttribute> getCustomAttributes(@NotNull DBSEntity entity) {
        DBVEntity vEntity = getVirtualEntity(entity, false);
        if (vEntity != null) {
            return vEntity.getCustomAttributes();
        }
        return Collections.emptyList();
    }

    @NotNull
    public static List<DBSEntityAttribute> getAllAttributes(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity) throws DBException {
        List<DBSEntityAttribute> result = new ArrayList<>();
        final Collection<? extends DBSEntityAttribute> realAttributes = entity.getAttributes(monitor);
        if (!CommonUtils.isEmpty(realAttributes)) {
            result.addAll(realAttributes);
        }
        DBVEntity vEntity = getVirtualEntity(entity, false);
        if (vEntity != null) {
            List<DBVEntityAttribute> vAttributes = vEntity.getEntityAttributes();
            if (!CommonUtils.isEmpty(vAttributes)) {
                for (DBVEntityAttribute attr : vAttributes) {
                    if (attr.isCustom()) {
                        result.add(attr);
                    }
                }
            }
        }

        return result;
    }

    @NotNull
    public static List<DBSEntityConstraint> getAllConstraints(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity) throws DBException {
        List<DBSEntityConstraint> result = new ArrayList<>();
        final Collection<? extends DBSEntityConstraint> realConstraints = entity.getConstraints(monitor);
        if (!CommonUtils.isEmpty(realConstraints)) {
            result.addAll(realConstraints);
        }
        DBVEntity vEntity = getVirtualEntity(entity, false);
        if (vEntity != null) {
            List<DBVEntityConstraint> vConstraints = vEntity.getConstraints();
            if (!CommonUtils.isEmpty(vConstraints)) {
                result.addAll(vConstraints);
            }
        }

        return result;
    }

    @NotNull
    public static List<DBSEntityAssociation> getAllAssociations(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity)
            throws DBException {
        List<DBSEntityAssociation> result = new ArrayList<>();
        final Collection<? extends DBSEntityAssociation> realConstraints = entity.getAssociations(monitor);
        if (!CommonUtils.isEmpty(realConstraints)) {
            result.addAll(realConstraints);
        }
        if (!(entity instanceof DBVEntity)) {
            DBVEntity vEntity = getVirtualEntity(entity, false);
            if (vEntity != null) {
                List<DBVEntityForeignKey> vFKs = vEntity.getForeignKeys();
                if (!CommonUtils.isEmpty(vFKs)) {
                    result.addAll(vFKs);
                }
            }
        }
        return result;
    }

    @NotNull
    public static List<DBSEntityAssociation> getAllReferences(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity onEntity) {
        List<DBSEntityAssociation> result = new ArrayList<>();
        try {
            final Collection<? extends DBSEntityAssociation> realConstraints = onEntity.getReferences(monitor);
            if (!CommonUtils.isEmpty(realConstraints)) {
                result.addAll(realConstraints);
            }
        } catch (DBException e) {
            log.debug("Error reading entity references", e);
        }

        result.addAll(getVirtualReferences(onEntity));

        return result;
    }

    @NotNull
    public static List<DBVEntityForeignKey> getVirtualReferences(@NotNull DBSEntity onEntity) {
        DBNDatabaseNode entityNode = DBNUtils.getNodeByObject(onEntity);
        if (entityNode != null) {
            List<DBVEntityForeignKey> globalRefs = DBVModel.getGlobalReferences(entityNode);
            if (!CommonUtils.isEmpty(globalRefs)) {
                return globalRefs;
            }
        }
        return Collections.emptyList();
    }

    @NotNull
    public static DBSEntity getRealEntity(@NotNull DBRProgressMonitor monitor, @NotNull DBSEntity entity) throws DBException {
        if (entity instanceof DBVEntity) {
            DBSEntity realEntity = ((DBVEntity) entity).getRealEntity(monitor);
            if (realEntity == null) {
                throw new DBException("Can't locate real entity for " + DBUtils.getObjectFullId(entity));
            }
            return realEntity;
        }
        return entity;
    }

    @NotNull
    public static DBSEntity tryGetRealEntity(@NotNull DBSEntity entity) {
        if (entity instanceof DBVEntity) {
            try {
                return ((DBVEntity) entity).getRealEntity(new VoidProgressMonitor());
            } catch (DBException e) {
                log.error("Can't get real entity from virtual entity", e);
            }
        }
        return entity;
    }

    public static DBVObject getVirtualObject(DBSObject source, boolean create) {
        if (source == null) {
            return null;
        }
        if (source instanceof DBVObject) {
            return (DBVObject) source;
        }
        DBPDataSource dataSource = source.getDataSource();
        return dataSource == null ? null : dataSource.getContainer().getVirtualModel().findObject(source, create);
    }

    public static Object executeExpression(DBVEntityAttribute attribute, DBDAttributeBinding[] allAttributes, Object[] row) {
        String exprString = attribute.getExpression();
        if (CommonUtils.isEmpty(exprString)) {
            return null;
        }
        JexlExpression expression = attribute.getParsedExpression();
        if (expression == null) {
            return null;
        }

        return evaluateDataExpression(allAttributes, row, expression, attribute.getName());
    }

    public static Object evaluateDataExpression(DBDAttributeBinding[] allAttributes, Object[] row, JexlExpression expression, String attributeName) {
        Map<String, Object> nsList = getExpressionNamespaces();

        JexlContext context = new JexlContext() {
            @Override
            public Object get(String s) {
                Object ns = nsList.get(s);
                if (ns != null) {
                    return ns;
                }
                if (s.equals(attributeName)) {
                    return null;
                }
                for (DBDAttributeBinding attr : allAttributes) {
                    if (s.equals(attr.getLabel())) {
                        return DBUtils.getAttributeValue(attr, allAttributes, row);
                    }
                }
                return null;
            }

            @Override
            public void set(String s, Object o) {

            }

            @Override
            public boolean has(String s) {
                return get(s) != null;
            }
        };
        try {
            return expression.evaluate(context);
        } catch (Exception e) {
            return GeneralUtils.getExpressionParseMessage(e);
        }
    }

    @NotNull
    private static Map<String, Object> getExpressionNamespaces() {
        Map<String, Object> nsList = new HashMap<>();

        for (ExpressionNamespaceDescriptor ns : ExpressionRegistry.getInstance().getExpressionNamespaces()) {
            Class<?> implClass = ns.getImplClass();
            if (implClass != null) {
                nsList.put(ns.getId(), implClass);
            }
        }
        return nsList;
    }

    public static JexlExpression parseExpression(String expression) {
        Map<String, Object> nsList = getExpressionNamespaces();

        JexlBuilder jexlBuilder = new JexlBuilder();
        jexlBuilder.cache(100);
        jexlBuilder.namespaces(nsList);

        JexlEngine jexlEngine = jexlBuilder.create();
        return jexlEngine.createExpression(expression);
    }

    public static boolean isIdentifyingAttributes(@NotNull DBRProgressMonitor monitor, @NotNull List<DBSEntityAttribute> attributes) throws DBException {
        if (attributes.isEmpty()) {
            return false;
        }
        DBSEntity table = attributes.get(0).getParentObject();

        {
            // Check constraints
            Collection<? extends DBSEntityConstraint> constraints = getAllConstraints(monitor, table);
            if (constraints != null) {
                for (DBSEntityConstraint constraint : constraints) {
                    if (DBUtils.isIdentifierConstraint(monitor, constraint)) {
                        List<? extends DBSEntityAttributeRef> attrRefs = ((DBSEntityReferrer) constraint).getAttributeReferences(monitor);
                        if (attrRefs == null) {
                            continue;
                        }
                        if (attributes.size() != attrRefs.size()) {
                            continue;
                        }
                        boolean matches = true;
                        for (int i = 0; i < attributes.size(); i++) {
                            if (attributes.get(i) != attrRefs.get(i).getAttribute()) {
                                matches = false;
                                break;
                            }
                        }
                        if (matches) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
