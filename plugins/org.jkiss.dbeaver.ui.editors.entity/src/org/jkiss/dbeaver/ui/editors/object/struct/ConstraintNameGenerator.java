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

package org.jkiss.dbeaver.ui.editors.object.struct;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ConstraintNameGenerator
 */
public class ConstraintNameGenerator {
    private static final Log log = Log.getLog(ConstraintNameGenerator.class);


    @NotNull
    private final DBSEntity entity;
    private final Map<DBSEntityConstraintType, String> TYPE_PREFIX = new HashMap<>();
    private DBSEntityConstraintType constraintType;
    private String constraintName;

    private static final Pattern NAME_INDEX_PATTERN = Pattern.compile("_([0-9]+)");

    public ConstraintNameGenerator(DBSEntity entity) {
        this(entity, null);
    }

    public ConstraintNameGenerator(DBSEntity entity, String constraintName) {
        this(entity, constraintName, DBSEntityConstraintType.PRIMARY_KEY);
    }

    public ConstraintNameGenerator(@NotNull DBSEntity entity, String constraintName, DBSEntityConstraintType constraintType) {
        this.entity = entity;
        this.constraintName = constraintName;

        addTypePrefix(DBSEntityConstraintType.PRIMARY_KEY, "_PK");
        addTypePrefix(DBSEntityConstraintType.UNIQUE_KEY, "_UNIQUE");
        addTypePrefix(DBSEntityConstraintType.VIRTUAL_KEY, "_VK");
        addTypePrefix(DBSEntityConstraintType.FOREIGN_KEY, "_FK");
        addTypePrefix(DBSEntityConstraintType.CHECK, "_CHECK");

        this.constraintType = constraintType;
        if (CommonUtils.isEmpty(constraintName)) {
            generateConstraintName(false);
        }
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public DBSEntityConstraintType getConstraintType() {
        return constraintType;
    }

    public void setConstraintType(DBSEntityConstraintType newType) {
        boolean nameUpdated = false;
        if (!CommonUtils.isEmpty(constraintName)) {
            String oldPrefix = TYPE_PREFIX.get(this.constraintType);
            if (oldPrefix != null) {
                String testName = constraintName;
                Matcher matcher = NAME_INDEX_PATTERN.matcher(testName);
                if (matcher.find() && matcher.end() == testName.length()) {
                    testName = testName.substring(0, matcher.start());
                }
                if (testName.toLowerCase().endsWith(oldPrefix.toLowerCase())) {
                    String newPrefix = TYPE_PREFIX.get(newType);
                    if (newPrefix != null) {
                        if (Character.isLowerCase(constraintName.charAt(0))) {
                            newPrefix = newPrefix.toLowerCase(Locale.ENGLISH);
                        }

                        constraintName = testName.substring(0, testName.length() - oldPrefix.length()) + newPrefix;
                        nameUpdated = true;
                    }
                }
            }
        }
        this.constraintType = newType;

        if (!nameUpdated) {
            this.generateConstraintName(true);
        } else {
            this.makeNameUnique();
        }
    }

    private void generateConstraintName(boolean forceRefresh) {
        if (CommonUtils.isEmpty(this.constraintName) || forceRefresh) {
            String namePrefix = TYPE_PREFIX.get(constraintType);
            if (namePrefix == null) {
                namePrefix = "_KEY";
            }
            String entityName = CommonUtils.escapeIdentifier(entity.getName());
            if (entityName != null && !entityName.isBlank()) {
                if (Character.isLowerCase(entityName.charAt(0))) {
                    namePrefix = namePrefix.toLowerCase(Locale.ENGLISH);
                }
            }
            this.constraintName = entityName + namePrefix;
        }
        makeNameUnique();
    }

    private void makeNameUnique() {
        try {
            int conIndex = 1;
            String curName = constraintName;
            Collection<? extends DBSEntityConstraint> conList = entity.getConstraints(new VoidProgressMonitor());
            while (DBUtils.findObject(conList, curName) != null) {
                curName = constraintName + "_" + conIndex;
                conIndex++;
            }
            constraintName = DBObjectNameCaseTransformer.transformName(entity.getDataSource(), curName);
        } catch (DBException e) {
            log.debug(e);
        }
    }

    private void addTypePrefix(DBSEntityConstraintType type, String prefix) {
        if (entity.getDataSource() != null) {
            prefix = entity.getDataSource().getSQLDialect().storesUnquotedCase().transform(prefix);
        }
        TYPE_PREFIX.put(type, prefix);
    }

    public String validateAllowedType(DBSEntityConstraintType constraintType) {
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY) {
            boolean hasPK = false;
            try {
                for (DBSEntityConstraint con : CommonUtils.safeCollection(entity.getConstraints(new VoidProgressMonitor()))) {
                    if (con.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        hasPK = true;
                        break;
                    }
                }
            } catch (DBException e) {
                log.debug(e);
            }
            if (hasPK) {
                return "Primary key already exists in '" + DBUtils.getObjectFullName(entity, DBPEvaluationContext.UI) + "'";
            }
        }
        return null;
    }

}
