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
package ru.org.sevn.jvert;

import io.vertx.core.json.JsonObject;

public class HttpVerticleConfig extends JsonObject {
    
    public HttpVerticleConfig(String s) {
        super(s);
        put("wpath", "/"+webpath());
        put("wpathDelim", "/"+webpath()+"/");
    }
    
    public String webpath() {
        return getString("webpath");
    }
    public String dirpath() {
        return getString("dirpath");
    }
    public String dirpathThumb() {
        return getString("dirpathThumb");
    }
    public String dirpathThumbBig() {
        return getString("dirpathThumbBig");
    }
    public String dirpathHtmlCache() {
        return getString("dirpathHtmlCache");
    }
    public String wpath() {
        return getString("wpath");
    }
    public String wpathDelim() {
        return getString("wpathDelim");
    }
}
