package org.jkiss.dbeaver.tools.data;

import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;

/**
 * Transfer settings
 */
public interface IDataTransferSettings {

    enum ExtractType {
        SINGLE_QUERY,
        SEGMENTS
    }

    ExtractType getExtractType();

    int getSegmentSize();

    boolean isQueryRowCount();

    DBDDataFormatterProfile getFormatterProfile();

}
