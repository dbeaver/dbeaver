/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableForeignKey;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSView;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQLView
 */
public class MySQLView extends MySQLTableBase implements DBSView
{
    private static final Log log = Log.getLog(MySQLView.class);

    private static final Pattern ALGORITHM_PATTERN = Pattern.compile("ALGORITHM\\s*=\\s*([A-Z_]+) ");
    private static final Pattern DEFINER_PATTERN = Pattern.compile(
        "DEFINER\\s*=\\s*(`(?:[^`]|``)*`(?:@`(?:[^`]|``)*`)?|CURRENT_USER(?:\\(\\))?) ", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_SECURITY_PATTERN = Pattern.compile(
        "SQL\\s+SECURITY\\s+(DEFINER|INVOKER) ", Pattern.CASE_INSENSITIVE);

    public enum CheckOption {
        NONE(null),
        CASCADE("CASCADED"),
        LOCAL("LOCAL");
        private final String definitionName;

        CheckOption(String definitionName)
        {
            this.definitionName = definitionName;
        }

        public String getDefinitionName()
        {
            return definitionName;
        }
    }

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private String definition;
        private CheckOption checkOption;
        private boolean updatable;
        private String definer;
        public String algorithm;

        public boolean isLoaded() { return loaded; }

        //@Property(hidden = true, editable = true, updatable = true, order = -1)
        public String getDefinition() { return definition; }
        public void setDefinition(String definition) { this.definition = definition; }

        @Property(viewable = true, editable = true, updatable = true, order = 4) public CheckOption getCheckOption() { return checkOption; }
        public void setCheckOption(CheckOption checkOption) { this.checkOption = checkOption; }

        @Property(viewable = true, order = 5) public boolean isUpdatable() { return updatable; }
        public void setUpdatable(boolean updatable) { this.updatable = updatable; }
        @Property(viewable = true, order = 6) public String getDefiner() { return definer; }
        public void setDefiner(String definer) { this.definer = definer; }

        @Property(viewable = true, order = 7) public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLView> {
        @Override
        public boolean isPropertyCached(@NotNull MySQLView object, @NotNull Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public MySQLView(MySQLCatalog catalog)
    {
        super(catalog);
    }

    public MySQLView(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @NotNull
    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isView()
    {
        return true;
    }

    public AdditionalInfo getAdditionalInfo()
    {
        return additionalInfo;
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public List<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public List<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public List<? extends DBSTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Override
    public List<? extends DBSTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }


    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted() || getContainer().isSystem()) {
            additionalInfo.loaded = true;
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + MySQLConstants.COL_CHECK_OPTION + "," + MySQLConstants.COL_DEFINER + "," + MySQLConstants.COL_IS_UPDATABLE +
                    " FROM " + MySQLConstants.META_TABLE_VIEWS + " WHERE " + MySQLConstants.COL_TABLE_SCHEMA + "=? AND " + MySQLConstants.COL_TABLE_NAME + "=?")) {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        try {
                            additionalInfo.setCheckOption(CheckOption.valueOf(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CHECK_OPTION)));
                        } catch (IllegalArgumentException e) {
                            log.warn(e);
                        }
                        additionalInfo.setDefiner(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFINER));
                        additionalInfo.setUpdatable("YES".equals(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_IS_UPDATABLE)));
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL))) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String definition = JDBCUtils.safeGetString(dbResult, "Create View");
                        if (definition != null) {
                            int divPos = definition.indexOf(" VIEW `");
                            if (divPos != -1) {
                                String viewOptions = definition.substring(0, divPos);
                                additionalInfo.algorithm = parseAlgorithm(viewOptions);
                                String params = getViewOptionsClause(viewOptions);
                                String statement;
                                if (getDataSource().supportsAlterView()) {
                                    statement = isPersisted() ? "ALTER" : "CREATE";
                                } else {
                                    statement = "CREATE OR REPLACE";
                                }
                                definition = statement + " " + params + definition.substring(divPos);
                            }
                        }
                        additionalInfo.setDefinition(
                            SQLFormatUtils.formatSQL(getDataSource(), definition));

                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            additionalInfo.loaded = true;
        }
    }

    @Nullable
    private static String parseAlgorithm(@NotNull String viewOptions) {
        return parseViewOption(ALGORITHM_PATTERN, viewOptions);
    }

    @Nullable
    private static String parseViewOption(@NotNull Pattern pattern, @NotNull String viewOptions) {
        // Trailing space: the last option is directly followed by " VIEW" in SHOW CREATE VIEW output
        Matcher matcher = pattern.matcher(viewOptions + " ");
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * Returns the ALGORITHM, DEFINER and SQL SECURITY clauses of a view.
     * Without DEFINER and SQL SECURITY, saving the view resets them to the current user and DEFINER.
     *
     * @param viewOptions the part of SHOW CREATE VIEW output before " VIEW `"
     */
    @NotNull
    static String getViewOptionsClause(@NotNull String viewOptions) {
        StringBuilder clause = new StringBuilder();
        String algorithm = parseAlgorithm(viewOptions);
        if (!CommonUtils.isEmpty(algorithm)) {
            clause.append(" ALGORITHM=").append(algorithm).append(" ");
        }
        String definer = parseViewOption(DEFINER_PATTERN, viewOptions);
        if (!CommonUtils.isEmpty(definer)) {
            clause.append(" DEFINER=").append(definer).append(" ");
        }
        String sqlSecurity = parseViewOption(SQL_SECURITY_PATTERN, viewOptions);
        if (!CommonUtils.isEmpty(sqlSecurity)) {
            clause.append(" SQL SECURITY ").append(sqlSecurity).append(" ");
        }
        return clause.toString();
    }

    @NotNull
    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException
    {
        String definition = getAdditionalInfo(monitor).getDefinition();
        if (definition == null && !isPersisted()) {
            return "";
        }
        return definition;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException
    {
        getAdditionalInfo().setDefinition(sourceText);
    }

}
