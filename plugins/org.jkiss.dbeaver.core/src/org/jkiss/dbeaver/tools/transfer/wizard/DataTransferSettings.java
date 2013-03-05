/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Export settings
 */
public class DataTransferSettings {

    private static final int DEFAULT_THREADS_NUM = 1;

    private static class SubSettings {
        IDataTransferNode sourceNode;
        IDataTransferSettings settings;
        IWizardPage[] pages;

        private SubSettings(IDataTransferNode sourceNode)
        {
            this.sourceNode = sourceNode;
            this.settings = sourceNode.createSettings();
            this.pages = sourceNode.createWizardPages();
        }
    }

    private List<DataTransferPipe> dataPipes;
    private IDataTransferConsumer consumer;
    private IDataTransferProcessor processor;
    private Map<Class, SubSettings> nodeSettings = new LinkedHashMap<Class, SubSettings>();

    private int maxJobCount = DEFAULT_THREADS_NUM;

    private transient int curPipeNum = 0;

    public DataTransferSettings(List<? extends IDataTransferProducer> dataProducers)
    {
        this(dataProducers, null);
    }

    public DataTransferSettings(List<? extends IDataTransferProducer> producers, List<? extends IDataTransferConsumer> consumers)
    {
        dataPipes = new ArrayList<DataTransferPipe>();
        if (!CommonUtils.isEmpty(producers)) {
            for (IDataTransferProducer producer : producers) {
                dataPipes.add(new DataTransferPipe(producer, null));
            }
        } else if (!CommonUtils.isEmpty(consumers)) {
            for (IDataTransferConsumer consumer : consumers) {
                dataPipes.add(new DataTransferPipe(null, consumer));
            }
        } else if (producers.size() == consumers.size()) {
            for (int i = 0; i < producers.size(); i++) {
                dataPipes.add(new DataTransferPipe(producers.get(i), consumers.get(i)));
            }
        } else {
            throw new IllegalArgumentException("Producers must match consumers or must be empty");
        }
        for (DataTransferPipe pipe : dataPipes) {
            addNodeSettings(pipe.getProducer());
            addNodeSettings(pipe.getConsumer());
        }
    }

    private void addNodeSettings(IDataTransferNode node)
    {
        if (node == null) {
            return;
        }
        Class<? extends IDataTransferNode> nodeClass = node.getClass();
        if (nodeSettings.containsKey(nodeClass)) {
            return;
        }
        nodeSettings.put(nodeClass, new SubSettings(node));
    }

    void addWizardPages(DataTransferWizard wizard)
    {
        for (SubSettings subSettings : nodeSettings.values()) {
            if (subSettings.pages != null) {
                for (IWizardPage page : subSettings.pages) {
                    wizard.addPage(page);
                }
            }
        }
    }

    public IDataTransferSettings getPageSettings(IWizardPage page)
    {
        for (SubSettings subSettings : nodeSettings.values()) {
            if (subSettings.pages != null) {
                for (IWizardPage nodePage : subSettings.pages) {
                    if (nodePage == page) {
                        return subSettings.settings;
                    }
                }
            }
        }
        return null;
    }

    public IDataTransferSettings getNodeSettings(IDataTransferNode node)
    {
        SubSettings subSettings = nodeSettings.get(node.getClass());
        return subSettings == null ? null : subSettings.settings;
    }

    public List<DataTransferPipe> getDataPipes()
    {
        return dataPipes;
    }

    public synchronized DataTransferPipe acquireDataPipe()
    {
/*
        if (curPipeNum >= dataPipes.size()) {
            if (!folderOpened && openFolderOnFinish) {
                // Last one
                folderOpened = true;
                DBeaverUI.getDisplay().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        RuntimeUtils.launchProgram(outputFolder);
                    }
                });
            }
            return null;
        }
*/
        DataTransferPipe result = dataPipes.get(curPipeNum);

        curPipeNum++;
        return result;
    }

    public IDataTransferConsumer getConsumer()
    {
        return consumer;
    }

    void setConsumer(IDataTransferConsumer consumer)
    {
        this.consumer = consumer;
    }

    public IDataTransferProcessor getProcessor()
    {
        return processor;
    }

    void setProcessor(IDataTransferProcessor processor)
    {
        this.processor = processor;
    }

    IDataTransferConsumer[] getAvailableConsumers()
    {
        return new IDataTransferConsumer[] {
            new StreamTransferConsumer()
        };
    }

    public int getMaxJobCount()
    {
        return maxJobCount;
    }

    public void setMaxJobCount(int maxJobCount)
    {
        if (maxJobCount > 0) {
            this.maxJobCount = maxJobCount;
        }
    }

    void loadFrom(IDialogSettings dialogSettings)
    {
        try {
            maxJobCount = dialogSettings.getInt("maxJobCount");
        } catch (NumberFormatException e) {
            maxJobCount = DEFAULT_THREADS_NUM;
        }
        // Load nodes' settings
        for (Map.Entry<Class, SubSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = dialogSettings.addNewSection(entry.getKey().getSimpleName());
            entry.getValue().settings.loadSettings(nodeSection);
        }
    }

    void saveTo(IDialogSettings dialogSettings)
    {
        dialogSettings.put("maxJobCount", maxJobCount);
        // Save nodes' settings
        for (Map.Entry<Class, SubSettings> entry : nodeSettings.entrySet()) {
            IDialogSettings nodeSection = dialogSettings.addNewSection(entry.getKey().getSimpleName());
            entry.getValue().settings.saveSettings(nodeSection);
        }
    }

}
