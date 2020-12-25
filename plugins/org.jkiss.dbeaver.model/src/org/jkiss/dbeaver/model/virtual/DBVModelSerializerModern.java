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
package org.jkiss.dbeaver.model.virtual;

import com.google.gson.stream.JsonWriter;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * DBVModelSerializerLegacy
 */
class DBVModelSerializerModern implements DBVModelSerializer
{
    private static final Log log = Log.getLog(DBVModelSerializerModern.class);

    static void serializeContainer(DBRProgressMonitor monitor, JsonWriter json, DBVContainer object) throws IOException, DBException {
        if (!object.hasValuableData()) {
            // nothing to save
            return;
        }
        if (object instanceof DBVModel) {
            json.name(((DBVModel) object).getId());
        } else {
            json.name(object.getName());
        }
        json.beginObject();

        JSONUtils.serializeProperties(json, DBVContainer.CONFIG_PREFIX + "properties", object.getProperties());
        for (DBVEntity entity : object.getEntities()) {
            if (entity.hasValuableData()) {
                serializeEntity(monitor, json, entity);
            }
        }

        // Containers
        for (DBVContainer container : object.getContainers()) {
            serializeContainer(monitor, json, container);
        }

        json.endObject();
    }

    private static void serializeEntity(DBRProgressMonitor monitor, JsonWriter json, DBVEntity entity) throws IOException, DBException {

        json.name(DBVContainer.ENTITY_PREFIX + entity.getName());

        json.beginObject();
        JSONUtils.fieldNE(json, ATTR_DESCRIPTION, entity.getDescriptionColumnNames());
        JSONUtils.serializeProperties(json, "properties", entity.getProperties());

        if (!CommonUtils.isEmpty(entity.getEntityAttributes())) {
            // Attributes
            json.name("attributes");
            json.beginObject();
            for (DBVEntityAttribute attr : entity.getEntityAttributes()) {
                if (!attr.hasValuableData()) {
                    continue;
                }
                json.name(attr.getName());
                json.beginObject();

                if (attr.isCustom()) {
                    JSONUtils.field(json, "custom", true);
                    JSONUtils.fieldNE(json, "expression", attr.getExpression());
                    JSONUtils.fieldNE(json, "dataKind", attr.getDataKind().name());
                    JSONUtils.fieldNE(json, "typeName", attr.getTypeName());
                }

                final DBVTransformSettings transformSettings = attr.getTransformSettings();
                if (transformSettings != null && transformSettings.hasValuableData()) {
                    json.name("transforms");
                    json.beginObject();
                    JSONUtils.fieldNE(json, "custom", transformSettings.getCustomTransformer());
                    JSONUtils.serializeStringList(json, "include", transformSettings.getIncludedTransformers());
                    JSONUtils.serializeStringList(json, "exclude", transformSettings.getExcludedTransformers());
                    JSONUtils.serializeProperties(json, "properties", transformSettings.getTransformOptions());
                    json.endObject();
                }
                JSONUtils.serializeProperties(json, "properties", attr.getProperties());
                json.endObject();
            }
            json.endObject();
        }

        if (!CommonUtils.isEmpty(entity.getConstraints())) {
            // Constraints
            json.name("constraints");
            json.beginObject();
            for (DBVEntityConstraint c : entity.getConstraints()) {
                if (c.hasAttributes()) {
                    json.name(c.getName());
                    json.beginObject();
                    JSONUtils.field(json, "type", c.getConstraintType().getId());
                    if (c.isUseAllColumns()) {
                        JSONUtils.field(json, "useAllColumns", true);
                    } else {
                        List<DBVEntityConstraintColumn> attrRefs = c.getAttributeReferences(null);
                        if (!CommonUtils.isEmpty(attrRefs)) {
                            json.name("attributes");
                            json.beginArray();
                            for (DBVEntityConstraintColumn cc : attrRefs) {
                                json.value(cc.getAttributeName());
                            }
                            json.endArray();
                        }
                    }
                    json.endObject();
                }
            }
            json.endObject();
        }

        if (!CommonUtils.isEmpty(entity.getForeignKeys())) {
            // Foreign keys
            json.name("foreign-keys");
            json.beginArray();
            for (DBVEntityForeignKey fk : CommonUtils.safeCollection(entity.getForeignKeys())) {
                json.beginObject();
                JSONUtils.field(json, "entity", fk.getRefEntityId());
                JSONUtils.field(json, "constraint", fk.getRefConstraintId());
                List<DBVEntityForeignKeyColumn> refAttrs = fk.getAttributeReferences(null);
                if (!CommonUtils.isEmpty(refAttrs)) {
                    json.name("attributes");
                    json.beginObject();
                    for (DBVEntityForeignKeyColumn cc : refAttrs) {
                        json.name(cc.getAttributeName());
                        json.value(cc.getRefAttributeName());
                    }
                    json.endObject();
                }
                json.endObject();
            }
            json.endArray();
        }

        // Colors
        if (!CommonUtils.isEmpty(entity.getColorOverrides())) {
            json.name("colors");
            json.beginArray();
            for (DBVColorOverride color : entity.getColorOverrides()) {
                json.beginObject();
                JSONUtils.field(json, "name", color.getAttributeName());
                JSONUtils.field(json, "operator", color.getOperator().name());
                if (color.isRange()) {
                    JSONUtils.field(json, "range", true);
                }
                if (color.isSingleColumn()) {
                    JSONUtils.field(json, "single-column", true);
                }
                JSONUtils.fieldNE(json, "foreground", color.getColorForeground());
                JSONUtils.fieldNE(json, "foreground2", color.getColorForeground2());
                JSONUtils.fieldNE(json, "background", color.getColorBackground());
                JSONUtils.fieldNE(json, "background2", color.getColorBackground2());
                if (!ArrayUtils.isEmpty(color.getAttributeValues())) {
                    JSONUtils.serializeObjectList(json, "values", Arrays.asList(color.getAttributeValues()));
                }
                json.endObject();
            }
            json.endArray();
        }

        json.endObject();
    }

}
