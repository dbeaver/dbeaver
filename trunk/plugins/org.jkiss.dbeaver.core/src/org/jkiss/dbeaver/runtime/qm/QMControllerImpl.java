/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
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

    static final Log log = Log.getLog(QMControllerImpl.class);

    private QMExecutionHandler defaultHandler;
    private QMMCollector metaHandler;
    private List<QMExecutionHandler> handlers = new ArrayList<QMExecutionHandler>();

    public QMControllerImpl() {
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
    public synchronized void registerHandler(QMExecutionHandler handler) {
        handlers.add(handler);
    }

    @Override
    public synchronized void unregisterHandler(QMExecutionHandler handler) {
        if (!handlers.remove(handler)) {
            log.warn("QM handler '" + handler + "' isn't registered within QM controller");
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
        return handlers;
    }

    private class NotifyInvocationHandler implements InvocationHandler {

        @Override
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
