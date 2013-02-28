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
package org.jkiss.dbeaver.tools.data.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.tools.data.DataTransferProducer;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Export settings
 */
public class DataExportSettings {

    enum ExtractType {
        SINGLE_QUERY,
        SEGMENTS
    }

    enum LobExtractType {
        SKIP,
        FILES,
        INLINE
    }

    enum LobEncoding {
        BASE64,
        HEX,
        BINARY
    }

    private static final int DEFAULT_SEGMENT_SIZE = 100000;
    private static final String PATTERN_TABLE = "{table}";
    private static final String PATTERN_TIMESTAMP = "{timestamp}";

    private static final int DEFAULT_THREADS_NUM = 1;

    private List<DataTransferProducer> dataProducers;
    private DataExporterDescriptor dataExporter;

    private ExtractType extractType = ExtractType.SINGLE_QUERY;
    private int segmentSize = DEFAULT_SEGMENT_SIZE;
    private DBDDataFormatterProfile formatterProfile;
    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private Map<Object, Object> extractorProperties = new HashMap<Object, Object>();

    private String outputFolder = System.getProperty("user.home");
    private String outputFilePattern = PATTERN_TABLE + "_" + PATTERN_TIMESTAMP;
    private String outputEncoding = ContentUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = true;

    private boolean compressResults = false;
    private boolean openNewConnections = true;
    private boolean queryRowCount = true;
    private boolean openFolderOnFinish = true;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private Map<DataExporterDescriptor, Map<Object,Object>> exporterPropsHistory = new HashMap<DataExporterDescriptor, Map<Object, Object>>();

    private transient boolean folderOpened = false;
    private transient int curProviderNum = 0;

    public DataExportSettings(List<DataTransferProducer> dataProducers)
    {
        this.dataProducers = dataProducers;
    }

    public List<DataTransferProducer> getDataProducers()
    {
        return dataProducers;
    }

    public synchronized DataTransferProducer acquireDataProvider()
    {
        if (curProviderNum >= dataProducers.size()) {
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
        DataTransferProducer result = dataProducers.get(curProviderNum);

        curProviderNum++;
        return result;
    }

    public DataExporterDescriptor getDataExporter()
    {
        return dataExporter;
    }

    public void setDataExporter(DataExporterDescriptor dataExporter)
    {
        Map<Object, Object> historyProps = this.exporterPropsHistory.get(dataExporter);
        if (historyProps == null) {
            historyProps = new HashMap<Object, Object>();
        }
        if (this.dataExporter != null) {
            this.exporterPropsHistory.put(this.dataExporter, this.extractorProperties);
        }
        this.dataExporter = dataExporter;
        this.extractorProperties = historyProps;
    }

    public ExtractType getExtractType()
    {
        return extractType;
    }

    public void setExtractType(ExtractType extractType)
    {
        this.extractType = extractType;
    }

    public int getSegmentSize()
    {
        return segmentSize;
    }

    public void setSegmentSize(int segmentSize)
    {
        if (segmentSize > 0) {
            this.segmentSize = segmentSize;
        }
    }

    public DBDDataFormatterProfile getFormatterProfile()
    {
        return formatterProfile;
    }

    public void setFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }

    public LobExtractType getLobExtractType()
    {
        return lobExtractType;
    }

    public void setLobExtractType(LobExtractType lobExtractType)
    {
        this.lobExtractType = lobExtractType;
    }

    public LobEncoding getLobEncoding()
    {
        return lobEncoding;
    }

    public void setLobEncoding(LobEncoding lobEncoding)
    {
        this.lobEncoding = lobEncoding;
    }

    public Map<Object, Object> getExtractorProperties()
    {
        return extractorProperties;
    }

    public void setExtractorProperties(Map<Object, Object> extractorProperties)
    {
        this.extractorProperties = extractorProperties;
    }

    public String getOutputFolder()
    {
        return outputFolder;
    }

