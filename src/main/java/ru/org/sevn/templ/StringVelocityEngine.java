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

import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResource;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;

/*
		StringVelocityEngine strVe = new StringVelocityEngine();
		strVe.init();
	    strVe.putStringResource("strTempl", "Hello $w");
	    
 */
public class StringVelocityEngine extends VelocityEngine implements StringResourceRepository {
	public StringVelocityEngine() {
		applyStringResourceLoader(this);
	}
	
	public static VelocityEngine applyStringResourceLoader(VelocityEngine ve) {
	    ve.setProperty(Velocity.RESOURCE_LOADER, "string");
	    ve.addProperty("string.resource.loader.class", StringResourceLoader.class.getName());
	    ve.addProperty("string.resource.loader.repository.static", "false");
	    return ve;
	}
	
	public StringResourceRepository getStringResourceRepository() {
	    StringResourceRepository repo = (StringResourceRepository) getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
		return repo;
	}

	@Override
	public StringResource getStringResource(String name) {
		return getStringResourceRepository().getStringResource(name);
	}

	@Override
	public void putStringResource(String name, String body) {
		getStringResourceRepository().putStringResource(name, body);
	}

	@Override
	public void putStringResource(String name, String body, String encoding) {
		getStringResourceRepository().putStringResource(name, body, encoding);
	}

	@Override
	public void removeStringResource(String name) {
		getStringResourceRepository().removeStringResource(name);
	}

	@Override
	public void setEncoding(String encoding) {
		getStringResourceRepository().setEncoding(encoding);
	}

	@Override
	public String getEncoding() {
		return getStringResourceRepository().getEncoding();
	}

}
