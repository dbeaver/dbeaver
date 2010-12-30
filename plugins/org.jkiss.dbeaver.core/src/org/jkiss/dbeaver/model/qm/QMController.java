/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.qm.QMMetaEvent;
import org.jkiss.dbeaver.runtime.qm.QMMetaListener;
import org.jkiss.dbeaver.runtime.qm.meta.QMMCollector;

import java.util.List;

/**
 * Query manager controller
 */
public interface QMController {

    QMMCollector getMetaCollector();

    QMExecutionHandler getDefaultHandler();

    void registerHandler(QMExecutionHandler handler);

    void unregisterHandler(QMExecutionHandler handler);

    void registerMetaListener(QMMetaListener metaListener);

    void unregisterMetaListener(QMMetaListener metaListener);

    List<QMMetaEvent> getPastMetaEvents();
}
