/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.List;

/**
 * DBC meta events listener
 */
public interface QMMetaListener {

    void metaInfoChanged(List<QMMetaEvent> events);

}
