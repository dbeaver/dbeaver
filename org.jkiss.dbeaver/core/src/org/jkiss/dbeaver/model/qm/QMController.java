package org.jkiss.dbeaver.model.qm;

/**
 * Query manager controller
 */
public interface QMController {

    QMExecutionHandler getDefaultHandler();

    void registerHandler(QMExecutionHandler handler);

    void unregisterHandler(QMExecutionHandler handler);

}
