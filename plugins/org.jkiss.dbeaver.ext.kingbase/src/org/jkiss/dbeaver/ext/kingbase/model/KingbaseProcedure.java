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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.ext.kingbase.KingbaseValueParser;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt2;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractProcedure;
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

/**
 * KingbaseProcedure
 */
public class KingbaseProcedure extends AbstractProcedure<KingbaseDataSource, KingbaseSchema>
        implements KingbaseObject, KingbaseScriptObject, KingbasePrivilegeOwner, DBPUniqueObject, DBPOverloadedObject, DBPNamedObject2, DBPRefreshableObject, DBPScriptObjectExt2
{
    private static final Log log = Log.getLog(KingbaseProcedure.class);

    private static final String CAT_FLAGS = "Flags";
    private static final String CAT_PROPS = "Properties";

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
        i(DBSProcedureParameterKind.IN, "in"),
        o(DBSProcedureParameterKind.OUT, "out"),
        b(DBSProcedureParameterKind.INOUT, "inout"),
        v(DBSProcedureParameterKind.RESULTSET, "variadic"),
        t(DBSProcedureParameterKind.TABLE, null),
        u(DBSProcedureParameterKind.UNKNOWN, null);

        private final DBSProcedureParameterKind parameterKind;
        private final String keyword;

        ArgumentMode(@NotNull DBSProcedureParameterKind parameterKind, @Nullable String keyword) {
            this.parameterKind = parameterKind;
            this.keyword = keyword;
        }

        @NotNull
        public DBSProcedureParameterKind getParameterKind() {
            return parameterKind;
        }

        @Nullable
        public String getKeyword() {
            return keyword;
        }
    }

    enum TransitionModifies {
        r("READ_ONLY"),
        s("SHAREABLE"),
        w("READ_WRITE");

        private final String keyword;

        TransitionModifies(String keyword) {
            this.keyword = keyword;
        }
    }

    private long oid;
    private KingbaseProcedureKind kind;
    private String procSrc;
    private String body;
    private long ownerId;
    private long languageId;
    private float execCost;
    private float estRows;
    private KingbaseDataType varArrayType;
    private String procTransform;
    private boolean isAggregate;
    private boolean isWindow;
    private boolean isSecurityDefiner;
    private boolean leakproof;
    private boolean isStrict;
    private boolean returnsSet;
    private ProcedureVolatile procVolatile;
    private KingbaseDataType returnType;
    private int[] transformTypes;
    private String[] config;
    private Object acl;

    private String overloadedName;
    private List<KingbaseProcedureParameter> params = new ArrayList<>();

    public KingbaseProcedure(KingbaseSchema schema) {
        super(schema, false);
    }

    public KingbaseProcedure(
        DBRProgressMonitor monitor,
        KingbaseSchema schema,
        ResultSet dbResult)
    {
        super(schema, true);
        loadInfo(monitor, dbResult);
    }

    private void loadInfo(DBRProgressMonitor monitor, ResultSet dbResult) {
        KingbaseDataSource dataSource = getDataSource();

        this.oid = JDBCUtils.safeGetLong(dbResult, "poid");
        setName(JDBCUtils.safeGetString(dbResult, "proname"));
        this.ownerId = JDBCUtils.safeGetLong(dbResult, "proowner");
        this.languageId = JDBCUtils.safeGetLong(dbResult, "prolang");
        
        this.execCost = JDBCUtils.safeGetFloat(dbResult, "procost");
        this.estRows = JDBCUtils.safeGetFloat(dbResult, "prorows");
        

        Number[] allArgTypes = KingbaseUtils.safeGetNumberArray(dbResult, "proallargtypes");
        String[] argNames = KingbaseUtils.safeGetStringArray(dbResult, "proargnames");
        if (!ArrayUtils.isEmpty(allArgTypes)) {
            String[] argModes = KingbaseUtils.safeGetStringArray(dbResult, "proargmodes");

            for (int i = 0; i < allArgTypes.length; i++) {
                final long paramType = allArgTypes[i].longValue();
                final KingbaseDataType dataType = container.getDatabase().getDataType(monitor, paramType);
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
                params.add(new KingbaseProcedureParameter(
                    this,
                    paramName,
                    dataType,
                    mode,
                    i + 1
                ));
            }

        } else {
            long[] inArgTypes = KingbaseUtils.getIdVector(JDBCUtils.safeGetObject(dbResult, "proargtypes"));

            if (!ArrayUtils.isEmpty(inArgTypes)) {
                for (int i = 0; i < inArgTypes.length; i++) {
                    Long paramType = inArgTypes[i];
                    final KingbaseDataType dataType = container.getDatabase().getDataType(monitor, paramType.intValue());
                    if (dataType == null) {
                        log.warn("Parameter data type [" + paramType + "] not found");
                        continue;
                    }
                    String paramName = argNames == null || argNames.length < inArgTypes.length ? "$" + (i + 1) : argNames[i];
                    KingbaseProcedureParameter param = new KingbaseProcedureParameter(
                        this, paramName, dataType, ArgumentMode.i, i + 1);
                    params.add(param);
                }
            }
        }

        try {
            String argDefaultsString = JDBCUtils.safeGetString(dbResult, "arg_defaults");
            String[] argDefaults = null;
            if (!CommonUtils.isEmpty(argDefaultsString)) {
                try {
                    argDefaults = KingbaseValueParser.parseSingleObject(argDefaultsString);
                } catch (DBCException e) {
                    log.debug("Error parsing function parameters defaults", e);
                }
            }
            if (argDefaults != null && argDefaults.length > 0) {
                int paramsAssigned = 0;
                for (int i = params.size() - 1; i >= 0; i--) {
                    DBSProcedureParameterKind parameterKind = params.get(i).getParameterKind();
                    if (parameterKind == DBSProcedureParameterKind.OUT || parameterKind == DBSProcedureParameterKind.TABLE || parameterKind == DBSProcedureParameterKind.RETURN) {
                        continue;
                    }
                    String defaultValue = argDefaults[argDefaults.length - 1 - paramsAssigned];
                    if (defaultValue != null) defaultValue = defaultValue.trim();
                    params.get(i).setDefaultValue(defaultValue);
                    paramsAssigned++;
                    if (paramsAssigned >= argDefaults.length) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing parameters defaults", e);
        }

        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false, false);

        
        final long varTypeId = JDBCUtils.safeGetLong(dbResult, "provariadic");
        if (varTypeId != 0) {
            varArrayType = container.getDatabase().getDataType(monitor, varTypeId);
        }
        
       
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

        
        this.config = KingbaseUtils.safeGetStringArray(dbResult, "proconfig");
        

        if (dataSource.getServerType().supportsStoredProcedures()) {
            String proKind = JDBCUtils.safeGetString(dbResult, "prokind");
            kind = CommonUtils.valueOf(KingbaseProcedureKind.class, proKind, KingbaseProcedureKind.f);
            if (kind == KingbaseProcedureKind.a) {
                isAggregate = true;
            }
        } else {
            if (isAggregate) {
                kind = KingbaseProcedureKind.a;
            } else if (isWindow) {
                kind = KingbaseProcedureKind.w;
            } else {
                boolean isProcedure = false;
                try {
                    isProcedure = dbResult.getBoolean("prosp");
                } catch (SQLException e) {
                }
                if (isProcedure) {
                    kind = KingbaseProcedureKind.p;
                } else {
                    kind = KingbaseProcedureKind.f;
                }
            }
        }
    }

    @NotNull
    @Override
    public KingbaseDatabase getDatabase() {
        return container.getDatabase();
    }

    @Property(viewable = false, order = 3)
    public KingbaseProcedureKind getKind() {
        return kind;
    }

    public void setKind(KingbaseProcedureKind kind) {
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

    @Nullable
    @Override
    public List<KingbaseProcedureParameter> getParameters(@NotNull DBRProgressMonitor monitor) {
        return params;
    }

    public List<KingbaseProcedureParameter> getInputParameters() {
        List<KingbaseProcedureParameter> result = new ArrayList<>();
        for (KingbaseProcedureParameter param : params) {
            if (param.getParameterKind().isInput()) {
                result.add(param);
            }
        }
        return result;
    }

    public List<KingbaseProcedureParameter> getParameters(DBSProcedureParameterKind kind) {
        List<KingbaseProcedureParameter> result = new ArrayList<>();
        for (KingbaseProcedureParameter param : params) {
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
        this.overloadedName = makeOverloadedName(getSchema(), getName(), params, false, false, false);
    }

    @Override
    @Property(hidden = true, editable = true, updatable = true, order = -1)
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        boolean omitHeader = CommonUtils.getOption(options, OPTION_DEBUGGER_SOURCE);
        String procDDL = omitHeader ? "" : "-- DROP " + getProcedureTypeName() + " " + getFullQualifiedSignature() + ";\n\n";
        if (isPersisted() && (!getDataSource().getServerType().supportsFunctionDefRead() || omitHeader) && !isAggregate) {
            if (procSrc == null) {
                try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read procedure body")) {
                    procSrc = JDBCUtils.queryString(session, "SELECT prosrc FROM sys_proc where oid = ?", getObjectId());
                } catch (SQLException e) {
                    throw new DBException("Error reading procedure body", e);
                }
            }
            KingbaseDataType returnType = getReturnType();
            String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
            procDDL += omitHeader ? procSrc : generateFunctionDeclaration(getLanguage(monitor), returnTypeName, procSrc);
        } else {
            if (body == null) {
                if (!isPersisted()) {
                    KingbaseDataType returnType = getReturnType();
                    String returnTypeName = returnType == null ? null : returnType.getFullTypeName();
                    body = generateFunctionDeclaration(getLanguage(monitor), returnTypeName, "\n\t-- Enter function body here\n");
                } else if (oid == 0) {
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
            procDDL += body;
        }
        if (this.isPersisted() && !omitHeader) {
            procDDL += ";\n";

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_COMMENTS) && !CommonUtils.isEmpty(getDescription())) {
                procDDL += "\nCOMMENT ON " + getProcedureTypeName() + " " + getFullQualifiedSignature() + " IS " + SQLUtils.quoteString(this, getDescription()) + ";\n";
            }

            if (CommonUtils.getOption(options, DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                List<DBEPersistAction> actions = new ArrayList<>();
                KingbaseUtils.getObjectGrantPermissionActions(monitor, this, actions, options);
                procDDL += "\n" + SQLUtils.generateScript(getDataSource(), actions.toArray(new DBEPersistAction[0]), false);
            }
        }

        return procDDL;
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
                "format_type(aggmtranstype, NULL) as aggmtranstype\n"  +
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
                        String aggfinalfn = JDBCUtils.safeGetString(dbResult, "aggfinalfn"); // Final function
                        String oprname = JDBCUtils.safeGetString(dbResult, "oprname"); // Associated sort operator

                        // The initial value of the transition state
                        String initval = JDBCUtils.safeGetString(dbResult, "agginitval");

                        // Forward transition function for moving-aggregate mode
                        String mtransfn = JDBCUtils.safeGetString(dbResult, "aggmtransfn");
                        String mtranstype = JDBCUtils.safeGetString(dbResult, "aggmtranstype");

                        // Inverse transition function for moving-aggregate mode
                        String minvtransfn = JDBCUtils.safeGetString(dbResult, "aggminvtransfn");

                        String serialfn = JDBCUtils.safeGetString(dbResult, "aggserialfn");
                        String deserialfn = JDBCUtils.safeGetString(dbResult, "aggdeserialfn");
                        String mfinalfn = JDBCUtils.safeGetString(dbResult, "aggmfinalfn");

                        TransitionModifies finalmodify = null;
                        TransitionModifies mfinalmodify = null;
                        
                        finalmodify = TransitionModifies.valueOf(
                            JDBCUtils.safeGetString(dbResult, "aggfinalmodify"));
                        mfinalmodify = TransitionModifies.valueOf(
                            JDBCUtils.safeGetString(dbResult, "aggmfinalmodify")); // For the aggmfinalfn
                        

                        boolean finalextra = JDBCUtils.safeGetBoolean(dbResult, "aggfinalextra"); // arguments to aggfinalfn
                        boolean mfinalextra = JDBCUtils.safeGetBoolean(dbResult, "aggmfinalextra"); // arguments to aggmfinalfn

                        StringBuilder aggregateBody = new StringBuilder("CREATE OR REPLACE AGGREGATE ");
                        final String delim = ",\n\t";
                        final String notResult = "-";
                        aggregateBody.append(fullName).append(" (\n")
                            .append("\tSFUNC = ").append(aggtransfn).append(delim)
                            .append("STYPE = ").append(aggtranstype);
                        if (CommonUtils.isNotEmpty(aggfinalfn)) {
                            aggregateBody.append(delim).append("FINALFUNC = ").append(aggfinalfn);
                            if (finalextra) {
                                aggregateBody.append(delim).append("FINALFUNC_EXTRA");
                            }
                            if (finalmodify != null) {
                                aggregateBody.append(delim).append("FINALFUNC_MODIFY = ").append(finalmodify.keyword);
                            }
                        }
                        if (CommonUtils.isNotEmpty(serialfn) && !notResult.equals(serialfn)) {
                            aggregateBody.append(delim).append("SERIALFUNC = ").append(serialfn);
                        }
                        if (CommonUtils.isNotEmpty(deserialfn) && !notResult.equals(deserialfn)) {
                            aggregateBody.append(delim).append("DESERIALFUNC = ").append(deserialfn);
                        }
                        if (CommonUtils.isNotEmpty(initval)) {
                            if (!Pattern.matches("[0-9]+", initval)) {
                                initval = "'" + initval + "'";
                            }
                            aggregateBody.append(delim).append("INITCOND = ").append(initval);
                        }
                        if (CommonUtils.isNotEmpty(mtransfn) && !notResult.equals(mtransfn)) {
                            aggregateBody.append(delim).append("MSFUNC = ").append(mtransfn);
                            if (CommonUtils.isNotEmpty(mtranstype) && !notResult.equals(mtranstype)) {
                                aggregateBody.append(delim).append("MSTYPE = ").append(mtranstype);
                            }
                        }
                        if (CommonUtils.isNotEmpty(minvtransfn) && !notResult.equals(minvtransfn)) {
                            aggregateBody.append(delim).append("MINVFUNC = ").append(minvtransfn);
                        }
                        if (CommonUtils.isNotEmpty(mfinalfn) && !notResult.equals(mfinalfn)) {
                            aggregateBody.append(delim).append("MFINALFUNC = ").append(mfinalfn);
                            if (mfinalextra) {
                                aggregateBody.append(delim).append("MFINALFUNC_EXTRA");
                            }
                            if (mfinalmodify != null) {
                                aggregateBody.append(delim).append("MFINALFUNC_MODIFY = ").append(mfinalmodify.keyword);
                            }
                        }
                        if (CommonUtils.isNotEmpty(oprname)) {
                            aggregateBody.append(delim).append("SORTOP = ").append(oprname);
                        }
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

    protected String generateFunctionDeclaration(KingbaseLanguage language, String returnTypeName, String functionBody) {
        String lineSeparator = GeneralUtils.getDefaultLineSeparator();

        StringBuilder decl = new StringBuilder();

        String functionSignature = makeOverloadedName(getSchema(), getName(), params, true, true, true);
        decl.append("CREATE OR REPLACE ").append(getProcedureTypeName()).append(" ")
            .append(DBUtils.getQuotedIdentifier(getContainer())).append(".")
            .append(functionSignature).append(lineSeparator);
        if (getProcedureType().hasReturnValue() && !CommonUtils.isEmpty(returnTypeName)) {
            decl.append("\tRETURNS ");
            if (isReturnsSet()) {
                List<KingbaseProcedureParameter> tableParams = getParameters(DBSProcedureParameterKind.TABLE);
                if (!tableParams.isEmpty()) {
                    decl.append("TABLE (");
                    for (int i = 0; i < tableParams.size(); i++) {
                        KingbaseProcedureParameter tp = tableParams.get(i);
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
        if (isSecurityDefiner()) {
            decl.append("\tSECURITY DEFINER").append(lineSeparator);
        }
        if (isWindow()) {
            decl.append("\tWINDOW").append(lineSeparator);
        }
        if (getProcedureType() == DBSProcedureType.FUNCTION && procVolatile != null) {
            decl.append("\t").append(procVolatile.getCreateClause()).append(lineSeparator);
        }
        if (isStrict) {
            decl.append("\tSTRICT").append(lineSeparator);
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
        String delimiter = "$$";
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
    public KingbaseRole getOwner(DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsRoles()) {
            return null;
        }
        return container.getDatabase().getRoleById(monitor, ownerId);
    }

    @Property(category = CAT_PROPS, viewable = true, order = 11)
    public KingbaseLanguage getLanguage(DBRProgressMonitor monitor) throws DBException {
        return KingbaseUtils.getObjectById(monitor, container.getDatabase().languageCache, container.getDatabase(), languageId);
    }

    public void setLanguage(KingbaseLanguage language) {
        this.languageId = language.getObjectId();
    }

    @Property(category = CAT_PROPS, viewable = true, order = 12)
    public KingbaseDataType getReturnType() {
        return returnType;
    }

    public void setReturnType(KingbaseDataType returnType) {
        this.returnType = returnType;
    }

    @Property(category = CAT_PROPS, viewable = false, order = 13)
    public KingbaseDataType getVarArrayType() {
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
        @NotNull KingbaseSchema schema,
        @NotNull String name,
        @NotNull List<KingbaseProcedureParameter> params,
        boolean quote,
        boolean showParamNames,
        boolean forDDL
    ) {
        final String selfName = (quote ? DBUtils.getQuotedIdentifier(schema.getDataSource(), name) : name);
        final StringJoiner signature = new StringJoiner(", ", "(", ")");
        final List<KingbaseProcedureParameter> keywordParams = params.stream()
            .filter(x -> x.getArgumentMode().getKeyword() != null)
            .collect(Collectors.toList());
        final boolean allIn = keywordParams.stream()
            .allMatch(x -> x.getArgumentMode() == ArgumentMode.i);

        for (KingbaseProcedureParameter param : keywordParams) {
            final StringJoiner parameter = new StringJoiner(" ");
            if (!allIn) {
                parameter.add(param.getArgumentMode().getKeyword());
            }
            if (showParamNames) {
                String paramName = param.getName();
                if (forDDL && paramName.startsWith("$")) {
                } else {
                    parameter.add(paramName);
                }
            }
            final KingbaseDataType dataType = param.getParameterType();
            final KingbaseSchema typeContainer = dataType.getParentObject();
            if (typeContainer.isPublicSchema() || typeContainer.isCatalogSchema() || typeContainer.isSysCatalogSchema()) {
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
    public String getDescription()
    {
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
    public KingbaseSchema getSchema() {
        return container;
    }

    @Override
    public Collection<KingbasePrivilege> getPrivileges(@NotNull DBRProgressMonitor monitor, boolean includeNestedObjects) throws DBException {
        return KingbaseUtils.extractPermissionsFromACL(monitor, this, acl, false);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getProceduresCache().refreshObject(monitor, getContainer(), this);
    }

    @Override
    public String generateChangeOwnerQuery(@NotNull String owner, @NotNull Map<String, Object> options) {
        return "ALTER " + this.getProcedureTypeName() + " " + this.getFullQualifiedSignature() + " OWNER TO " + owner;
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
