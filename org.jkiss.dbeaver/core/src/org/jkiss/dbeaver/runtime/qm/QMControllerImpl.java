/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.runtime.qm.meta.QMMCollector;

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

    static final Log log = LogFactory.getLog(QMControllerImpl.class);

    private QMExecutionHandler defaultHandler;
    private QMMCollector metaHandler;
    private List<QMExecutionHandler> handlers = new ArrayList<QMExecutionHandler>();
    private DataSourceRegistry dataSourceRegistry;

    public QMControllerImpl(DataSourceRegistry dataSourceRegistry) {
        this.dataSourceRegistry = dataSourceRegistry;

        defaultHandler = (QMExecutionHandler) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{ QMExecutionHandler.class },
            new NotifyInvocationHandler());

        metaHandler = new QMMCollector();
        registerHandler(metaHandler);
    }

    public void dispose()
    {
        if (metaHandler != null) {
            unregisterHandler(metaHandler);
            metaHandler.dispose();
            metaHandler = null;
        }

        if (!handlers.isEmpty()) {
            log.warn("Some QM handlers are still registered: " + handlers);
            handlers.clear();
        }
      	defaultHandler = null;
    }

    public QMExecutionHandler getDefaultHandler() {
        return defaultHandler;
    }

    public synchronized void registerHandler(QMExecutionHandler handler) {
        handlers.add(handler);
    }

    public synchronized void unregisterHandler(QMExecutionHandler handler) {
        if (!handlers.remove(handler)) {
            log.warn("QM handler '" + handler + "' isn't registered within QM controller");
        }
    }

    public void registerMetaListener(QMMetaListener metaListener)
    {
        metaHandler.addListener(metaListener);
    }

    public void unregisterMetaListener(QMMetaListener metaListener)
    {
        metaHandler.removeListener(metaListener);
    }

    DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

    List<QMExecutionHandler> getHandlers()
    {
        return handlers;
    }

    private class NotifyInvocationHandler implements InvocationHandler {

        public synchronized Object invoke(Object proxy, Method method, Object[] args)
        {
            if (method.getReturnType() == Void.TYPE && method.getName().startsWith("handle")) {
                for (QMExecutionHandler handler : getHandlers()) {
                    try {
                        method.invoke(handler, args);
                    } catch (InvocationTargetException e) {
                        log.debug("Error notifying QM handler '" + handler.getHandlerName() + "'", e.getTargetException());
                    } catch (Throwable e) {
                        log.debug("Internal error while notifying QM handler '" + handler.getHandlerName() + "'", e);
                    }
                }
                return null;
            } else if (method.getName().equals("getHandlerName")) {
                return "Default";
            } else {
                try {
                    return method.invoke(this, args);
                } catch (Throwable e) {
                    // just ignore it
                    log.debug("Error executing QM method " + method, e);
                    return null;
                }
            }
        }

    }

}
