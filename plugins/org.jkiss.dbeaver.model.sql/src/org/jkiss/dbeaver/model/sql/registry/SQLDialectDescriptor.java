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

package org.jkiss.dbeaver.model.sql.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * SQLDialectDescriptor
 */
public class SQLDialectDescriptor extends AbstractContextDescriptor implements SQLDialectMetadata {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlDialect"; //$NON-NLS-1$

    private final String id;
    private final String label;
    private final String description;
    private final ObjectType implClass;
    private final DBPImage icon;
    private boolean isAbstract;
    private boolean isHidden;
    private SQLDialectDescriptor parentDialect;
    private List<SQLDialectDescriptor> subDialects = null;

    private Map<String, Object> properties;

    private List<String> keywords;
    private List<String> ddlKeywords;
    private List<String> dmlKeywords;
    private List<String> execKeywords;
    private List<String> txnKeywords;
    private List<String> types;
    private List<String> functions;

    SQLDialectDescriptor(IConfigurationElement config) {
        super(config);
        this.id = config.getAttribute("id");
        this.label = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.implClass = new ObjectType(config.getAttribute("class"));
        this.icon = iconToImage(config.getAttribute("icon"));

        this.isAbstract = CommonUtils.getBoolean(config.getAttribute("abstract"));
        this.isHidden = CommonUtils.getBoolean(config.getAttribute("hidden"));

        for (IConfigurationElement propElement : config.getChildren("property")) {
            String propName = propElement.getAttribute("name");
            String propValue = propElement.getAttribute("value");
            if (propName == null || CommonUtils.isEmpty(propValue)) {
                continue;
            }
            switch (propName) {
                case "keywords":
                    this.keywords = loadList(propValue);
                    break;
                case "ddlKeywords":
                    this.ddlKeywords = loadList(propValue);
                    break;
                case "dmlKeywords":
                    this.dmlKeywords = loadList(propValue);
                    break;
                case "execKeywords":
                    this.execKeywords = loadList(propValue);
                    break;
                case "txnKeywords":
                    this.txnKeywords = loadList(propValue);
                    break;
                case "types":
                    this.types = loadList(propValue);
                    break;
                case "functions":
                    this.functions = loadList(propValue);
                    break;
                default:
                    if (properties == null) {
                        properties = new LinkedHashMap<>();
                    }
                    this.properties.put(propName, propValue);
                    break;
            }
        }

    }

    private List<String> loadList(String str) {
        List<String> list = Arrays.asList(str.split(","));
        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i).toUpperCase(Locale.ENGLISH));
        }
        return list;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getIcon() {
        return icon;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    @Override
    @NotNull
    public SQLDialect createInstance() throws DBException {
        return implClass.createInstance(SQLDialect.class);
    }

    @Override
    @NotNull
    public List<String> getReservedWords() {
        return CommonUtils.safeList(keywords);
    }

    @Override
    @NotNull
    public List<String> getDataTypes() {
        return CommonUtils.safeList(types);
    }

    @Override
    @NotNull
    public List<String> getFunctions() {
        return CommonUtils.safeList(functions);
    }

    @Override
    @NotNull
    public List<String> getDDLKeywords() {
        return CommonUtils.safeList(ddlKeywords);
    }

    @NotNull
    @Override
    public List<String> getDMLKeywords() {
        return CommonUtils.safeList(dmlKeywords);
    }

    @NotNull
    @Override
    public List<String> getExecuteKeywords() {
        return CommonUtils.safeList(execKeywords);
    }

    @Override
    @NotNull
    public List<String> getTransactionKeywords() {
        return CommonUtils.safeList(txnKeywords);
    }

    @Override
    @NotNull
    public String getScriptDelimiter() {
        return ";";
    }

    @Override
    public Object getProperty(String name) {
        return properties == null ? null : properties.get(name);
    }

    @Nullable
    @Override
    public SQLDialectMetadata getParentDialect() {
        return parentDialect;
    }

    void setParentDialect(SQLDialectDescriptor parentDialect) {
        this.parentDialect = parentDialect;

        List<SQLDialectDescriptor> psd = parentDialect.subDialects;
        if (psd == null) {
            psd = new ArrayList<>();
            parentDialect.subDialects = psd;
        }
        psd.add(this);
    }

    @NotNull
    @Override
    public List<SQLDialectMetadata> getSubDialects(boolean addNested) {
        if (subDialects == null) {
            return Collections.emptyList();
        }
        List<SQLDialectMetadata> subs = new ArrayList<>();
        for (SQLDialectDescriptor sd : subDialects) {
            if (sd.isHidden) {
                subs.addAll(sd.getSubDialects(false));
            } else {
                subs.add(sd);
            }
        }
        return subs;
    }

    @Override
    public String toString() {
        return label + " (" + id + ")";
    }
}
