package org.jkiss.dbeaver.model.preferences;

import java.util.EventListener;
import java.util.EventObject;

public interface DBPPreferenceListener extends EventListener {

    class PreferenceChangeEvent extends EventObject {

        private String propertyName;
        private Object oldValue;
        private Object newValue;

        public PreferenceChangeEvent(Object source, String property, Object oldValue,
                                     Object newValue) {
            super(source);
            this.propertyName = property;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getProperty() {
            return propertyName;
        }
        public Object getNewValue() {
            return newValue;
        }
        public Object getOldValue() {
            return oldValue;
        }
    }


    void preferenceChange(PreferenceChangeEvent event);
}
