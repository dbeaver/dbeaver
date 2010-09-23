package org.jkiss.dbeaver.runtime.qm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.qm.QMController;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.registry.DataSourceRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * QMController default implementation
 */
public class QMControllerImpl implements QMController {

    static final Log log = LogFactory.getLog(QMControllerImpl.class);

    private QMExecutionHandlerImpl defaultHandler;
    private List<QMExecutionHandler> handlers = new ArrayList<QMExecutionHandler>();
    private DataSourceRegistry dataSourceRegistry;

    public QMControllerImpl(DataSourceRegistry dataSourceRegistry) {
        this.dataSourceRegistry = dataSourceRegistry;
        defaultHandler = new QMExecutionHandlerImpl(this);
    }

    public void dispose()
    {
        if (!handlers.isEmpty()) {
            log.warn("Some QM handlers are still registered: " + handlers);
            handlers.clear();
        }
      	defaultHandler.dispose();
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

    DataSourceRegistry getDataSourceRegistry() {
        return dataSourceRegistry;
    }

    List<QMExecutionHandler> getHandlers()
    {
        return handlers;
    }

}
