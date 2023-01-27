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

package org.jkiss.dbeaver.model.sql;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.dialects.SQLInsertReplaceMethodDescriptor;
import org.jkiss.dbeaver.model.impl.dialects.SQLInsertReplaceMethodRegistry;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * SQLDialectDescriptor
 */
public class SQLDialectDescriptor extends AbstractContextDescriptor implements SQLDialectMetadata {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.sqlDialect"; //$NON-NLS-1$

    private static final Log log = Log.getLog(SQLDialectDescriptor.class);

    private String id;
    private String label;
    private String description;
    private ObjectType implClass;
    private DBPImage icon;
    private boolean isAbstract;
    private boolean isHidden;
    private SQLDialectDescriptor parentDialect;
    private Set<SQLDialectDescriptor> subDialects = null;

    private Map<String, Object> properties;

    private Set<String> keywords = new HashSet<>();
    private Set<String> ddlKeywords = new HashSet<>();
    private Set<String> dmlKeywords = new HashSet<>();
    private Set<String> execKeywords = new HashSet<>();
    private Set<String> txnKeywords = new HashSet<>();
    private Set<String> types = new HashSet<>();
    private Set<String> functions = new HashSet<>();
    private String scriptDelimiter;

    private List<String> insertMethodNames;
    private DBDInsertReplaceMethod[] insertReplaceMethods;
    private List<SQLInsertReplaceMethodDescriptor> insertMethodDescriptors;

    public SQLDialectDescriptor(String id) {
        super("org.jkiss.dbeaver.registry");
        this.id = id;
    }

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
                    this.keywords = loadSet(propValue);
                    break;
                case "ddlKeywords":
                    this.ddlKeywords = loadSet(propValue);
                    break;
                case "dmlKeywords":
                    this.dmlKeywords = loadSet(propValue);
                    break;
                case "execKeywords":
                    this.execKeywords = loadSet(propValue);
                    break;
                case "txnKeywords":
                    this.txnKeywords = loadSet(propValue);
                    break;
                case "types":
                    this.types = loadSet(propValue);
                    break;
                case "functions":
                    this.functions = loadSet(propValue);
                    break;
                case "insertMethods":
                    insertMethodNames = loadList(propValue);
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

    private Set<String> loadSet(String str) {
        List<String> list = loadList(str);
        return new HashSet<>(list);
    }

    @NotNull
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
    public Set<String> getReservedWords(boolean nested) {
        if (!nested) {
            return keywords;
        }
        HashSet<String> nestedKeywords = new HashSet<>(keywords);
        if (parentDialect != null) {
            nestedKeywords.addAll(parentDialect.getReservedWords(true));
        }
        return nestedKeywords;
    }

    @Override
    @NotNull
    public Set<String> getDataTypes(boolean nested) {
        if (!nested) {
            return types;
        }
        HashSet<String> nestedTypes = new HashSet<>(types);
        if (parentDialect != null) {
            nestedTypes.addAll(parentDialect.getDataTypes(true));
        }
        return nestedTypes;

    }

    @Override
    @NotNull
    public Set<String> getFunctions(boolean nested) {
        if (!nested) {
            return functions;
        }
        HashSet<String> nestedFunctions = new HashSet<>(functions);
        if (parentDialect != null) {
            nestedFunctions.addAll(parentDialect.getFunctions(true));
        }
        return nestedFunctions;

    }

    @Override
    @NotNull
    public Set<String> getDDLKeywords(boolean nested) {
        if (!nested) {
            return ddlKeywords;
        }
        HashSet<String> nestedDDLKeywords = new HashSet<>(ddlKeywords);
        if (parentDialect != null) {
            nestedDDLKeywords.addAll(parentDialect.getDDLKeywords(true));
        }
        return nestedDDLKeywords;
    }

    @NotNull
    @Override
    public Set<String> getDMLKeywords(boolean nested) {
        if (!nested) {
            return dmlKeywords;
        }
        HashSet<String> nestedDMLKeywords = new HashSet<>(dmlKeywords);
        if (parentDialect != null) {
            nestedDMLKeywords.addAll(parentDialect.getDMLKeywords(true));
        }
        return nestedDMLKeywords;

    }

