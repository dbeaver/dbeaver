package org.jkiss.dbeaver.ext.test.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class IconRenamer {

	public static void main(String[] args) throws Exception {
		//fixIconSet1();
		fixIconSet2();
	}

	private static void fixIconSet1() {
		File[] icons = new File("C:\\devel\\my\\ext\\new-icons\\Icons_set1\\").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".png");
			}
		});
		if (icons != null) {
			for (File icon : icons) {
				String fileName = icon.getName();
				fileName = fileName.substring(6);
				int divPos = fileName.lastIndexOf('_');
				fileName = fileName.substring(0, divPos) + fileName.substring(divPos + 6);
				fileName = fileName.toLowerCase();
				System.out.println(fileName);

				icon.renameTo(new File(icon.getParent(), fileName));
			}
		}
	}

	private static void fixIconSet2() throws Exception {
		File dir = new File("C:\\devel\\my\\dbeaver-packaging\\docs\\branding3\\Icons\\tree\\");
		File[] icons = dir.listFiles((d, name) -> name.endsWith(".png"));
		if (icons != null) {
			for (File icon : icons) {
				final String fileName = icon.getName().toLowerCase();
				if (fileName.lastIndexOf('@') != -1) {
					continue;
				}
				int divPos = fileName.lastIndexOf('.');
				String targetName;
				if (divPos != -1) {
					targetName = fileName.substring(0, divPos) + "@2x.png";
				} else {
					targetName = fileName;
				}
				System.out.println(fileName + "->" + targetName);

				//icon.renameTo(new File(icon.getParent(), targetName));
				Process process = Runtime.getRuntime().exec(
					new String[]{"git", "mv", fileName, targetName},
					null,
					dir);
				process.waitFor();
			}
		}
	}

}
