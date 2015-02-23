/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.util.List;

/**
 * Compare settings
 */
public class CompareObjectsSettings {

    public enum OutputType {
        BROWSER("Open in browser"),
        FILE("Save to file");
        private final String title;

        private OutputType(String title)
        {
            this.title = title;
        }

        public String getTitle()
        {
            return title;
        }
    }

    private final List<DBNDatabaseNode> nodes;
    private boolean skipSystemObjects = true;
    private boolean compareLazyProperties = false;
    private boolean compareOnlyStructure = false;
    private boolean showOnlyDifferences = false;
    private OutputType outputType = OutputType.BROWSER;
    private String outputFolder = ContentUtils.getCurDialogFolder();

    public CompareObjectsSettings(List<DBNDatabaseNode> nodes)
    {
        this.nodes = nodes;
    }

    public List<DBNDatabaseNode> getNodes()
    {
        return nodes;
    }

    public boolean isSkipSystemObjects()
    {
        return skipSystemObjects;
    }

    public void setSkipSystemObjects(boolean skipSystemObjects)
    {
        this.skipSystemObjects = skipSystemObjects;
    }

    public boolean isCompareLazyProperties()
    {
        return compareLazyProperties;
    }

    public void setCompareLazyProperties(boolean compareLazyProperties)
    {
        this.compareLazyProperties = compareLazyProperties;
    }

    public boolean isCompareOnlyStructure()
    {
        return compareOnlyStructure;
    }

    public void setCompareOnlyStructure(boolean compareOnlyStructure)
    {
        this.compareOnlyStructure = compareOnlyStructure;
    }

    public boolean isShowOnlyDifferences()
    {
        return showOnlyDifferences;
    }

    public void setShowOnlyDifferences(boolean showOnlyDifferences)
    {
        this.showOnlyDifferences = showOnlyDifferences;
    }

    public OutputType getOutputType()
    {
        return outputType;
    }

    public void setOutputType(OutputType outputType)
    {
        this.outputType = outputType;
    }

    public String getOutputFolder()
    {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    void loadFrom(IDialogSettings dialogSettings)
    {
        if (dialogSettings.get("skipSystem") != null) {
            skipSystemObjects = dialogSettings.getBoolean("skipSystem");
        }
        if (dialogSettings.get("compareLazy") != null) {
            compareLazyProperties = dialogSettings.getBoolean("compareLazy");
        }
        if (dialogSettings.get("compareStructure") != null) {
            compareOnlyStructure = dialogSettings.getBoolean("compareStructure");
        }
        if (dialogSettings.get("showDifference") != null) {
            showOnlyDifferences = dialogSettings.getBoolean("showDifference");
        }
        if (dialogSettings.get("outputType") != null) {
            outputType = OutputType.valueOf(dialogSettings.get("outputType"));
        }
        if (dialogSettings.get("outputFolder") != null) {
            outputFolder = dialogSettings.get("outputFolder");
        }
    }

    void saveTo(IDialogSettings dialogSettings)
    {
        dialogSettings.put("skipSystem", skipSystemObjects);
        dialogSettings.put("compareLazy", compareLazyProperties);
        dialogSettings.put("compareStructure", compareOnlyStructure);
        dialogSettings.put("showDifference", showOnlyDifferences);
        dialogSettings.put("outputType", outputType.name());
        dialogSettings.put("outputFolder", outputFolder);
    }

}
