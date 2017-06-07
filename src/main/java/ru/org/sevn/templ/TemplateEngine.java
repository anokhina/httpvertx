/*
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
 */
package ru.org.sevn.templ;

import java.io.File;
import org.apache.velocity.app.VelocityEngine;

public class TemplateEngine {
	private VelocityEngine ve;
    
    public TemplateEngine(String resdir) {
        VelocityEngine ve = new VelocityEngine();
        if (resdir == null || !new File(resdir).exists()) {
            ClasspathVelocityEngine.applyClasspathResourceLoader(ve);
        } else {
            ClasspathVelocityEngine.applyFileResourceLoader(ve, resdir);

        }
        this.ve = ve;
		ve.init();
    }
    
    public VelocityEngine getVelocityEngine() {
        return ve;
    }

}
