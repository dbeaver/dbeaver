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
package org.jkiss.dbeaver.ext.cubrid.model.meta;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cubrid.model.CubridSQLDialect;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.dbeaver.model.sql.registry.SQLDialectRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CubridMetaModelDescriptor extends AbstractDescriptor {

    private IConfigurationElement contributorConfig;
    private ObjectType implType;
    private CubridMetaModel instance;

    private String id;
    private final Map<String, CubridMetaObject> objects = new HashMap<>();
    private String[] driverClass;
    private final String dialectId;
    private List<String> modelReplacements;

    public CubridMetaModelDescriptor() {
        super("org.jkiss.dbeaver.ext.cubrid");
        implType = new ObjectType(CubridMetaModel.class.getName());
        instance = new CubridMetaModel();
        instance.descriptor = this;
        dialectId = CubridSQLDialect.CUBRID_DIALECT_ID;
    }

    public CubridMetaModelDescriptor(IConfigurationElement cfg) {
        super(cfg);
        this.contributorConfig = cfg;

        this.id = cfg.getAttribute("id");
        IConfigurationElement[] objectList = cfg.getChildren("object");
        if (!ArrayUtils.isEmpty(objectList)) {
            for (IConfigurationElement childConfig : objectList) {
                CubridMetaObject metaObject = new CubridMetaObject(childConfig);
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
        dialectId = CommonUtils.toString(cfg.getAttribute("dialect"), CubridSQLDialect.CUBRID_DIALECT_ID);

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

    public CubridMetaObject getObject(String id)
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

    public CubridMetaModel getInstance() throws DBException {
        if (instance != null) {
            return instance;
        }
        Class<? extends CubridMetaModel> implClass = implType.getObjectClass(CubridMetaModel.class);
        if (implClass == null) {
            throw new DBException("Can't create cubrid meta model instance '" + implType.getImplName() + "'");
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
