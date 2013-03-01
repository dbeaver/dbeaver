package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;

/**
 * Transfer settings
 */
public interface IDataTransferSettings {

    enum ExtractType {
        SINGLE_QUERY,
        SEGMENTS
    }

    boolean isOpenNewConnections();

    ExtractType getExtractType();

    int getSegmentSize();

    boolean isQueryRowCount();

    DBDDataFormatterProfile getFormatterProfile();

}
