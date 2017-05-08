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

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.impl.StaticHandlerImpl;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NReachableFSStaticHandlerImpl extends StaticHandlerImpl {
    private String webRoot = DEFAULT_WEB_ROOT;

    @Override
    public StaticHandler setWebRoot(String webRoot) {
        super.setWebRoot(webRoot);
        this.webRoot = webRoot;
        return this;
    }

    @Override
    public void handle(RoutingContext context) {
        try {
            if (new File(webRoot).exists()) {
                super.handle(context);
            } else {
                context.fail(HttpResponseStatus.NOT_FOUND.code());
            }
        } catch (Exception ex) {
            Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, "Not found: " + webRoot);
            context.fail(HttpResponseStatus.NOT_FOUND.code());
        }
    }
    
}
