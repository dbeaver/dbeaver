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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

/**
 * Data consumer
 */
public interface IDataTransferConsumer<SETTINGS extends IDataTransferSettings, PROCESSOR extends IDataTransferProcessor>
    extends IDataTransferNode<SETTINGS>, DBDDataReceiver
{

    void initTransfer(DBSObject sourceObject, SETTINGS settings, PROCESSOR processor, Map<Object, Object> processorProperties);

    void startTransfer(DBRProgressMonitor monitor) throws DBException;

    /**
     * Finishes this transfer
     * @param monitor monitor
     * @param last called in the very end of all transfers
     */
    void finishTransfer(DBRProgressMonitor monitor, boolean last);

    String getTargetName();

}
