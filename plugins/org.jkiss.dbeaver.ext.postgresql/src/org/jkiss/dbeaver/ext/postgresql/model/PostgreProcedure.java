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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * PostgreProcedure
 */
public class PostgreProcedure extends AbstractProcedure<PostgreDataSource, PostgreSchema> implements PostgreObject, PostgreScriptObject, PostgrePrivilegeOwner, DBPUniqueObject, DBPOverloadedObject, DBPNamedObject2, DBPRefreshableObject
{
    private static final Log log = Log.getLog(PostgreProcedure.class);

    private static final String CAT_FLAGS = "Flags";
    private static final String CAT_PROPS = "Properties";
    private static final String CAT_STATS = "Statistics";

    public static final float DEFAULT_EST_ROWS = 1000.0f;
    public static final float DEFAULT_COST = 100.0f;

    public enum ProcedureVolatile {
        i("IMMUTABLE"),
        s("STABLE"),
        v("VOLATILE");

        private final String createClause;

        ProcedureVolatile(String createClause) {
            this.createClause = createClause;
        }

        public String getCreateClause() {
            return createClause;
        }
    }

    public enum ArgumentMode {
        i(DBSProcedureParameterKind.IN),
        o(DBSProcedureParameterKind.OUT),
        b(DBSProcedureParameterKind.INOUT),
        v(DBSProcedureParameterKind.RESULTSET),
        t(DBSProcedureParameterKind.TABLE),
        u(DBSProcedureParameterKind.UNKNOWN);

        private final DBSProcedureParameterKind parameterKind;
        ArgumentMode(DBSProcedureParameterKind parameterKind) {
            this.parameterKind = parameterKind;
        }

        public DBSProcedureParameterKind getParameterKind() {
            return parameterKind;
        }
    }

    private long oid;
    private PostgreProcedureKind kind;
    private String procSrc;
    private String body;
    private long ownerId;
    private long languageId;
    private float execCost;
    private float estRows;
    private PostgreDataType varArrayType;
    private String procTransform;
    private boolean isAggregate;
    private boolean isWindow;
    private boolean isSecurityDefiner;
    private boolean leakproof;
    private boolean isStrict;
    private boolean returnsSet;
    private ProcedureVolatile procVolatile;
    private PostgreDataType returnType;
    private int[] transformTypes;
    private String[] config;
    private Object acl;

    private String overloadedName;
    private List<PostgreProcedureParameter> params = new ArrayList<>();

    public PostgreProcedure(PostgreSchema schema) {
        super(schema, false);
    }

