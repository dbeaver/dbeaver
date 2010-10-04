package org.jkiss.dbeaver.runtime.qm.meta;

/**
 * DBC meta events listener
 */
public interface QMMetaListener {

    enum Action {
        ADD,
        REMOVE,
        UPDATE
    }

    void metaInfoChanged(QMMObject object, Action action);

}
