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

package org.jkiss.dbeaver.model.qm;

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
