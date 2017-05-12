/*******************************************************************************
 * Copyright 2017 Veronica Anokhina.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package ru.org.sevn.util;

import java.io.File;
import java.util.ArrayList;

public class FileUtil {
	public static File getExistsFile (File f, String name, String ... extensions) {
		for (String e : extensions) {
			String testName = name + e;
			File testFile = new File(f, testName); 
			if (testFile.exists()) {
				return testFile;
			}
		}
		return null;
	}
	private static String toRoot(File root, File file) {
		if (file != null && !file.isDirectory()) {
			file = file.getParentFile();
		}
		if (file == null || file.equals(root)) {
			return ".";
		}
		String ret = toRoot(root, file.getParentFile());
		if (ret.equals(".")) {
			return "..";
		} else { 
			return "../" + ret;
		}
	}
	private static File getRoot(File file, ArrayList<File> lst) {
		if (file != null && lst != null && lst.size() > 0) {
			for(File f: lst) {
				if (file.equals(f)) {
					return f;
				}
			}
			return getRoot(file.getParentFile(), lst);
		}
		return null;
	}
	private static ArrayList<File> getParents(File f, ArrayList<File> lst) {
		if (lst == null) {
			lst = new ArrayList<>();
		}
		File pf = f.getParentFile();
		if (pf != null) {
			getParents(pf, lst);
			lst.add(pf);
		}
		return lst;
	}
	public static String getRelativePath(File fromFile, File toFile) {
		if (toFile != null) {
			ArrayList<File> lst = getParents(fromFile, null);
			if(fromFile.isDirectory()) {
				lst.add(fromFile);
			}
			File rootFile = getRoot(toFile, lst);
			if (rootFile != null) {
				String ret = toRoot(rootFile, fromFile) + toFile.getAbsolutePath().replace(rootFile.getAbsolutePath(), "").replace("\\", "/");
				if (ret.startsWith("./")) {
					ret = ret.substring("./".length());
				}
				//System.err.println("---------href----"+fromFile+":"+toFile+":"+ret);
				return ret;
			}
		}
		return null;
	}


}