    public PostgreProcedure(
        DBRProgressMonitor monitor,
        PostgreSchema schema,
        ResultSet dbResult)
    {
        super(schema, true);
        loadInfo(monitor, dbResult);
    }

    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult) {
        this.oid = JDBCUtils.safeGetLong(dbResult, "oid");
        setName(JDBCUtils.safeGetString(dbResult, "proname"));
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "proowner");
        this.languageId = JDBCUtils.safeGetLong(dbResult, "prolang");
        this.execCost = JDBCUtils.safeGetFloat(dbResult, "procost");
        this.estRows = JDBCUtils.safeGetFloat(dbResult, "prorows");

        Long[] allArgTypes = JDBCUtils.safeGetArray(dbResult, "proallargtypes");
        String[] argNames = JDBCUtils.safeGetArray(dbResult, "proargnames");
        if (!ArrayUtils.isEmpty(allArgTypes)) {
            String[] argModes = JDBCUtils.safeGetArray(dbResult, "proargmodes");

            for (int i = 0; i < allArgTypes.length; i++) {
                Long paramType = allArgTypes[i];
                final PostgreDataType dataType = container.getDatabase().getDataType(monitor, paramType.intValue());
                if (dataType == null) {
                    log.warn("Parameter data type [" + paramType + "] not found");
                    continue;
                }
                //String paramName = argNames == null || argNames.length < inArg
                String paramName = argNames == null || argNames.length < allArgTypes.length ? "$" + (i + 1) : argNames[i];
                ArgumentMode mode = ArgumentMode.i;
                if (argModes != null && argModes.length == allArgTypes.length) {
                    try {
                        mode = ArgumentMode.valueOf(argModes[i]);
                    } catch (IllegalArgumentException e) {
                        log.debug(e);
                    }
                }
                DBSProcedureParameterKind parameterKind = mode == null ? DBSProcedureParameterKind.IN : mode.getParameterKind();
                PostgreProcedureParameter param = new PostgreProcedureParameter(
                    this,
                    paramName,
                    dataType,
                    parameterKind,
                    i + 1);
                params.add(param);
            }

        } else {
            long[] inArgTypes = PostgreUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "proargtypes"));

            if (!ArrayUtils.isEmpty(inArgTypes)) {
                for (int i = 0; i < inArgTypes.length; i++) {
                    Long paramType = inArgTypes[i];
                    final PostgreDataType dataType = container.getDatabase().getDataType(monitor, paramType.intValue());
                    if (dataType == null) {
                        log.warn("Parameter data type [" + paramType + "] not found");
                        continue;
                    }
                    //String paramName = argNames == null || argNames.length < inArg
                    //String paramName = "$" + (i + 1);
                    String paramName = argNames == null || argNames.length < inArgTypes.length ? "$" + (i + 1) : argNames[i];
                    PostgreProcedureParameter param = new PostgreProcedureParameter(
                        this, paramName, dataType, DBSProcedureParameterKind.IN, i + 1);
                    params.add(param);
                }
            }
        }

        try {
            String argDefaultsString = JDBCUtils.safeGetString(dbResult, "arg_defaults");
            String[] argDefaults = null;
            if (!CommonUtils.isEmpty(argDefaultsString)) {
                try {
                    argDefaults = PostgreUtils.parseObjectString(argDefaultsString);
                } catch (DBCException e) {
                    log.debug("Error parsing function parameters defaults", e);
                }
            }
            if (argDefaults != null && argDefaults.length > 0) {
                // Assign defaults to last X arguments
                int paramsAssigned = 0;
                for (int i = params.size() - 1; i >= 0; i--) {
                    DBSProcedureParameterKind parameterKind = params.get(i).getParameterKind();
                    if (parameterKind == DBSProcedureParameterKind.OUT || parameterKind == DBSProcedureParameterKind.TABLE || parameterKind == DBSProcedureParameterKind.RETURN) {
                        continue;
                    }
                    params.get(i).setDefaultValue(argDefaults[argDefaults.length - 1 - paramsAssigned]);
                    paramsAssigned++;
                    if (paramsAssigned >= argDefaults.length) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing parameters defaults", e);
        }

        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false);

        {
            final long varTypeId = JDBCUtils.safeGetLong(dbResult, "provariadic");
            if (varTypeId != 0) {
                varArrayType = container.getDatabase().getDataType(monitor, varTypeId);
            }
        }
        this.procTransform = JDBCUtils.safeGetString(dbResult, "protransform");
        this.isAggregate = JDBCUtils.safeGetBoolean(dbResult, "proisagg");
        this.isWindow = JDBCUtils.safeGetBoolean(dbResult, "proiswindow");
        this.isSecurityDefiner = JDBCUtils.safeGetBoolean(dbResult, "prosecdef");
        this.leakproof = JDBCUtils.safeGetBoolean(dbResult, "proleakproof");
        this.isStrict = JDBCUtils.safeGetBoolean(dbResult, "proisstrict");
        this.returnsSet = JDBCUtils.safeGetBoolean(dbResult, "proretset");
        try {
            String provolatile = JDBCUtils.safeGetString(dbResult, "provolatile");
            this.procVolatile = provolatile == null ? null : ProcedureVolatile.valueOf(provolatile);
        } catch (IllegalArgumentException e) {
            log.debug(e);
        }
        {
            final long retTypeId = JDBCUtils.safeGetLong(dbResult, "prorettype");
            if (retTypeId != 0) {
                returnType = container.getDatabase().getDataType(monitor, retTypeId);
            }
        }
        this.procSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
        this.description = JDBCUtils.safeGetString(dbResult, "description");

        this.acl = JDBCUtils.safeGetObject(dbResult, "proacl");

        this.config = JDBCUtils.safeGetArray(dbResult, "proconfig");

        if (getDataSource().getServerType().supportsStoredProcedures()) {
            String proKind = JDBCUtils.safeGetString(dbResult, "prokind");
            kind = CommonUtils.valueOf(PostgreProcedureKind.class, proKind, PostgreProcedureKind.f);
        } else {
            if (isAggregate) {
                kind = PostgreProcedureKind.a;
            } else if (isWindow) {
                kind = PostgreProcedureKind.w;
            } else {
                kind = PostgreProcedureKind.f;
            }
        }
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return container.getDatabase();
    }

    @Property(viewable = false, order = 3)
    public PostgreProcedureKind getKind() {
        return kind;
    }

    public void setKind(PostgreProcedureKind kind) {
        this.kind = kind;
    }

    @Override
    @Property(order = 5)
    public long getObjectId() {
        return oid;
    }

    @Override
    public DBSProcedureType getProcedureType()
    {
        switch (kind) {
            case f:
            case a:
            case w:
                return DBSProcedureType.FUNCTION;
            default:
                return DBSProcedureType.PROCEDURE;
        }
    }

    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getBody()
    {
        return body;
    }

    @Override
    public List<PostgreProcedureParameter> getParameters(@Nullable DBRProgressMonitor monitor) {
        return params;
    }

    public List<PostgreProcedureParameter> getInputParameters() {
        List<PostgreProcedureParameter> result = new ArrayList<>();
        for (PostgreProcedureParameter param : params) {
            if (param.getParameterKind().isInput()) {
                result.add(param);
            }
        }
        return result;
    }

    public List<PostgreProcedureParameter> getParameters(DBSProcedureParameterKind kind) {
        List<PostgreProcedureParameter> result = new ArrayList<>();
        for (PostgreProcedureParameter param : params) {
            if (param.getParameterKind() == kind) {
                result.add(param);
            }
        }
        return result;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
    }

    @NotNull
    @Override
    public String getOverloadedName() {
        return overloadedName;
    }

    @NotNull
    @Override
    public String getUniqueName() {
        return overloadedName;
    }

    public String getSpecificName() {
        return name + "_" + getObjectId();
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException
    {
        String procDDL;
        boolean omitHeader = CommonUtils.getOption(options, OPTION_DEBUGGER_SOURCE);
        if (isPersisted() && (!getDataSource().getServerType().supportsFunctionDefRead() || omitHeader)) {
            if (procSrc == null) {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                    procSrc = JDBCUtils.queryString(session, "SELECT prosrc FROM pg_proc where oid = ?", getObjectId());
                } catch (SQLException e) {
                    throw new DBException("Error reading procedure body", e);
                }
            }
            PostgreDataType returnType = getReturnType();
            String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
            procDDL = omitHeader ? procSrc : generateFunctionDeclaration(getLanguage(monitor), returnTypeName, procSrc);
        } else {
            if (body == null) {
                if (!isPersisted()) {
                    PostgreDataType returnType = getReturnType();
                    String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
                    body = generateFunctionDeclaration(getLanguage(monitor), returnTypeName, "\n\t-- Enter function body here\n");
                } else if (oid == 0 || isAggregate) {
                    // No OID so let's use old (bad) way
                    body = this.procSrc;
                } else {
                    try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                        body = JDBCUtils.queryString(session, "SELECT pg_get_functiondef(" + getObjectId() + ")");
                    } catch (SQLException e) {
                        if (!CommonUtils.isEmpty(this.procSrc)) {
                            log.debug("Error reading procedure body", e);
                            // At least we have it
                            body = this.procSrc;
                        } else {
                            throw new DBException("Error reading procedure body", e);
                        }
                    }
                }
            }
            procDDL = body;
        }
        if (this.isPersisted() && !omitHeader) {
            procDDL += ";\n";

            if (CommonUtils.getOption(options, PostgreConstants.OPTION_DDL_SHOW_COLUMN_COMMENTS) && !CommonUtils.isEmpty(getDescription())) {
                procDDL += "\nCOMMENT ON FUNCTION " + getFullQualifiedSignature() + " IS " + SQLUtils.quoteString(this, getDescription()) + ";\n";
            }

            if (CommonUtils.getOption(options, PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS)) {
                List<DBEPersistAction> actions = new ArrayList<>();
                PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
                procDDL += "\n" + SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[0]), false);
            }
        }

        return procDDL;
    }

    protected String generateFunctionDeclaration(PostgreLanguage language, String returnTypeName, String functionBody) {
        String lineSeparator = GeneralUtils.getDefaultLineSeparator();

        StringBuilder decl = new StringBuilder();

        String functionSignature = makeOverloadedName(getSchema(), getName(), params, true, true);
        decl.append("CREATE OR REPLACE ").append(getProcedureTypeName()).append(" ")
            .append(DBUtils.getQuotedIdentifier(getContainer())).append(".")
            .append(functionSignature).append(lineSeparator);
        if (getProcedureType().hasReturnValue() && !CommonUtils.isEmpty(returnTypeName)) {
            decl.append("\tRETURNS ");
            if (isReturnsSet()) {
                // Check for TABLE parameters and construct
                List<PostgreProcedureParameter> tableParams = getParameters(DBSProcedureParameterKind.TABLE);
                if (!tableParams.isEmpty()) {
                    decl.append("TABLE (");
                    for (int i = 0; i < tableParams.size(); i++) {
                        PostgreProcedureParameter tp = tableParams.get(i);
                        if (i > 0) decl.append(", ");
                        decl.append(tp.getName()).append(" ").append(tp.getTypeName());
                    }
                    decl.append(")");
                } else {
                    decl.append("SETOF ").append(returnTypeName);
                }
            } else {
                decl.append(returnTypeName);
            }
            decl.append(lineSeparator);
        }
        if (language != null) {
            decl.append("\tLANGUAGE ").append(language).append(lineSeparator);
        }
        if (isWindow()) {
            decl.append("\tWINDOW").append(lineSeparator);
        }
        if (getProcedureType() == DBSProcedureType.FUNCTION && procVolatile != null) {
            decl.append("\t").append(procVolatile.getCreateClause()).append(lineSeparator);
        }
        if (execCost > 0 && execCost != DEFAULT_COST) {
            decl.append("\tCOST ").append(CommonUtils.niceFormatFloat(execCost)).append(lineSeparator);
        }
        if (estRows > 0 && estRows != DEFAULT_EST_ROWS) {
            decl.append("\tROWS ").append(CommonUtils.niceFormatFloat(estRows)).append(lineSeparator);
        }
        if (!ArrayUtils.isEmpty(config)) {
            for (String configLine : config) {
                int divPos = configLine.indexOf('=');
                if (divPos != -1) {
                    String paramName = configLine.substring(0, divPos);
                    String paramValue = configLine.substring(divPos + 1);
                    boolean isNumeric = true;
                    try {
                        Double.parseDouble(paramValue);
                    } catch (NumberFormatException e) {
                        isNumeric = false;
                    }
                    decl.append("\tSET ").append(paramName).append(" = ").append(isNumeric ? paramValue : "'" + paramValue + "'").append(lineSeparator);
                } else {
                    log.debug("Wrong function configuration parameter [" + configLine + "]");
                }
            }
        }
        String delimiter = "$$";// + getProcedureType().name().toLowerCase(Locale.ENGLISH) + "$";
        decl.append("AS ").append(delimiter).append("\n");
        if (!CommonUtils.isEmpty(functionBody)) {
            decl.append("\t").append(functionBody).append("\n");
        }
        decl.append(delimiter).append(lineSeparator);

        return decl.toString();
    }

    @Override
    public void setObjectDefinitionText(String sourceText)
    {
        body = sourceText;
    }

    public long getOwnerId() {
        return ownerId;
    }

    @Property(category = CAT_PROPS, order = 10)
    public PostgreRole getOwner(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        return container.getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(category = CAT_PROPS, viewable = true, order = 11)
    public PostgreLanguage getLanguage(DBRProgressMonitor monitor) throws DBException {
        return PostgreUtils.getObjectById(monitor, container.getDatabase().languageCache, container.getDatabase(), languageId);
    }

    public void setLanguage(PostgreLanguage language) {
        this.languageId = language.getObjectId();
    }

    @Property(category = CAT_PROPS, viewable = true, order = 12)
    public PostgreDataType getReturnType() {
        return returnType;
    }

    public void setReturnType(PostgreDataType returnType) {
        this.returnType = returnType;
    }

    @Property(category = CAT_PROPS, viewable = false, order = 13)
    public PostgreDataType getVarArrayType() {
        return varArrayType;
    }

    @Property(category = CAT_PROPS, viewable = false, order = 14)
    public String getProcTransform() {
        return procTransform;
    }

    @Property(category = CAT_STATS, viewable = false, order = 30)
    public float getExecCost() {
        return execCost;
    }

    @Property(category = CAT_STATS, viewable = false, order = 31)
    public float getEstRows() {
        return estRows;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 100)
    public boolean isAggregate() {
        return isAggregate;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 101)
    public boolean isWindow() {
        return isWindow;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 102)
    public boolean isSecurityDefiner() {
        return isSecurityDefiner;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 103)
    public boolean isLeakproof() {
        return leakproof;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 104)
    public boolean isStrict() {
        return isStrict;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 105)
    public boolean isReturnsSet() {
        return returnsSet;
    }

    @Property(category = CAT_FLAGS, viewable = true, order = 106)
    public ProcedureVolatile getProcVolatile() {
        return procVolatile;
    }

    public static String makeOverloadedName(PostgreSchema schema, String name, List<PostgreProcedureParameter> params, boolean quote, boolean showParamNames) {
        String selfName = (quote ? DBUtils.getQuotedIdentifier(schema.getDataSource(), name) : name);
        if (!CommonUtils.isEmpty(params)) {
            StringBuilder paramsSignature = new StringBuilder(64);
            paramsSignature.append("(");
            boolean hasParam = false;
            for (PostgreProcedureParameter param : params) {
                if (param.getParameterKind() != DBSProcedureParameterKind.IN &&
                    param.getParameterKind() != DBSProcedureParameterKind.INOUT)
                {
                    continue;
                }
                if (hasParam) paramsSignature.append(',');
                hasParam = true;
                if (showParamNames) {
                    paramsSignature.append(param.getName()).append(' ');
                }
                final PostgreDataType dataType = param.getParameterType();
                final PostgreSchema typeContainer = dataType.getParentObject();
                if (typeContainer == null ||
                    typeContainer.isPublicSchema() ||
                    typeContainer.isCatalogSchema())
                {
                    paramsSignature.append(dataType.getName());
                } else {
                    paramsSignature.append(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
                }
            }
            paramsSignature.append(")");
            return selfName + paramsSignature.toString();
        } else {
            return selfName + "()";
        }
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 200)
    public String getDescription()
    {
        return super.getDescription();
    }

    public String getFullQualifiedSignature() {
        return DBUtils.getQuotedIdentifier(getContainer()) + "." +
            makeOverloadedName(getSchema(), getName(), params, true, false);
    }

    public String getProcedureTypeName() {
        return kind.getName().toUpperCase(Locale.ENGLISH);
    }

    @Override
    public PostgreSchema getSchema() {
        return container;
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(monitor,this, acl);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getProceduresCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public String generateChangeOwnerQuery(String owner) {
        return "ALTER " + this.getProcedureTypeName() + " " + this.getFullQualifiedSignature() + " OWNER TO " + owner;
    }

    @Association
    public List<PostgreDependency> getDependencies(DBRProgressMonitor monitor) throws DBCException {
        return PostgreDependency.readDependencies(monitor, this, true);
    }

    @Override
    public String toString() {
        return overloadedName == null ? name : overloadedName;
    }
}
