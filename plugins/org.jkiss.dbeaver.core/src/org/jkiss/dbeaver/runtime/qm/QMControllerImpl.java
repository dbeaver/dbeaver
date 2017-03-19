/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.qm.QMMetaEvent;
import org.jkiss.dbeaver.model.qm.QMMetaListener;
import org.jkiss.dbeaver.model.qm.QMMCollector;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * QMController default implementation
 */
public class QMControllerImpl implements QMController {

    private static final Log log = Log.getLog(QMControllerImpl.class);

    private QMExecutionHandler defaultHandler;
    private QMMCollectorImpl metaHandler;
    private final List<QMExecutionHandler> handlers = new ArrayList<>();

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

    @Override
    public List<QMMetaEvent> getPastMetaEvents()
    {
        return metaHandler.getPastEvents();
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

}
