/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm;

import java.util.Collection;

/**
 * DBC meta events listener
 */
public interface QMMetaListener {

    void metaInfoChanged(Collection<QMMetaEvent> events);

}
