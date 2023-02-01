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

package org.jkiss.dbeaver.model.impl.dialects;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadata;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import javax.management.ReflectionException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Describes dialect keywords, functions and types
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

    public enum WordType {
        ATTR_DIALECTS_VALUE_KEYWORDS("keywords"), //$NON-NLS-1$
        ATTR_DIALECTS_VALUE_KEYWORDS_EXEC("execKeywords"), //$NON-NLS-1$
        ATTR_DIALECTS_VALUE_KEYWORDS_DDL("ddlKeywords"), //$NON-NLS-1$
        ATTR_DIALECTS_TXN_KEYWORDS("txnKeywords"), //$NON-NLS-1$
        ATTR_DIALECTS_VALUE_KEYWORDS_DML("dmlKeywords"), //$NON-NLS-1$
        ATTR_DIALECTS_VALUE_SCRIPT_SEPARATOR("scriptSeparator"),
        ATTR_DIALECTS_VALUE_FUNCTIONS("functions"), //$NON-NLS-1$
        ATTR_DIALECTS_VALUE_TYPES("types"); //$NON-NLS-1$

        @Nullable
        public static WordType getByTypeName(@NotNull String typename) {
            Optional<WordType> first = Arrays.stream(values())
                .filter(it -> it.getTypeName().equals(typename))
                .findFirst();
            return first.orElse(null);
        }

        private final String typeName;

        WordType(String name) {
            this.typeName = name;
        }

        public String getTypeName() {
            return typeName;
        }
    }
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

    private SQLDialectDescriptorTransformer transformer;

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
            if (propName == null) {
                log.warn("Can't find property name for" + propElement.getName());
                continue;
            }
            if (CommonUtils.isEmpty(propValue)) {
                String className = propElement.getAttribute("class");
                String fieldName = propElement.getAttribute("field");
                if (fieldName == null || className == null) {
                    continue;
                }
                Set<String> values = new HashSet<>();
                for (String field : fieldName.split(",")) {
                    Set<String> fieldValues = extractValuesFromStaticField(className, field.trim());
                    if (fieldValues == null) {
                        log.warn("Can't extract field " + field + " values");
                        continue;
                    }
                    values.addAll(fieldValues);
                }
                setValue(propName, values);
            } else {
                loadFromValue(propName, propValue);
            }
        }
    }

    private void loadFromValue(String propName, String propValue) {
        Set<String> values = loadSet(propValue);
        setValue(propName, values);
    }

    private void setValue(String propName, Set<String> values) {
        WordType typeName = WordType.getByTypeName(propName);
        if (typeName == null) {
            if ("insertMethods".equals(propName)) {
                insertMethodNames = new ArrayList<>(values);
                return;
            } else {
                if (properties == null) {
                    properties = new LinkedHashMap<>();
                }
                this.properties.put(propName, values);
                return;
            }
        }
        switch (typeName) {
            case ATTR_DIALECTS_VALUE_KEYWORDS:
                this.keywords = values;
                break;
            case ATTR_DIALECTS_VALUE_KEYWORDS_DDL:
                this.ddlKeywords = values;
                break;
            case ATTR_DIALECTS_VALUE_KEYWORDS_DML:
                this.dmlKeywords = values;
                break;
            case ATTR_DIALECTS_VALUE_KEYWORDS_EXEC:
                this.execKeywords = values;
                break;
            case ATTR_DIALECTS_TXN_KEYWORDS:
                this.txnKeywords = values;
                break;
            case ATTR_DIALECTS_VALUE_TYPES:
                this.types = values;
                break;
            case ATTR_DIALECTS_VALUE_FUNCTIONS:
                this.functions = values;
                break;
            default:
                if (properties == null) {
                    properties = new LinkedHashMap<>();
                }
                this.properties.put(propName, values);
                break;
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

    @Nullable
    public SQLDialectDescriptorTransformer getTransformer() {
        return transformer;
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
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS, keywords);
        }
        HashSet<String> nestedKeywords = new HashSet<>(keywords);
        if (parentDialect != null) {
            nestedKeywords.addAll(parentDialect.getReservedWords(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS, nestedKeywords);
    }

    @Override
    @NotNull
    public Set<String> getDataTypes(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_TYPES, types);
        }
        HashSet<String> nestedTypes = new HashSet<>(types);
        if (parentDialect != null) {
            nestedTypes.addAll(parentDialect.getDataTypes(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_TYPES, nestedTypes);

    }

    @Override
    @NotNull
    public Set<String> getFunctions(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_FUNCTIONS, functions);
        }
        HashSet<String> nestedFunctions = new HashSet<>(functions);
        if (parentDialect != null) {
            nestedFunctions.addAll(parentDialect.getFunctions(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_FUNCTIONS, nestedFunctions);

    }

    @Override
    @NotNull
    public Set<String> getDDLKeywords(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DDL, ddlKeywords);
        }
        HashSet<String> nestedDDLKeywords = new HashSet<>(ddlKeywords);
        if (parentDialect != null) {
            nestedDDLKeywords.addAll(parentDialect.getDDLKeywords(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DDL, nestedDDLKeywords);
    }

    @NotNull
    @Override
    public Set<String> getDMLKeywords(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DML, dmlKeywords);
        }
        HashSet<String> nestedDMLKeywords = new HashSet<>(dmlKeywords);
        if (parentDialect != null) {
            nestedDMLKeywords.addAll(parentDialect.getDMLKeywords(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_DML, nestedDMLKeywords);

    }

    @NotNull
    @Override
    public Set<String> getExecuteKeywords(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_EXEC, execKeywords);
        }
        HashSet<String> nestedExecuteKeywords = new HashSet<>(execKeywords);
        if (parentDialect != null) {
            nestedExecuteKeywords.addAll(parentDialect.getExecuteKeywords(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_VALUE_KEYWORDS_EXEC, nestedExecuteKeywords);
    }

    @Override
    @NotNull
    public Set<String> getTransactionKeywords(boolean nested) {
        if (!nested) {
            return getTransformedValues(WordType.ATTR_DIALECTS_TXN_KEYWORDS, txnKeywords);
        }
        HashSet<String> nestedTXNKeywords = new HashSet<>(txnKeywords);
        if (parentDialect != null) {
            nestedTXNKeywords.addAll(parentDialect.getTransactionKeywords(true));
        }
        return getTransformedValues(WordType.ATTR_DIALECTS_TXN_KEYWORDS, nestedTXNKeywords);
    }

    @Override
    @NotNull
    public String getScriptDelimiter() {
        return ";";
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

    public void setTransformer(@Nullable SQLDialectDescriptorTransformer transformer) {
        this.transformer = transformer;
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


    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return label + " (" + id + ")";
    }

    /**
     * Use reflection to extract values from the string array/list static field by field references
     * All constants should not be declared in dialect class because it will lead to the incorrect initialization
     */
    @Nullable
    private Set<String> extractValuesFromStaticField(@NotNull String classReference, @NotNull String reference) {
        try {

            Class<?> clazz = getContributorBundle().loadClass(classReference);
            Field field = clazz.getDeclaredField(reference);
            if (
                field.getType().equals(String[].class)
                || field.getType().equals(List.class)
                || field.getType().equals(Set.class)
            ) {
                Object o = field.get(reference);
                if (o instanceof String[]) {
                    return new HashSet<>(Arrays.asList((String[]) o));
                } else if (o instanceof List) {
                    return new HashSet<String>((Collection<? extends String>) o);
                } else if (o instanceof Set){
                    return (Set<String>) o;
                } else {
                    throw new ReflectionException(new ClassCastException());
                }
            } else {
                throw new NoSuchElementException("Invalid field type " + field.getType());
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ReflectionException e) {
            log.error("Invalid dialect XML entry, " + classReference + '.' + reference + " field not found!", e);
            return null;
        }
    }

    private Set<String> getTransformedValues(WordType type, Set<String> value) {
        if (transformer == null) {
            return value;
        }
        return transformer.transform(type, value);
    }

}
