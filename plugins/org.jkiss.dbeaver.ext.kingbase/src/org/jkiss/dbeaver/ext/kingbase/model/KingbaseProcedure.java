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

package org.jkiss.dbeaver.ext.kingbase.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.PostgreValueParser;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDependency;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreLanguage;
import org.jkiss.dbeaver.ext.postgresql.model.PostgrePrivilege;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedure;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureKind;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreProcedureParameter;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreRole;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KingbaseProcedure extends PostgreProcedure{
    private static final Log log = Log.getLog(KingbaseProcedure.class);
   
   
    public long propackageid;
    public String prokind;
    public String procSrc;
    public PostgreProcedureKind kind;
    
    enum TransitionModifies {
        r("READ_ONLY"),
        s("SHAREABLE"),
        w("READ_WRITE");

        private final String keyword;

        TransitionModifies(String keyword) {
            this.keyword = keyword;
        }
    }

    public String body = getBody();

    public long getPropackageid() {
        return propackageid;
    }

    public KingbaseProcedure(PostgreSchema schema) {
        super(schema);
    }

    public KingbaseProcedure(DBRProgressMonitor monitor, PostgreSchema schema, ResultSet dbResult) {
        super(schema);
        this.persisted = true;
        this.propackageid = JDBCUtils.safeGetLong(dbResult, "propackageid");
        this.procSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
        loadInfo(monitor, dbResult);
    }
    
    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult) {
        this.oid = JDBCUtils.safeGetLong(dbResult, "poid");
        setName(JDBCUtils.safeGetString(dbResult, "proname"));
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "proowner");
        this.languageId = JDBCUtils.safeGetLong(dbResult, "prolang");
        this.execCost = JDBCUtils.safeGetFloat(dbResult, "procost");
        this.estRows = JDBCUtils.safeGetFloat(dbResult, "prorows");
        Number[] allArgTypes = PostgreUtils.safeGetNumberArray(dbResult, "proallargtypes");
        String[] argNames = PostgreUtils.safeGetStringArray(dbResult, "proargnames");
        argTypesJudge(allArgTypes, dbResult, monitor, argNames);
        try {
            String argDefaultsString = JDBCUtils.safeGetString(dbResult, "arg_defaults");
            String[] argDefaults = null;
            argDefaults = argDefaultsJudge(argDefaultsString, argDefaults);
            paramsSetDefaultValue(argDefaults);
        } catch (Exception e) {
            log.error("Error parsing parameters defaults", e);
        }

        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false, false);
        final long varTypeId = JDBCUtils.safeGetLong(dbResult, "provariadic");
        varArrayType(varTypeId, monitor);
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
        
        final long retTypeId = JDBCUtils.safeGetLong(dbResult, "prorettype");
        returnTypeJudge(retTypeId, monitor);
        this.procSrc = JDBCUtils.safeGetString(dbResult, "prosrc");
        this.description = JDBCUtils.safeGetString(dbResult, "description");
        this.acl = JDBCUtils.safeGetObject(dbResult, "proacl");
        this.config = PostgreUtils.safeGetStringArray(dbResult, "proconfig");
        PostgreDataSource dataSource = getDataSource();
        procedureJudge(dataSource, dbResult);
    
    }
    
    private void argTypesJudge(Number[] allArgTypes, ResultSet dbResult, DBRProgressMonitor monitor, String[] argNames) {
        if (!ArrayUtils.isEmpty(allArgTypes)) {
            String[] argModes = PostgreUtils.safeGetStringArray(dbResult, "proargmodes");
            allArgTypesJudge(allArgTypes, monitor, argNames, argModes);

        } else {
            long[] inArgTypes = KingbaseUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "proargtypes"));
            inArgTypesJudge(inArgTypes, monitor, argNames);
        }
    }
    
    private void allArgTypesJudge(Number[] allArgTypes, DBRProgressMonitor monitor, String[] argNames, String[] argModes) {
        for (int i = 0; i < allArgTypes.length; i++) {
            final long paramType = allArgTypes[i].longValue();
            final PostgreDataType dataType = container.getDatabase().getDataType(monitor, paramType);
            if (dataType == null) {
                log.warn("Parameter data type [" + paramType + "] not found");
                continue;
            }
            String paramName = argNames == null || argNames.length < allArgTypes.length ? "$" + (i + 1) : argNames[i];
            ArgumentMode mode = ArgumentMode.i;
            if (argModes != null && argModes.length == allArgTypes.length) {
                try {
                    mode = ArgumentMode.valueOf(argModes[i]);
                } catch (IllegalArgumentException e) {
                    log.debug(e);
                }
            }
            params.add(new PostgreProcedureParameter(
                this,
                paramName,
                dataType,
                mode,
                i + 1
            ));
        }
    }
    
    private String[] argDefaultsJudge(String argDefaultsString, String[] argDefaults) {
        String[] newArgDefaults = argDefaults;
        if (!CommonUtils.isEmpty(argDefaultsString)) {
            try {
                newArgDefaults = PostgreValueParser.parseSingleObject(argDefaultsString);
            } catch (DBCException e) {
                log.debug("Error parsing function parameters defaults", e);
            }
        }
        return newArgDefaults;
    }
    
    private void paramsSetDefaultValue(String[] argDefaults) {
        if (argDefaults != null && argDefaults.length > 0) {
            // Assign defaults to last X arguments
            int paramsAssigned = 0;
            for (int i = params.size() - 1; i >= 0; i--) {
                DBSProcedureParameterKind parameterKind = params.get(i).getParameterKind();
                if (parameterKind == DBSProcedureParameterKind.OUT 
                        || parameterKind == DBSProcedureParameterKind.TABLE 
                        || parameterKind == DBSProcedureParameterKind.RETURN) {
                    continue;
                }
                String defaultValue = argDefaults[argDefaults.length - 1 - paramsAssigned];
                if (defaultValue != null) {
                    defaultValue = defaultValue.trim();
                }
                params.get(i).setDefaultValue(defaultValue);
                paramsAssigned++;
                if (paramsAssigned >= argDefaults.length) {
                    break;
                }
            }
        }
    }
    
    private void inArgTypesJudge(long[] inArgTypes, DBRProgressMonitor monitor, String[] argNames) {
        if (!ArrayUtils.isEmpty(inArgTypes)) {
            for (int i = 0; i < inArgTypes.length; i++) {
                Long paramType = inArgTypes[i];
                final PostgreDataType dataType = container.getDatabase().getDataType(monitor, paramType.intValue());
                if (dataType == null) {
                    log.warn("Parameter data type [" + paramType + "] not found");
                    continue;
                }
                String paramName = argNames == null || argNames.length < inArgTypes.length ? "$" + (i + 1) : argNames[i];
                PostgreProcedureParameter param = new PostgreProcedureParameter(
                    this, paramName, dataType, ArgumentMode.i, i + 1);
                params.add(param);
            }
        }
    }
    
    private void procedureJudge(PostgreDataSource dataSource, ResultSet dbResult) {
        if (dataSource.getServerType().supportsStoredProcedures()) {
            String proKind = JDBCUtils.safeGetString(dbResult, "prokind");
            kind = CommonUtils.valueOf(PostgreProcedureKind.class, proKind, PostgreProcedureKind.f);
            kindAJudge();
        } else {
            kindJudge(dbResult);
        }
    }
    
    private void varArrayType(long varTypeId, DBRProgressMonitor monitor) {
        if (varTypeId != 0) {
            varArrayType = container.getDatabase().getDataType(monitor, varTypeId);
        }
    }
    
    private void returnTypeJudge(long retTypeId, DBRProgressMonitor monitor) {
        if (retTypeId != 0) {
            returnType = container.getDatabase().getDataType(monitor, retTypeId);
        }
    }
    
    private void kindAJudge() {
        if (kind == PostgreProcedureKind.a) {
            isAggregate = true;
        }
    }
    
    private void kindJudge(ResultSet dbResult) {
        if (isAggregate) {
            kind = PostgreProcedureKind.a;
        } else if (isWindow) {
            kind = PostgreProcedureKind.w;
        } else {
            boolean isProcedure = false;
            try {
                isProcedure = dbResult.getBoolean("prosp");
            } catch (SQLException e) {
                log.debug("Error get procedure.", e);
            }
            if (isProcedure) {
                kind = PostgreProcedureKind.p;
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
    public DBSProcedureType getProcedureType() {
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
    public String getBody() {
        return body;
    }

    @Nullable
    @Override
    public List<PostgreProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor) {
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
    public String getFullyQualifiedName(DBPEvaluationContext context) {
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
        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false, false);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        boolean omitHeader = CommonUtils.getOption(options, OPTION_DEBUGGER_SOURCE);
        String procDDL = omitHeader ? "" : "-- DROP " + getProcedureTypeName() + " " + getFullQualifiedSignature() + ";\n\n";
        if (isPersisted() && (!getDataSource().getServerType().supportsFunctionDefRead() || omitHeader) && !isAggregate) {
            procSrcJudge(monitor);
            PostgreDataType returnType = getReturnType();
            String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
            procDDL += omitHeader ? procSrc : generateFunctionDeclaration(getLanguage(monitor), returnTypeName, procSrc);
        } else {
            bodyJudge(monitor);
            procDDL += body;
        }
        procDDL = omitHeaderJudge(omitHeader, procDDL, options, monitor);
        return procDDL;
    }
    
    private String bodyJudge(DBRProgressMonitor monitor) throws DBException {
        if (body == null) {
            if (!isPersisted()) {
                PostgreDataType returnType = getReturnType();
                String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
                body = generateFunctionDeclaration(getLanguage(monitor), returnTypeName, "\n\t-- Enter function body here\n");
            } else if (oid == 0) {
                // No OID so let's use old (bad) way
                body = this.procSrc;
            } else {
                if (isAggregate) {
                    configureAggregateQuery(monitor);
                } else {
                    try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                        body = JDBCUtils.queryString(session, "SELECT sys_get_functiondef(" + getObjectId() + ")");
                    } catch (SQLException e) {
                        throw new DBException("Error reading procedure body", e);
                    }
                }
            }
        }
        return body;
    }
    
    private String procSrcJudge(DBRProgressMonitor monitor) throws DBException {
        if (procSrc == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                procSrc = JDBCUtils.queryString(session, "SELECT prosrc FROM sys_proc where oid = ?", getObjectId());
            } catch (SQLException e) {
                throw new DBException("Error reading procedure body", e);
            }
        }
        return procSrc;
    }
    
    private String omitHeaderJudge(boolean omitHeader, String procDDL, Map<String, Object> options, 
            DBRProgressMonitor monitor) throws DBException {
        String newProcDDL = procDDL;
        if (this.isPersisted() && !omitHeader) {
            newProcDDL += ";\n";

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS) && !CommonUtils.isEmpty(getDescription())) {
                newProcDDL += "\nCOMMENT ON " + getProcedureTypeName() + " " 
                    + getFullQualifiedSignature() + " IS " + SQLUtils.quoteString(this, getDescription()) + ";\n";
            }

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                List<DBEPersistAction> actions = new ArrayList<>();
                PostgreUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
                newProcDDL += "\n" + SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[0]), false);
            }
        }
        return newProcDDL;
    }

    private void configureAggregateQuery(DBRProgressMonitor monitor) throws DBCException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read aggregate function body")) {
            String query = "SELECT (sys_identify_object('sys_proc'::regclass, aggfnoid, 0)).identity,\n" +
                "aggtransfn::regproc,\n" +
                "format_type(aggtranstype, NULL) as aggtranstype,\n" +
                "CASE aggfinalfn WHEN '-'::regproc THEN NULL ELSE aggfinalfn::text END,\n" +
                "CASE aggsortop WHEN 0 THEN NULL ELSE oprname END,\n" +
                "agginitval, " +
                "aggmtransfn, aggminvtransfn,\n" +
                "aggfinalextra, aggmfinalextra, aggserialfn, aggdeserialfn, aggmfinalfn,\n" +
                "format_type(aggmtranstype, NULL) as aggmtranstype\n" +
                ",aggfinalmodify, aggmfinalmodify " +
                "FROM sys_aggregate\n" +
                "LEFT JOIN sys_operator ON sys_operator.oid = aggsortop\n" +
                "WHERE aggfnoid = ?::regproc";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(query)) {
                dbStat.setString(1, getFullyQualifiedName(DBPEvaluationContext.DDL));
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        String fullName = JDBCUtils.safeGetString(dbResult, "identity");
                        String aggtransfn = JDBCUtils.safeGetString(dbResult, "aggtransfn"); // Transition function

                        // Data type of the aggregate function's internal transition (state) data
                        String aggtranstype = JDBCUtils.safeGetString(dbResult, "aggtranstype");
                        TransitionModifies finalmodify = null;
                        TransitionModifies mfinalmodify = null;
                        finalmodify = TransitionModifies.valueOf(
                                JDBCUtils.safeGetString(dbResult, "aggfinalmodify"));
                        mfinalmodify = TransitionModifies.valueOf(
                                JDBCUtils.safeGetString(dbResult, "aggmfinalmodify")); // For the aggmfinalfn
                        boolean finalextra = JDBCUtils.safeGetBoolean(dbResult, "aggfinalextra"); // arguments to aggfinalfn
                        

                        StringBuilder aggregateBody = new StringBuilder("CREATE OR REPLACE AGGREGATE ");
                        final String delim = ",\n\t";
                        final String notResult = "-";
                        aggregateBody.append(fullName).append(" (\n")
                            .append("\tSFUNC = ").append(aggtransfn).append(delim)
                            .append("STYPE = ").append(aggtranstype);
                        String aggfinalfn = JDBCUtils.safeGetString(dbResult, "aggfinalfn"); // Final function
                        aggfinalfnJudge(aggfinalfn, aggregateBody, finalextra, delim, finalmodify);
                        String serialfn = JDBCUtils.safeGetString(dbResult, "aggserialfn");
                        serialfnJudge(serialfn, aggregateBody, notResult, delim);
                        String deserialfn = JDBCUtils.safeGetString(dbResult, "aggdeserialfn");
                        deserialfnJudge(deserialfn, aggregateBody, notResult, delim);
                        // The initial value of the transition state
                        String initval = JDBCUtils.safeGetString(dbResult, "agginitval");
                        initvalJudge(initval, aggregateBody, delim);
                        // Forward transition function for moving-aggregate mode
                        String mtransfn = JDBCUtils.safeGetString(dbResult, "aggmtransfn");
                        String mtranstype = JDBCUtils.safeGetString(dbResult, "aggmtranstype");
                        mtransfnJudge(mtransfn, mtranstype, notResult, aggregateBody, delim);
                        // Inverse transition function for moving-aggregate mode
                        String minvtransfn = JDBCUtils.safeGetString(dbResult, "aggminvtransfn");
                        minvtransfnJudge(minvtransfn, notResult, aggregateBody, delim);
                        String mfinalfn = JDBCUtils.safeGetString(dbResult, "aggmfinalfn");
                        boolean mfinalextra = JDBCUtils.safeGetBoolean(dbResult, "aggmfinalextra"); // arguments to aggmfinalfn
                        mfinalfnJudge(mfinalfn, notResult, mfinalextra, mfinalmodify, aggregateBody, delim);
                        String oprname = JDBCUtils.safeGetString(dbResult, "oprname"); // Associated sort operator
                        oprNameJudge(oprname, aggregateBody, delim);
                        aggregateBody.append("\n)");
                        body = aggregateBody.toString();
                    }
                }
            }
        } catch (SQLException e) {
            log.debug("Error reading aggregate function body", e);
            body = "-- Aggregate function " + getFullQualifiedSignature() + "\n-- " + e.getMessage();
        }
    }
    
    private StringBuilder mfinalfnJudge(String mfinalfn, String notResult, boolean mfinalextra, 
            TransitionModifies mfinalmodify, StringBuilder aggregateBody, String delim) {
        if (CommonUtils.isNotEmpty(mfinalfn) && !notResult.equals(mfinalfn)) {
            aggregateBody.append(delim).append("MFINALFUNC = ").append(mfinalfn);
            mfinalextraJudge(mfinalextra, aggregateBody, delim);
            mfinalmodifyJudge(mfinalmodify, aggregateBody, delim);
        }
        return aggregateBody;
    }
    
    private StringBuilder mfinalextraJudge(boolean mfinalextra, StringBuilder aggregateBody, String delim) {
        if (mfinalextra) {
            aggregateBody.append(delim).append("MFINALFUNC_EXTRA");
        }
        return aggregateBody;
    }
    
    private StringBuilder mfinalmodifyJudge(TransitionModifies mfinalmodify, StringBuilder aggregateBody, String delim) {
        if (mfinalmodify != null) {
            aggregateBody.append(delim).append("MFINALFUNC_MODIFY = ").append(mfinalmodify.keyword);
        }
        return aggregateBody;
    }
    
    private StringBuilder oprNameJudge(String oprname, StringBuilder aggregateBody, String delim) {
        if (CommonUtils.isNotEmpty(oprname)) {
            aggregateBody.append(delim).append("SORTOP = ").append(oprname);
        }
        return aggregateBody;
    }
    
    private StringBuilder minvtransfnJudge(String minvtransfn, String notResult, StringBuilder aggregateBody, String delim) {
        if (CommonUtils.isNotEmpty(minvtransfn) && !notResult.equals(minvtransfn)) {
            aggregateBody.append(delim).append("MINVFUNC = ").append(minvtransfn);
        }
        return aggregateBody;
    }
    
    private StringBuilder mtransfnJudge(String mtransfn, String mtranstype, String notResult, StringBuilder aggregateBody, String delim) {
        if (CommonUtils.isNotEmpty(mtransfn) && !notResult.equals(mtransfn)) {
            aggregateBody.append(delim).append("MSFUNC = ").append(mtransfn);
            mtranstypeJudge(mtranstype, notResult, aggregateBody, delim);
        }
        return aggregateBody;
    }
    
    private StringBuilder mtranstypeJudge(String mtranstype, String notResult, StringBuilder aggregateBody, String delim) {
        if (CommonUtils.isNotEmpty(mtranstype) && !notResult.equals(mtranstype)) {
            aggregateBody.append(delim).append("MSTYPE = ").append(mtranstype);
        }
        return aggregateBody;
    }
    
    private StringBuilder initvalJudge(String initval, StringBuilder aggregateBody, String delim) {
        String newInitval = initval;
        if (CommonUtils.isNotEmpty(initval)) {
            if (!Pattern.matches("[0-9]+", initval)) {
                // Quote non numeric values
                newInitval = "'" + initval + "'";
            }
            aggregateBody.append(delim).append("INITCOND = ").append(newInitval);
        }
        return aggregateBody;
    }
    
    private StringBuilder aggfinalfnJudge(String aggfinalfn, StringBuilder aggregateBody, 
            boolean finalextra, String delim, TransitionModifies finalmodify) {
        if (CommonUtils.isNotEmpty(aggfinalfn)) {
            aggregateBody.append(delim).append("FINALFUNC = ").append(aggfinalfn);
            finalextraJudge(aggregateBody, finalextra, delim);
            finalmodifyJudge(aggregateBody, finalmodify, delim);
        }
        return aggregateBody;
    }
    
    private StringBuilder finalextraJudge(StringBuilder aggregateBody, boolean finalextra, String delim) {
        if (finalextra) {
            aggregateBody.append(delim).append("FINALFUNC_EXTRA");
        }
        return aggregateBody;
    }
    
    private StringBuilder finalmodifyJudge(StringBuilder aggregateBody, TransitionModifies finalmodify, 
            String delim) {
        if (finalmodify != null) {
            aggregateBody.append(delim).append("FINALFUNC_MODIFY = ").append(finalmodify.keyword);
        }
        return aggregateBody;
    }
    
    private StringBuilder serialfnJudge(String serialfn, StringBuilder aggregateBody, String notResult, String delim) {
        if (CommonUtils.isNotEmpty(serialfn) && !notResult.equals(serialfn)) {
            aggregateBody.append(delim).append("SERIALFUNC = ").append(serialfn);
        }
        return aggregateBody;
    }
    
    private StringBuilder deserialfnJudge(String deserialfn, StringBuilder aggregateBody, String notResult, String delim) {
        if (CommonUtils.isNotEmpty(deserialfn) && !notResult.equals(deserialfn)) {
            aggregateBody.append(delim).append("DESERIALFUNC = ").append(deserialfn);
        }
        return aggregateBody;
    }

    protected String generateFunctionDeclaration(PostgreLanguage language, String returnTypeName, String functionBody) {
        String lineSeparator = GeneralUtils.getDefaultLineSeparator();

        StringBuilder decl = new StringBuilder();

        String functionSignature = makeOverloadedName(getSchema(), getName(), params, true, true, true);
        decl.append("CREATE OR REPLACE ").append(getProcedureTypeName()).append(" ")
            .append(DBUtils.getQuotedIdentifier(getContainer())).append(".")
            .append(functionSignature).append(lineSeparator);
        returnJudge(returnTypeName, decl, lineSeparator);
        languageJudge(language, decl, lineSeparator);
        securityJudge(decl, lineSeparator);
        windowJudge(decl, lineSeparator);
        procedureTypeJudge(decl, lineSeparator);
        strictJudge(decl, lineSeparator);
        execCostJudge(decl, lineSeparator);
        estRowsJudge(decl, lineSeparator);
        if (!ArrayUtils.isEmpty(config)) {
            configJudge(decl, lineSeparator);
        }
        String delimiter = "$$"; // + getProcedureType().name().toLowerCase(Locale.ENGLISH) + "$";
        decl.append("AS ").append(delimiter).append("\n");
        functionBodyJudge(functionBody, decl);
        decl.append(delimiter).append(lineSeparator);
        return decl.toString();
    }
    
    private StringBuilder returnJudge(String returnTypeName, StringBuilder decl, String lineSeparator) {
        if (getProcedureType().hasReturnValue() && !CommonUtils.isEmpty(returnTypeName)) {
            decl.append("\tRETURNS ");
            if (isReturnsSet()) {
                // Check for TABLE parameters and construct
                List<PostgreProcedureParameter> tableParams = getParameters(DBSProcedureParameterKind.TABLE);
                tableParamsJudge(tableParams, decl, returnTypeName);
            } else {
                decl.append(returnTypeName);
            }
            decl.append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder tableParamsJudge(List<PostgreProcedureParameter> tableParams, StringBuilder decl, String returnTypeName) {
        if (!tableParams.isEmpty()) {
            decl.append("TABLE (");
            tableParamsSizeJudge(tableParams, decl);
            decl.append(")");
        } else {
            decl.append("SETOF ").append(returnTypeName);
        }
        return decl;
    }
    
    private StringBuilder tableParamsSizeJudge(List<PostgreProcedureParameter> tableParams, StringBuilder decl) {
        for (int i = 0; i < tableParams.size(); i++) {
            PostgreProcedureParameter tp = tableParams.get(i);
            if (i > 0) {
                decl.append(", ");
            }
            decl.append(tp.getName()).append(" ").append(tp.getTypeName());
        }
        return decl;
    }
    
    private StringBuilder configJudge(StringBuilder decl, String lineSeparator) {
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
                decl.append("\tSET ").append(paramName).append(" = ")
                .append(isNumeric ? paramValue : "'" + paramValue + "'").append(lineSeparator);
            } else {
                log.debug("Wrong function configuration parameter [" + configLine + "]");
            }
        }
        return decl;
    }
    
    private StringBuilder languageJudge(PostgreLanguage language, StringBuilder decl, String lineSeparator) {
        if (language != null) {
            decl.append("\tLANGUAGE ").append(language).append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder securityJudge(StringBuilder decl, String lineSeparator) {
        if (isSecurityDefiner()) {
            decl.append("\tSECURITY DEFINER").append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder windowJudge(StringBuilder decl, String lineSeparator) {
        if (isWindow()) {
            decl.append("\tWINDOW").append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder strictJudge(StringBuilder decl, String lineSeparator) {
        if (isStrict) {
            decl.append("\tSTRICT").append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder procedureTypeJudge(StringBuilder decl, String lineSeparator) {
        if (getProcedureType() == DBSProcedureType.FUNCTION && procVolatile != null) {
            decl.append("\t").append(procVolatile.getCreateClause()).append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder execCostJudge(StringBuilder decl, String lineSeparator) {
        if (execCost > 0 && execCost != DEFAULT_COST) {
            decl.append("\tCOST ").append(CommonUtils.niceFormatFloat(execCost)).append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder estRowsJudge(StringBuilder decl, String lineSeparator) {
        if (estRows > 0 && estRows != DEFAULT_EST_ROWS) {
            decl.append("\tROWS ").append(CommonUtils.niceFormatFloat(estRows)).append(lineSeparator);
        }
        return decl;
    }
    
    private StringBuilder functionBodyJudge(String functionBody, StringBuilder decl) {
        if (!CommonUtils.isEmpty(functionBody)) {
            decl.append("\t").append(functionBody).append("\n");
        }
        return decl;
    }

    @Override
    public void setObjectDefinitionText(String sourceText) {
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
        return PostgreUtils.getObjectById(monitor, new KingbaseDatabase.LanguageCache(), container.getDatabase(), languageId);
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

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 30)
    public float getExecCost() {
        return execCost;
    }

    @Property(category = DBConstants.CAT_STATISTICS, viewable = false, order = 31)
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

    public static String makeOverloadedName(
        @NotNull PostgreSchema schema,
        @NotNull String name,
        @NotNull List<PostgreProcedureParameter> params,
        boolean quote,
        boolean showParamNames,
        boolean forDDL
    ) {
        final String selfName = (quote ? DBUtils.getQuotedIdentifier(schema.getDataSource(), name) : name);
        final StringJoiner signature = new StringJoiner(", ", "(", ")");

        // Function signature may only contain a limited set of arguments inside parenthesis.
        // Examples of such arguments are: 'in', 'out', 'inout' and 'variadic'.
        // In our case, they all have associated keywords, so we could abuse it.
        final List<PostgreProcedureParameter> keywordParams = params.stream()
            .filter(x -> x.getArgumentMode().getKeyword() != null)
            .collect(Collectors.toList());

        // In general, 'in' arguments may contain only the type without the keyword because it's implied.
        // It's a shorthand for procedures that accept a set of arguments and return nothing, making its
        // signature slightly shorter. On the other hand, if procedure has mixed set of argument types,
        // we want to always include the keyword to avoid ambiguity.
        final boolean allIn = keywordParams.stream()
            .allMatch(x -> x.getArgumentMode() == ArgumentMode.i);

        for (PostgreProcedureParameter param : keywordParams) {
            final StringJoiner parameter = new StringJoiner(" ");
            if (!allIn) {
                parameter.add(param.getArgumentMode().getKeyword());
            }
            if (showParamNames) {
                String paramName = param.getName();
                if (forDDL && paramName.startsWith("$")) {
                    // Old PG versions. Skip this specific case, because it is not name, but param order number
                } else {
                    parameter.add(paramName);
                }
            }
            final PostgreDataType dataType = param.getParameterType();
            final PostgreSchema typeContainer = dataType.getParentObject();
            if (typeContainer.isPublicSchema() || typeContainer.isCatalogSchema()) {
                parameter.add(dataType.getName());
            } else {
                parameter.add(dataType.getFullyQualifiedName(DBPEvaluationContext.DDL));
            }
            String paramDefaultValue = param.getDefaultValue();
            if (forDDL && CommonUtils.isNotEmpty(paramDefaultValue)) {
                parameter.add("DEFAULT").add(paramDefaultValue);
            }
            signature.add(parameter.toString());
        }

        return selfName + signature;
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 200)
    public String getDescription() {
        return super.getDescription();
    }

    public String getFullQualifiedSignature() {
        return DBUtils.getQuotedIdentifier(getContainer()) + "." +
            makeOverloadedName(getSchema(), getName(), params, true, false, false);
    }

    public String getProcedureTypeName() {
        return kind.getName().toUpperCase(Locale.ENGLISH);
    }

    @Override
    public PostgreSchema getSchema() {
        return container;
    }

    @Override
    public Collection<PostgrePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, 
            boolean includeNestedObjects) throws DBException {
        return PostgreUtils.extractPermissionsFromACL(monitor, this, acl, false);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getProceduresCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return "ALTER " + this.getProcedureTypeName() + " " + this.getFullQualifiedSignature() + " OWNER TO " + owner;
    }

    @Association
    public List<PostgreDependency> getDependencies(DBRProgressMonitor monitor) throws DBCException {
        return PostgreDependency.readDependencies(monitor, this, true);
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        return DBPScriptObject.OPTION_INCLUDE_COMMENTS.equals(option) 
            || DBPScriptObject.OPTION_INCLUDE_PERMISSIONS.equals(option) 
            || DBPScriptObject.OPTION_CAST_PARAMS.equals(option);
    }

    @Override
    public String toString() {
        return overloadedName == null ? name : overloadedName;
    }
    
    
}
