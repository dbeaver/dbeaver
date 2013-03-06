package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Stream transfer settings
 */
public class StreamConsumerSettings implements IDataTransferSettings {

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

    private static final String PATTERN_TABLE = "{table}";
    private static final String PATTERN_TIMESTAMP = "{timestamp}";

    public static final String PROP_EXTRACT_IMAGES = "extractImages";
    public static final String PROP_FILE_EXTENSION = "extension";
    public static final String PROP_FORMAT = "format";

    //private Map<Object, Object> processorProperties = new HashMap<Object, Object>();

    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private String outputFolder = System.getProperty("user.home");
    private String outputFilePattern = PATTERN_TABLE + "_" + PATTERN_TIMESTAMP;
    private String outputEncoding = ContentUtils.getDefaultFileEncoding();
    private boolean outputEncodingBOM = true;

    private DBDDataFormatterProfile formatterProfile;

    private boolean compressResults = false;
    private boolean openFolderOnFinish = true;

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

    public boolean isOpenFolderOnFinish()
    {
        return openFolderOnFinish;
    }

    public void setOpenFolderOnFinish(boolean openFolderOnFinish)
    {
        this.openFolderOnFinish = openFolderOnFinish;
    }

    public DBDDataFormatterProfile getFormatterProfile()
    {
        return formatterProfile;
    }

    public void setFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        this.formatterProfile = formatterProfile;
    }
/*

    public IStreamDataExporterDescriptor getExporterDescriptor()
    {
        return dataExporter;
    }

    public void setExporterDescriptor(IStreamDataExporterDescriptor dataExporter)
    {
        Map<Object, Object> historyProps = this.exporterPropsHistory.get(dataExporter);
        if (historyProps == null) {
            historyProps = new HashMap<Object, Object>();
        }
        if (this.dataExporter != null) {
            this.exporterPropsHistory.put(this.dataExporter, this.processorProperties);
        }
        this.dataExporter = dataExporter;
        this.processorProperties = historyProps;
    }
*/

    @Override
    public void loadSettings(IDialogSettings dialogSettings)
    {
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
        if (dialogSettings.get("openFolderOnFinish") != null) {
            openFolderOnFinish = dialogSettings.getBoolean("openFolderOnFinish");
        }

        if (!CommonUtils.isEmpty(dialogSettings.get("formatterProfile"))) {
            formatterProfile = DBeaverCore.getInstance().getDataFormatterRegistry().getCustomProfile(dialogSettings.get("formatterProfile"));
        }
/*
        IDialogSettings[] expSections = dialogSettings.getSections();
        if (expSections != null && expSections.length > 0) {
            for (IDialogSettings expSection : expSections) {
                expId = expSection.getName();
                IStreamDataExporterDescriptor exporter = DBeaverCore.getInstance().getDataTransferRegistry().getDataExporter(expId);
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
        setExporterDescriptor(dataExporter);
*/
    }

    @Override
    public void saveSettings(IDialogSettings dialogSettings)
    {
/*
        if (this.dataExporter != null) {
            this.exporterPropsHistory.put(this.dataExporter, this.processorProperties);
            dialogSettings.put("exporter", dataExporter.getId());
        }
*/

        dialogSettings.put("lobExtractType", lobExtractType.name());
        dialogSettings.put("lobEncoding", lobEncoding.name());

        dialogSettings.put("outputFolder", outputFolder);
        dialogSettings.put("outputFilePattern", outputFilePattern);
        dialogSettings.put("outputEncoding", outputEncoding);
        dialogSettings.put("outputEncodingBOM", outputEncodingBOM);

        dialogSettings.put("compressResults", compressResults);
        dialogSettings.put("openFolderOnFinish", openFolderOnFinish);

        if (formatterProfile != null) {
            dialogSettings.put("formatterProfile", formatterProfile.getProfileName());
        } else {
            dialogSettings.put("formatterProfile", "");
        }

/*
        for (IStreamDataExporterDescriptor exp : exporterPropsHistory.keySet()) {
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
*/

    }

}
