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
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.SimpleExclusiveLock;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSInstance;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC data source
 */
public class JDBCRemoteInstance implements DBSInstance {
    private static final Log log = Log.getLog(JDBCRemoteInstance.class);

    @NotNull
    protected final JDBCDataSource dataSource;
    @Nullable
    protected JDBCRemoteInstance sharedInstance;
    @Nullable
    protected JDBCExecutionContext executionContext;
    @Nullable
    protected JDBCExecutionContext metaContext;
    @NotNull
    private final List<JDBCExecutionContext> allContexts = new ArrayList<>();
    private final DBPExclusiveResource exclusiveLock = new SimpleExclusiveLock();

    protected JDBCRemoteInstance(@NotNull DBRProgressMonitor monitor, @NotNull JDBCDataSource dataSource, boolean initContext)
        throws DBException {
        this.dataSource = dataSource;
        if (initContext) {
            initializeMainContext(monitor);
        }
    }

    protected JDBCRemoteInstance(@NotNull JDBCDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public JDBCDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public String getName() {
        return dataSource.getName();
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public String getDescription() {
        return null;
    }

    protected void initializeMainContext(@NotNull DBRProgressMonitor monitor) throws DBCException {
        if (sharedInstance != null) {
            return;
        }
        if (executionContext == null) {
            String mainContextName = dataSource.getContainer().getClientApplicationName();
            if (mainContextName == null) {
                mainContextName = getMainContextName();
            }
            this.executionContext = dataSource.createExecutionContext(this, mainContextName);
            this.executionContext.connect(monitor, null, null, null, true);
        }
    }

    public JDBCExecutionContext initializeMetaContext(@NotNull DBRProgressMonitor monitor)
        throws DBException {
        if (sharedInstance != null) {
            return sharedInstance.initializeMetaContext(monitor);
        }
        if (this.metaContext != null) {
            return this.metaContext;
        }
        
        DBPDataSourceContainer container = dataSource.getContainer();
        SeparateConnectionBehavior behavior = SeparateConnectionBehavior.parse(
            container.getPreferenceStore().getString(ModelPreferences.META_SEPARATE_CONNECTION)
        );
        boolean isMetaConnectionSeparate;
        switch (behavior) {
            case ALWAYS:
                isMetaConnectionSeparate = true;
                break;
            case NEVER:
                isMetaConnectionSeparate = false;
                break;
            case DEFAULT:
            default: 
                isMetaConnectionSeparate = !container.getDriver().isEmbedded() && !container.isForceUseSingleConnection();
                break;
        }

        if (isMetaConnectionSeparate) {
            // FIXME: do not sync expensive operations
            //synchronized (allContexts) {
                this.metaContext = dataSource.createExecutionContext(this, getMetadataContextName());
                this.metaContext.connect(monitor, true, null, null, true);
                return this.metaContext;
            //}
        } else {
            return this.executionContext;
        }
    }

    @NotNull
    protected String getMainContextName() {
        return JDBCExecutionContext.TYPE_MAIN;
    }

    @NotNull
    protected String getMetadataContextName() {
        return JDBCExecutionContext.TYPE_METADATA;
    }

    @NotNull
    @Override
    public DBCExecutionContext openIsolatedContext(@NotNull DBRProgressMonitor monitor, @NotNull String purpose, @Nullable DBCExecutionContext initFrom) throws DBException {
        if (sharedInstance != null) {
            return sharedInstance.openIsolatedContext(monitor, purpose, initFrom);
        }
        JDBCExecutionContext context = dataSource.createExecutionContext(this, purpose);
        DBExecUtils.tryExecuteRecover(monitor, getDataSource(), monitor1 -> {
            try {
                context.connect(monitor1, null, null, (JDBCExecutionContext) initFrom, true);
            } catch (DBCException e) {
                throw new InvocationTargetException(e);
            }
        });
        return context;
    }

    @NotNull
    @Override
    public JDBCExecutionContext[] getAllContexts() {
        if (sharedInstance != null) {
            return sharedInstance.getAllContexts();
        }
        synchronized (allContexts) {
            return allContexts.toArray(new JDBCExecutionContext[0]);
        }
    }

    @NotNull
    @Override
    public JDBCExecutionContext getDefaultContext(@NotNull DBRProgressMonitor monitor, boolean meta) {
        if (sharedInstance != null) {
            return sharedInstance.getDefaultContext(monitor, meta);
        }
        return getDefaultContext(meta);
    }

    @NotNull
    public JDBCExecutionContext getDefaultContext(boolean meta) {
        if (sharedInstance != null) {
            return sharedInstance.getDefaultContext(meta);
        }
        if (metaContext != null && (meta || executionContext == null)) {
            return this.metaContext;
        }
        if (executionContext == null) {
            log.debug("No execution context within database instance");
            return null;
        }
        return executionContext;
    }

    @Override
    public void shutdown(DBRProgressMonitor monitor) {
        if (sharedInstance != null) {
            return;
        }
        shutdown(monitor, false);
    }

    @NotNull
    @Override
    public DBPExclusiveResource getExclusiveLock() {
        return exclusiveLock;
    }

    /**
     * Closes all instance contexts
     *
     * @param monitor  progress monitor
     * @param keepMeta do not close meta context
     */
    public void shutdown(DBRProgressMonitor monitor, boolean keepMeta) {
        // [JDBC] Need sync here because real connection close could take some time
        // while UI may invoke callbacks to operate with connection
        List<JDBCExecutionContext> ctxCopy;
        synchronized (allContexts) {
            ctxCopy = new ArrayList<>(allContexts);
        }
        for (JDBCExecutionContext context : ctxCopy) {
            if (keepMeta && context == metaContext) {
                continue;
            }
            monitor.subTask("Close context '" + context.getContextName() + "'");
            context.close();
            monitor.worked(1);
        }
    }

    void addContext(JDBCExecutionContext context) {
        synchronized (allContexts) {
            if (allContexts.contains(context)) {
                log.debug("Duplicate execution context add");
            } else {
                allContexts.add(context);
            }
        }
    }

    boolean removeContext(JDBCExecutionContext context) {
        synchronized (allContexts) {
            if (context == executionContext) {
                executionContext = null;
            }
            if (context == metaContext) {
                metaContext = null;
            }
            return allContexts.remove(context);
        }
    }
}
