/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.transfer.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class DataTransferHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection ss = (IStructuredSelection)selection;
        final List<IDataTransferProducer> producers = new ArrayList<>();
        final List<IDataTransferConsumer> consumers = new ArrayList<>();
        for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
            Object object = iter.next();

            IDataTransferNode node = adaptTransferNode(object);
            if (node instanceof IDataTransferProducer) {
                producers.add((IDataTransferProducer) node);
            } else if (node instanceof IDataTransferConsumer) {
                consumers.add((IDataTransferConsumer) node);
            }
        }

        if (!consumers.isEmpty()) {
            // We need to choose producer for consumers
            for (IDataTransferConsumer consumer : consumers) {
                IDataTransferProducer producer = chooseProducer(event, consumer);
                if (producer == null) {
                    return null;
                }
                producers.add(producer);
            }
        }

        // Run transfer wizard
        if (!producers.isEmpty() || !consumers.isEmpty()) {
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                workbenchWindow,
                new DataTransferWizard(
                    producers.toArray(new IDataTransferProducer[producers.size()]),
                    consumers.toArray(new IDataTransferConsumer[consumers.size()])));
            dialog.open();
        }

        return null;
    }

    protected IDataTransferProducer chooseProducer(ExecutionEvent event, IDataTransferConsumer consumer)
    {
        return null;
    }

    protected abstract IDataTransferNode adaptTransferNode(Object object);
}