package org.jkiss.dbeaver.model.lsm.interfaces;

public interface LSMObject<T> {
    @SuppressWarnings("unchecked")
    default <T2 extends T> T2 coerce(Class<T2> desired) {
        if (desired.isAssignableFrom(this.getClass())) {
            return (T2)this;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
