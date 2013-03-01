package org.jkiss.dbeaver.tools.transfer.stream;

import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;

import java.util.Map;

/**
 * Stream transfer settings
 */
public interface IStreamTransferSettings extends IDataTransferSettings {

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

    Map<Object, Object> getExtractorProperties();

    LobExtractType getLobExtractType();

    LobEncoding getLobEncoding();

    boolean isCompressResults();

    boolean isOpenFolderOnFinish();

    String getOutputEncoding();

    boolean isOutputEncodingBOM();

    String getOutputFolder();

    String getOutputFilePattern();

    IStreamDataExporterDescriptor getExporterDescriptor();
}
