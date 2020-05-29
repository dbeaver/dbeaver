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
package org.jkiss.dbeaver.ext.generic.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericMetaModelDescriptor extends AbstractDescriptor {

    private IConfigurationElement contributorConfig;
    private ObjectType implType;
    private GenericMetaModel instance;

    private String id;
    private final Map<String, GenericMetaObject> objects = new HashMap<>();
    private String[] driverClass;
    private final String dialectId;
    private List<String> modelReplacements;

    public GenericMetaModelDescriptor() {
        super("org.jkiss.dbeaver.ext.generic");
        implType = new ObjectType(GenericMetaModel.class.getName());
        instance = new GenericMetaModel();
        instance.descriptor = this;
        dialectId = GenericSQLDialect.GENERIC_DIALECT_ID;
    }

    public GenericMetaModelDescriptor(IConfigurationElement cfg) {
        super(cfg);
        this.contributorConfig = cfg;

        this.id = cfg.getAttribute("id");
        IConfigurationElement[] objectList = cfg.getChildren("object");
        if (!ArrayUtils.isEmpty(objectList)) {
            for (IConfigurationElement childConfig : objectList) {
                GenericMetaObject metaObject = new GenericMetaObject(childConfig);
                objects.put(metaObject.getType(), metaObject);
            }
        }
        String driverClassList = cfg.getAttribute("driverClass");
        if (CommonUtils.isEmpty(driverClassList)) {
            this.driverClass = new String[0];
        } else {
            this.driverClass = driverClassList.split(",");
        }

        implType = new ObjectType(cfg.getAttribute("class"));
        dialectId = CommonUtils.toString(cfg.getAttribute("dialect"), GenericSQLDialect.GENERIC_DIALECT_ID);

        IConfigurationElement[] replaceElements = cfg.getChildren("replace");
        for (IConfigurationElement replace : replaceElements) {
            String modelId = replace.getAttribute("model");
            if (modelReplacements == null) {
                modelReplacements = new ArrayList<>();
            }
            modelReplacements.add(modelId);
        }
    }

    public String getId()
    {
        return id;
    }

    @NotNull
    public String[] getDriverClass() {
        return driverClass;
    }

    public GenericMetaObject getObject(String id)
    {
        return objects.get(id);
    }

    public SQLDialectMetadata getDialect() {
        return SQLDialectRegistry.getInstance().getDialect(dialectId);
    }

    public List<String> getModelReplacements() {
        return CommonUtils.safeList(modelReplacements);
    }

    public void setModelReplacements(List<String> modelReplacements) {
        this.modelReplacements = modelReplacements;
    }

    public GenericMetaModel getInstance() throws DBException {
        if (instance != null) {
            return instance;
        }
        Class<? extends GenericMetaModel> implClass = implType.getObjectClass(GenericMetaModel.class);
        if (implClass == null) {
            throw new DBException("Can't create generic meta model instance '" + implType.getImplName() + "'");
        }
        try {
            instance = implClass.getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            throw new DBException("Can't instantiate meta model", e);
        }
        instance.descriptor = this;
        return instance;
    }

}
