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
package org.jkiss.dbeaver.ext.mimer.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanSourceFormat;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlan;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Mimer SQL execution plan. Retrieved via the driver's {@code
 * com.mimer.jdbc.Statement#getExplainText()} - called reflectively off {@link
 * JDBCPreparedStatement#getOriginal()} since the driver jar isn't a compile dependency, same
 * technique {@code FireBirdUtils.getPlan} uses.
 * <p>
 * Explain generation must be turned on for the connection first ({@code Connection#setExplain(1)},
 * also reflective), and the statement compiled (a plain {@code prepareStatement}, no execution
 * needed) before {@code getExplainText()} returns anything - it returns the plan for whichever
 * statement was compiled <i>last</i> on the connection, not necessarily the one it's called on.
 * The result is an XML document per {@code .../Manuals/App_explain/App_explain.htm}, mapped into
 * a tree by {@link MimerPlanNode}; {@link #findStatementRoot} unwraps the driver's own {@code
 * <mimersql><explain>} envelope around it first.
 *
 * @author Mimer Information Technology
 */
public class MimerExecutionPlan extends AbstractExecutionPlan {

    private static final Log log = Log.getLog(MimerExecutionPlan.class);

    private final String query;
    private String planText;
    private List<MimerPlanNode> rootNodes = List.of();

    public MimerExecutionPlan(@NotNull String query) {
        this.query = query;
    }

    /**
     * Compiles {@link #query} and reads back its explain text. Does not execute the query.
     */
    public void explain(@NotNull DBCSession session) throws DBCException {
        JDBCSession jdbcSession = (JDBCSession) session;
        Connection connection = null;
        try {
            connection = jdbcSession.getOriginal();
            setExplainMode(connection, true);
            try (JDBCPreparedStatement dbStat = jdbcSession.prepareStatement(query)) {
                planText = readExplainText(dbStat.getOriginal());
            }
        } catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        } finally {
            // Best-effort - don't let a failure here mask an explain text we already have
            if (connection != null) {
                try {
                    setExplainMode(connection, false);
                } catch (DBCException e) {
                    log.debug("Can't turn off Mimer SQL explain mode", e);
                }
            }
        }
        if (!CommonUtils.isEmpty(planText)) {
            try {
                Document document = XMLUtils.parseDocument(new StringReader(planText));
                Element statementRoot = findStatementRoot(document.getDocumentElement());
                rootNodes = List.of(new MimerPlanNode(statementRoot, null));
            } catch (XMLException e) {
                throw new DBCException("Can't parse Mimer SQL explain output as XML", e);
            }
        }
    }

    private static void setExplainMode(@NotNull Connection connection, boolean enabled) throws DBCException {
        try {
            connection.getClass().getMethod("setExplain", int.class).invoke(connection, enabled ? 1 : 0);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new DBCException("Can't set Mimer SQL explain mode - driver doesn't expose Connection#setExplain(int)", e);
        }
    }

    /**
     * Skips past the {@code <mimersql><explain>} wrapper to the real statement root ({@code
     * <select>}/{@code <insert>}/etc.). Falls back to the raw document root if a future driver
     * version changes the envelope.
     */
    @NotNull
    static Element findStatementRoot(@NotNull Element documentRoot) {
        Element explain = XMLUtils.getChildElement(documentRoot, "explain");
        if (explain != null) {
            Collection<Element> children = XMLUtils.getChildElementList(explain);
            if (!children.isEmpty()) {
                return children.iterator().next();
            }
        }
        return documentRoot;
    }

    @Nullable
    private static String readExplainText(@NotNull PreparedStatement stmt) throws DBCException {
        try {
            return (String) stmt.getClass().getMethod("getExplainText").invoke(stmt);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new DBCException("Can't read Mimer SQL explain text - driver doesn't expose getExplainText()", e);
        }
    }

    @NotNull
    @Override
    public String getQueryString() {
        return query;
    }

    @NotNull
    @Override
    public String getPlanQueryString() {
        // No standalone SQL text to (re-)run - the plan comes from a driver API call made
        // against a prepared version of getQueryString(), not from executing SQL of its own.
        return "";
    }

    @NotNull
    @Override
    public DBCPlanSourceFormat getPlanSourceDataFormat() {
        return DBCPlanSourceFormat.XML;
    }

    @Nullable
    @Override
    public Object getPlanSourceData() {
        return planText;
    }

    @NotNull
    @Override
    public List<? extends DBCPlanNode> getPlanNodes(@NotNull Map<String, Object> options) {
        return rootNodes;
    }
}