    @NotNull
    @Override
    public Set<String> getExecuteKeywords(boolean nested) {
        if (!nested) {
            return execKeywords;
        }
        HashSet<String> nestedExecuteKeywords = new HashSet<>(execKeywords);
        if (parentDialect != null) {
            nestedExecuteKeywords.addAll(parentDialect.getExecuteKeywords(true));
        }
        return nestedExecuteKeywords;
    }

    @Override
    @NotNull
    public Set<String> getTransactionKeywords(boolean nested) {
        if (!nested) {
            return txnKeywords;
        }
        HashSet<String> nestedTXNKeywords = new HashSet<>(execKeywords);
        if (parentDialect != null) {
            nestedTXNKeywords.addAll(parentDialect.getTransactionKeywords(true));
        }
        return nestedTXNKeywords;

    }

    @Override
    @NotNull
    public String getScriptDelimiter() {
        return CommonUtils.isEmpty(scriptDelimiter) ? ";" : scriptDelimiter;
    }

    public ObjectType getImplClass() {
        return implClass;
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

    public void setParentDialect(SQLDialectDescriptor parentDialect) {
        this.parentDialect = parentDialect;

        Set<SQLDialectDescriptor> psd = parentDialect.subDialects;
        if (psd == null) {
            psd = new HashSet<>();
            parentDialect.subDialects = psd;
        }
        psd.add(this);
    }

    @NotNull
    @Override
    public Set<SQLDialectMetadata> getSubDialects(boolean addNested) {
        if (subDialects == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(subDialects);
    }

    @Override
    public DBDInsertReplaceMethod[] getSupportedInsertReplaceMethods() {
        getSupportedInsertReplaceMethodsDescriptors();
        if (!ArrayUtils.isEmpty(insertReplaceMethods)) {
            return insertReplaceMethods;
        }
        return new DBDInsertReplaceMethod[0];
    }

    public List<SQLInsertReplaceMethodDescriptor> getSupportedInsertReplaceMethodsDescriptors() {
        if (insertReplaceMethods == null && !CommonUtils.isEmpty(insertMethodNames)) {
            try {
                insertMethodDescriptors = new ArrayList<>();
                List<DBDInsertReplaceMethod> methodsList = new ArrayList<>();
                for (String insertMethodId : insertMethodNames) {
                    SQLInsertReplaceMethodDescriptor method = SQLInsertReplaceMethodRegistry.getInstance().getInsertMethod(insertMethodId);
                    insertMethodDescriptors.add(method);
                    methodsList.add(method.createInsertMethod());
                }
                insertReplaceMethods = methodsList.toArray(new DBDInsertReplaceMethod[0]);
            } catch (DBException e) {
                log.debug("Can't get SQL insert replace methods");
            }
        }

        return insertMethodDescriptors;
    }

    public void setScriptDelimiter(String scriptDelimiter) {
        this.scriptDelimiter = scriptDelimiter;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setKeywords(Set<String> keywords) {
        this.keywords = keywords;
    }

    public void setDdlKeywords(Set<String> ddlKeywords) {
        this.ddlKeywords = ddlKeywords;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImplClass(String implClass) {
        this.implClass = new ObjectType(implClass);
    }

    public void setIcon(DBPImage icon) {
        this.icon = icon;
    }

    public void setDmlKeywords(Set<String> dmlKeywords) {
        this.dmlKeywords = dmlKeywords;
    }

    public void setExecKeywords(Set<String> execKeywords) {
        this.execKeywords = execKeywords;
    }

    public void setTxnKeywords(Set<String> txnKeywords) {
        this.txnKeywords = txnKeywords;
    }

    public void setTypes(Set<String> types) {
        this.types = types;
    }

    public void setFunctions(Set<String> functions) {
        this.functions = functions;
    }

    @Override
    public String toString() {
        return label + " (" + id + ")";
    }

}