    public void setOutputFolder(String outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    public String getOutputFilePattern()
    {
        return outputFilePattern;
    }

    public void setOutputFilePattern(String outputFilePattern)
    {
        this.outputFilePattern = outputFilePattern;
    }

    public String getOutputEncoding()
    {
        return outputEncoding;
    }

    public void setOutputEncoding(String outputEncoding)
    {
        this.outputEncoding = outputEncoding;
    }

    public boolean isOutputEncodingBOM()
    {
        return outputEncodingBOM;
    }

    public void setOutputEncodingBOM(boolean outputEncodingBOM)
    {
        this.outputEncodingBOM = outputEncodingBOM;
    }

    public boolean isCompressResults()
    {
        return compressResults;
    }

    public void setCompressResults(boolean compressResults)
    {
        this.compressResults = compressResults;
    }

    public boolean isQueryRowCount()
    {
        return queryRowCount;
    }

    public void setQueryRowCount(boolean queryRowCount)
    {
        this.queryRowCount = queryRowCount;
    }

    public boolean isOpenNewConnections()
    {
        return openNewConnections;
    }

    public void setOpenNewConnections(boolean openNewConnections)
    {
        this.openNewConnections = openNewConnections;
    }

    public boolean isOpenFolderOnFinish()
    {
        return openFolderOnFinish;
    }

    public void setOpenFolderOnFinish(boolean openFolderOnFinish)
    {
        this.openFolderOnFinish = openFolderOnFinish;
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

    public String getOutputFileName(DBPNamedObject source)
    {
        return processTemplate(stripObjectName(source.getName())) + "." + dataExporter.getFileExtension();
    }

    public File makeOutputFile(DBPNamedObject source)
    {
        File dir = new File(outputFolder);
        String fileName = getOutputFileName(source);
        if (compressResults) {
            fileName += ".zip";
        }
        return new File(dir, fileName);
    }

    private String processTemplate(String tableName)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmm").format(new Date());
        return outputFilePattern.replaceAll("\\{table\\}", tableName).replaceAll("\\{timestamp\\}", timeStamp);
    }

    private static String stripObjectName(String name)
    {
        StringBuilder result = new StringBuilder();
        boolean lastUnd = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isJavaIdentifierPart(c)) {
                result.append(c);
                lastUnd = false;
            } else if (!lastUnd) {
                result.append('_');
                lastUnd = true;
            }
            if (result.length() >= 64) {
                break;
            }
        }
        return result.toString();
    }

    void loadFrom(IDialogSettings dialogSettings)
    {
        DataExporterDescriptor dataExporter = null;
        String expId = dialogSettings.get("exporter");
        if (expId != null) {
            dataExporter = DBeaverCore.getInstance().getDataExportersRegistry().getDataExporter(expId);
        }

        if (dialogSettings.get("extractType") != null) {
            try {
                extractType = ExtractType.valueOf(dialogSettings.get("extractType"));
            } catch (IllegalArgumentException e) {
                extractType = ExtractType.SINGLE_QUERY;
            }
        }
        try {
            segmentSize = dialogSettings.getInt("segmentSize");
        } catch (NumberFormatException e) {
            segmentSize = DEFAULT_SEGMENT_SIZE;
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("formatterProfile"))) {
            formatterProfile = DBeaverCore.getInstance().getDataFormatterRegistry().getCustomProfile(dialogSettings.get("formatterProfile"));
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("lobExtractType"))) {
            try {
                lobExtractType = LobExtractType.valueOf(dialogSettings.get("lobExtractType"));
            } catch (IllegalArgumentException e) {
                lobExtractType = LobExtractType.SKIP;
            }
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("lobEncoding"))) {
            try {
                lobEncoding = LobEncoding.valueOf(dialogSettings.get("lobEncoding"));
            } catch (IllegalArgumentException e) {
                lobEncoding = LobEncoding.HEX;
            }
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("outputFolder"))) {
            outputFolder = dialogSettings.get("outputFolder");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputFilePattern"))) {
            outputFilePattern = dialogSettings.get("outputFilePattern");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputEncoding"))) {
            outputEncoding = dialogSettings.get("outputEncoding");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("outputEncodingBOM"))) {
            outputEncodingBOM = dialogSettings.getBoolean("outputEncodingBOM");
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("compressResults"))) {
            compressResults = dialogSettings.getBoolean("compressResults");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("openNewConnections"))) {
            openNewConnections = dialogSettings.getBoolean("openNewConnections");
        }
        if (!CommonUtils.isEmpty(dialogSettings.get("queryRowCount"))) {
            queryRowCount = dialogSettings.getBoolean("queryRowCount");
        }
        try {
            maxJobCount = dialogSettings.getInt("maxJobCount");
        } catch (NumberFormatException e) {
            maxJobCount = DEFAULT_THREADS_NUM;
        }
        if (dialogSettings.get("openFolderOnFinish") != null) {
            openFolderOnFinish = dialogSettings.getBoolean("openFolderOnFinish");
        }

        IDialogSettings[] expSections = dialogSettings.getSections();
        if (expSections != null && expSections.length > 0) {
            for (IDialogSettings expSection : expSections) {
                expId = expSection.getName();
                DataExporterDescriptor exporter = DBeaverCore.getInstance().getDataExportersRegistry().getDataExporter(expId);
                if (exporter != null) {
                    Map<Object, Object> expProps = new HashMap<Object, Object>();
                    exporterPropsHistory.put(exporter, expProps);
                    for (IPropertyDescriptor prop : exporter.getProperties()) {
                        Object value = expSection.get(prop.getId().toString());
                        if (value != null) {
                            if ("extractImages".equals(prop.getId())) {
                                value = Boolean.parseBoolean((String) value);
                            }
                            expProps.put(prop.getId(), value);
                        }
                    }
                }
            }
        }
        setDataExporter(dataExporter);
    }

    void saveTo(IDialogSettings dialogSettings)
    {
        if (this.dataExporter != null) {
            this.exporterPropsHistory.put(this.dataExporter, this.extractorProperties);
            dialogSettings.put("exporter", dataExporter.getId());
        }

        dialogSettings.put("extractType", extractType.name());
        dialogSettings.put("segmentSize", segmentSize);
        if (formatterProfile != null) {
            dialogSettings.put("formatterProfile", formatterProfile.getProfileName());
        } else {
            dialogSettings.put("formatterProfile", "");
        }
        dialogSettings.put("lobExtractType", lobExtractType.name());
        dialogSettings.put("lobEncoding", lobEncoding.name());

        dialogSettings.put("outputFolder", outputFolder);
        dialogSettings.put("outputFilePattern", outputFilePattern);
        dialogSettings.put("outputEncoding", outputEncoding);
        dialogSettings.put("outputEncodingBOM", outputEncodingBOM);

        dialogSettings.put("compressResults", compressResults);
        dialogSettings.put("openNewConnections", openNewConnections);
        dialogSettings.put("queryRowCount", queryRowCount);
        dialogSettings.put("maxJobCount", maxJobCount);
        dialogSettings.put("openFolderOnFinish", openFolderOnFinish);

        for (DataExporterDescriptor exp : exporterPropsHistory.keySet()) {
            IDialogSettings expSettings = dialogSettings.getSection(exp.getName());
            if (expSettings == null) {
                expSettings = dialogSettings.addNewSection(exp.getId());
            }
            Map<Object, Object> props = exporterPropsHistory.get(exp);
            if (props != null) {
                for (Map.Entry<Object,Object> prop : props.entrySet()) {
                    expSettings.put(CommonUtils.toString(prop.getKey()), CommonUtils.toString(prop.getValue()));
                }
            }
        }

    }

}
