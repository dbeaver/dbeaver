package org.jkiss.dbeaver.ext.postgresql.pldbg;

public interface Variable<T> {
	
	T getVal();
	String getName();
	VariableType getType();

}
