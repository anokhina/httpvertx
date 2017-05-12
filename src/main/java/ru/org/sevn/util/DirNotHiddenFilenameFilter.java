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
import java.io.FilenameFilter;

public class DirNotHiddenFilenameFilter implements FilenameFilter {
	
	@Override
	public boolean accept(File dir, String name) {
		if (name.startsWith(".")) {
			return false;
		}
		return new File(dir, name).isDirectory();
	}
	

}
