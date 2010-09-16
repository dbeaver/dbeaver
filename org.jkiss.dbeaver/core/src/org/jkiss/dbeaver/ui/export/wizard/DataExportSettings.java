package org.jkiss.dbeaver.ui.export.wizard;

import org.jkiss.dbeaver.ext.IResultSetProvider;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;

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

    private static final int DEFAULT_THREADS_NUM = 5;

    private List<IResultSetProvider> dataProviders;
    private DataExporterDescriptor dataExporter;

    private ExtractType extractType = ExtractType.SINGLE_QUERY;
    private int segmentSize = DEFAULT_SEGMENT_SIZE;
    private LobExtractType lobExtractType = LobExtractType.SKIP;
    private LobEncoding lobEncoding = LobEncoding.HEX;

    private Map<String, Object> extractorProperties = new HashMap<String, Object>();

    private String outputFolder = System.getProperty("user.home");
    private String outputFilePattern = PATTERN_TABLE + "_" + PATTERN_TIMESTAMP;
    private String outputEncoding = System.getProperty("file.encoding");

    private boolean compressResults = false;
    private boolean queryRowCount = true;
    private int maxJobCount = DEFAULT_THREADS_NUM;

    public DataExportSettings(List<IResultSetProvider> dataProviders)
    {
        this.dataProviders = dataProviders;
    }

    public List<IResultSetProvider> getDataProviders()
    {
        return dataProviders;
    }

    public DataExporterDescriptor getDataExporter()
    {
        return dataExporter;
    }

    public void setDataExporter(DataExporterDescriptor dataExporter)
    {
        this.dataExporter = dataExporter;
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
        this.segmentSize = segmentSize;
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

    public Map<String, Object> getExtractorProperties()
    {
        return extractorProperties;
    }

    public void setExtractorProperties(Map<String, Object> extractorProperties)
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

    public int getMaxJobCount()
    {
        return maxJobCount;
    }

    public void setMaxJobCount(int maxJobCount)
    {
        this.maxJobCount = maxJobCount;
    }
}
