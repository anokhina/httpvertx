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
package ru.org.sevn.templ;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

/*
		VelocityEngine ve = new VelocityEngine();
		ClasspathVelocityEngine.applyClasspathResourceLoader(ve);
		ve.init();
 */
public class ClasspathVelocityEngine extends VelocityEngine {
	public ClasspathVelocityEngine() {
		applyClasspathResourceLoader(this);
	}
	
	public static VelocityEngine applyClasspathResourceLoader(VelocityEngine ve) {
	    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
	    ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
	    ve.setProperty("input.encoding", "UTF-8");
	    return ve;
	}
	public static VelocityEngine applyFileResourceLoader(VelocityEngine ve, String path) {
	    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
	    ve.setProperty("file.resource.loader.class", FileResourceLoader.class.getName());
	    ve.setProperty("file.resource.loader.path", path);
	    ve.setProperty("file.resource.loader.cache", false);
	    ve.setProperty("input.encoding", "UTF-8");
	    return ve;
	}
}
