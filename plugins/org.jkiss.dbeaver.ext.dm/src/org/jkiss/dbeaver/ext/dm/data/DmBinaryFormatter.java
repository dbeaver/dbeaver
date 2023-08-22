package org.jkiss.dbeaver.ext.dm.data;

import org.jkiss.dbeaver.model.impl.data.formatters.BinaryFormatterHex;

public class DmBinaryFormatter extends BinaryFormatterHex {

	public static final DmBinaryFormatter INSTANCE = new DmBinaryFormatter();

	private static final String HEX_PREFIX = "'";
	private static final String HEX_POSTFIX = "'";

	public String getId() {
		return "dmhex";
	}

	public String getTitle() {
		return "dm Hex";
	}

	public String toString(byte[] bytes, int offset, int length) {
		return HEX_PREFIX + super.toString(bytes, offset, length) + HEX_POSTFIX;
	}

	public byte[] toBytes(String string) {
		if (string.startsWith(HEX_PREFIX)) {
			string = string.substring(HEX_PREFIX.length(), string.length() - HEX_POSTFIX.length());
		}
		return super.toBytes(string);
	}
}
