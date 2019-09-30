package org.jkiss.dbeaver.ui;

public interface IHelpContextIdProvider {

	/**
     * Return the help context id that should be used in place of the given help context id.
	 * @return the help context id that should be used in place of the given help context id
	 * or <code>null</code> if default is to be used
	 */
	String getHelpContextId();
}
