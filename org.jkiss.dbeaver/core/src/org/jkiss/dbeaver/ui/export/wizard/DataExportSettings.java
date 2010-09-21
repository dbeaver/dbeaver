/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.wizard;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.program.Program;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.prop.DBPProperty;
import org.jkiss.dbeaver.model.prop.DBPPropertyGroup;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.dbeaver.utils.ContentUtils;

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

    private List<DBSDataContainer> dataProviders;
    private DataExporterDescriptor dataExporter;

    private ExtractType extractType = ExtractType.SINGLE_QUERY;
    private int segmentSize = DEFAULT_SEGMENT_SIZE;
    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private Map<String, String> extractorProperties = new HashMap<String, String>();

    private String outputFolder = System.getProperty("user.home");
    private String outputFilePattern = PATTERN_TABLE + "_" + PATTERN_TIMESTAMP;
    private String outputEncoding = ContentUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = true;

    private boolean compressResults = false;
    private boolean openNewConnections = true;
    private boolean queryRowCount = true;
    private boolean openFolderOnFinish = true;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    private Map<DataExporterDescriptor, Map<String,String>> exporterPropsHistory = new HashMap<DataExporterDescriptor, Map<String, String>>();

    private transient boolean folderOpened = false;
    private transient int curProviderNum = 0;

    public DataExportSettings(List<DBSDataContainer> dataProviders)
    {
        this.dataProviders = dataProviders;
    }

    public List<DBSDataContainer> getDataProviders()
    {
        return dataProviders;
    }

    public synchronized DBSDataContainer acquireDataProvider()
    {
        if (curProviderNum >= dataProviders.size()) {
            if (!folderOpened) {
                // Last one
                folderOpened = true;
                DBeaverCore.getDisplay().asyncExec(new Runnable() {
                    public void run() {
                        Program.launch(outputFolder);
                    }
                });
            }
            return null;
        }
        DBSDataContainer result = dataProviders.get(curProviderNum);

        curProviderNum++;
        return result;
    }

    public DataExporterDescriptor getDataExporter()
    {
        return dataExporter;
    }

    public void setDataExporter(DataExporterDescriptor dataExporter)
    {
        Map<String, String> historyProps = this.exporterPropsHistory.get(dataExporter);
        if (historyProps == null) {
            historyProps = new HashMap<String, String>();
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

    public Map<String, String> getExtractorProperties()
    {
        return extractorProperties;
    }

    public void setExtractorProperties(Map<String, String> extractorProperties)
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

    public void loadFrom(IDialogSettings dialogSettings)
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
        if (dialogSettings.get("lobExtractType") != null) {
            try {
                lobExtractType = LobExtractType.valueOf(dialogSettings.get("lobExtractType"));
            } catch (IllegalArgumentException e) {
                lobExtractType = LobExtractType.SKIP;
            }
        }
        if (dialogSettings.get("lobEncoding") != null) {
            try {
                lobEncoding = LobEncoding.valueOf(dialogSettings.get("lobEncoding"));
            } catch (IllegalArgumentException e) {
                lobEncoding = LobEncoding.HEX;
            }
        }

        if (dialogSettings.get("outputFolder") != null) {
            outputFolder = dialogSettings.get("outputFolder");
        }
        if (dialogSettings.get("outputFilePattern") != null) {
            outputFilePattern = dialogSettings.get("outputFilePattern");
        }
        if (dialogSettings.get("outputEncoding") != null) {
            outputEncoding = dialogSettings.get("outputEncoding");
        }
        if (dialogSettings.get("outputEncodingBOM") != null) {
            outputEncodingBOM = dialogSettings.getBoolean("outputEncodingBOM");
        }

        if (dialogSettings.get("compressResults") != null) {
            compressResults = dialogSettings.getBoolean("compressResults");
        }
        if (dialogSettings.get("openNewConnections") != null) {
            openNewConnections = dialogSettings.getBoolean("openNewConnections");
        }
        if (dialogSettings.get("queryRowCount") != null) {
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
                    Map<String, String> expProps = new HashMap<String, String>();
                    exporterPropsHistory.put(exporter, expProps);
                    for (DBPPropertyGroup group : exporter.getPropertyGroups()) {
                        for (DBPProperty prop : group.getProperties()) {
                            String value = expSection.get(prop.getId());
                            if (value != null) {
                                expProps.put(prop.getId(), value);
                            }
                        }
                    }
                }
            }
        }
        setDataExporter(dataExporter);
    }

    public void saveTo(IDialogSettings dialogSettings)
    {
        if (this.dataExporter != null) {
            this.exporterPropsHistory.put(this.dataExporter, this.extractorProperties);
            dialogSettings.put("exporter", dataExporter.getId());
        }

        dialogSettings.put("extractType", extractType.name());
        dialogSettings.put("segmentSize", segmentSize);
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
            Map<String, String> props = exporterPropsHistory.get(exp);
            if (props != null) {
                for (Map.Entry<String,String> prop : props.entrySet()) {
                    expSettings.put(prop.getKey(), prop.getValue());
                }
            }
        }

    }

}
