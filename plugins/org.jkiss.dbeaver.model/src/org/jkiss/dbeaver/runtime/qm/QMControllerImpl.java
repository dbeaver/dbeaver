/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * QMController default implementation
 */
public class QMControllerImpl implements QMController {

    private static final Log log = Log.getLog(QMControllerImpl.class);

    private QMExecutionHandler defaultHandler;
    private QMMCollectorImpl metaHandler;
    private final List<QMExecutionHandler> handlers = new ArrayList<>();
    private QMEventBrowser eventBrowser;
    private DefaultEventBrowser defaultEventBrowser = new DefaultEventBrowser();

    public QMControllerImpl() {
        defaultHandler = (QMExecutionHandler) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{ QMExecutionHandler.class },
            new NotifyInvocationHandler());

        metaHandler = new QMMCollectorImpl();
        registerHandler(metaHandler);
    }

    public void dispose()
    {
        if (metaHandler != null) {
            unregisterHandler(metaHandler);
            metaHandler.dispose();
            metaHandler = null;
        }

        synchronized (handlers) {
            if (!handlers.isEmpty()) {
                log.warn("Some QM handlers are still registered: " + handlers);
                handlers.clear();
            }
        }
      	defaultHandler = null;
    }

    @Override
    public QMMCollector getMetaCollector()
    {
        return metaHandler;
    }

    @Override
    public QMExecutionHandler getDefaultHandler() {
        return defaultHandler;
    }

    @Override
    public synchronized QMEventBrowser getEventBrowser(boolean currentSessionOnly) {
        if (currentSessionOnly) {
            return defaultEventBrowser;
        }
        if (eventBrowser == null) {
            eventBrowser = GeneralUtils.adapt(this, QMEventBrowser.class);
            if (eventBrowser == null) {
                // Default browser
                this.eventBrowser = defaultEventBrowser;
            }
        }

        return eventBrowser;
    }

    @Override
    public void registerHandler(QMExecutionHandler handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    @Override
    public void unregisterHandler(QMExecutionHandler handler) {
        synchronized (handlers) {
            if (!handlers.remove(handler)) {
                log.warn("QM handler '" + handler + "' isn't registered within QM controller");
            }
        }
    }

    @Override
    public void registerMetaListener(QMMetaListener metaListener)
    {
        metaHandler.addListener(metaListener);
    }

    @Override
    public void unregisterMetaListener(QMMetaListener metaListener)
    {
        metaHandler.removeListener(metaListener);
    }

    List<QMExecutionHandler> getHandlers()
    {
        synchronized (handlers) {
            return handlers;
        }
    }

    private class NotifyInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            try {
                if (method.getReturnType() == Void.TYPE && method.getName().startsWith("handle")) {
                    QMExecutionHandler[] handlersCopy;
                    synchronized (handlers) {
                        handlersCopy = handlers.toArray(new QMExecutionHandler[handlers.size()]);
                    }
                    for (QMExecutionHandler handler : handlersCopy) {
                        try {
                            method.invoke(handler, args);
                        } catch (InvocationTargetException e) {
                            log.debug("Error notifying QM handler '" + handler.getHandlerName() + "'", e.getTargetException());
                        }
                    }

                    return null;
                } else if (method.getName().equals("getHandlerName")) {
                    return "Default";
                } else {
                    return method.invoke(this, args);
                }
            } catch (Throwable e) {
            // just ignore it
            log.debug("Error executing QM method " + method, e);
            return null;
        }
        }

    }

    private class DefaultEventBrowser implements QMEventBrowser {
        @Override
        public QMEventCursor getQueryHistoryCursor(
            @NotNull DBRProgressMonitor monitor,
            @NotNull QMEventCriteria criteria,
            @Nullable QMEventFilter filter)
            throws DBException
        {
            List<QMMetaEvent> pastEvents = metaHandler.getPastEvents();
            Collections.reverse(pastEvents);
            if (criteria.getObjectTypes() != null || criteria.getQueryTypes() != null) {
                // Filter by query type and object type
                for (Iterator<QMMetaEvent> iter = pastEvents.iterator(); iter.hasNext(); ) {
                    QMMetaEvent event = iter.next();
                    if (criteria.getObjectTypes() != null) {
                        if (!matchesObjectType(event.getObject(), criteria.getObjectTypes())) {
                            iter.remove();
                            continue;
                        }
                    }
                    if (filter != null && !filter.accept(event)) {
                        iter.remove();
                        continue;
                    }
                    if (criteria.getQueryTypes() != null) {
                        QMMStatementInfo statementInfo = null;
                        if (event.getObject() instanceof QMMStatementInfo) {
                            statementInfo = (QMMStatementInfo) event.getObject();
                        } else if (event.getObject() instanceof QMMStatementExecuteInfo) {
                            statementInfo = ((QMMStatementExecuteInfo) event.getObject()).getStatement();
                        }
                        if (statementInfo != null &&
                            !ArrayUtils.contains(criteria.getQueryTypes(), statementInfo.getPurpose()))
                        {
                            iter.remove();
                        }
                    }
                }
            }
            if (CommonUtils.isEmpty(criteria.getSearchString())) {
                return new QMUtils.ListCursorImpl(pastEvents);
            } else {
                String searchString = criteria.getSearchString().toLowerCase();
                List<QMMetaEvent> filtered = new ArrayList<>();
                for (QMMetaEvent event : pastEvents) {
                    if (event.getObject().getText().toLowerCase().contains(searchString) &&
                        (filter == null || filter.accept(event)))
                    {
                        filtered.add(event);
                    }
                }
                return new QMUtils.ListCursorImpl(filtered);
            }
        }

        private boolean matchesObjectType(QMMObject object, QMObjectType[] objectTypes) {
            if (object instanceof QMMSessionInfo)
                return ArrayUtils.contains(objectTypes, QMObjectType.session);
            else if (object instanceof QMMTransactionInfo || object instanceof QMMTransactionSavepointInfo)
                return ArrayUtils.contains(objectTypes, QMObjectType.txn);
            else
                return ArrayUtils.contains(objectTypes, QMObjectType.query);
        }
    }
}
