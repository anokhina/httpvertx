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
package ru.org.sevn.jvert.auth;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.impl.RedirectAuthHandlerImpl;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.org.sevn.jvert.HttpVerticle;
import ru.org.sevn.jvert.VertxOutputStream;
/**
 *
 * @author avn
 */
public class InviteHandler implements io.vertx.core.Handler<RoutingContext> {
    
    static String DEFAULT_RETURN_URL_PARAM = "return_url";
    
    private final String servPath;
    private final String returnURLParam;
    
    public InviteHandler(String servPath) {
        this(servPath, DEFAULT_RETURN_URL_PARAM);
    }
    
    public InviteHandler(String servPath, String returnURLParam) {
        this.servPath = servPath;
        this.returnURLParam = returnURLParam;
    }

    @Override
    public void handle(RoutingContext ctx) {
        User user = ctx.user();
        if (user == null) {
            Session session = ctx.session();
            if (session != null) {
                session.put(returnURLParam, ctx.request().uri());
            }
            File fl = getInviteFile();
            String fileCont = null;
            String inv = "";
            String path = ctx.request().path();
            if (servPath.length() < path.length()) {
                inv = ctx.request().path().substring((servPath).length());
            } else if (servPath.length() > path.length()) {
                ctx.fail(HttpResponseStatus.NOT_FOUND.code());
                return;
            }
            try {
                inv = java.net.URLDecoder.decode(inv, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
            }        
            try {
                fileCont = new String(Files.readAllBytes(fl.toPath()), "UTF-8");
                fileCont = fileCont.replace("inv:invitation", inv);
            } catch (Exception ex) {
                Logger.getLogger(InviteHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (fileCont != null) {
                ctx.response().putHeader("content-type", "text/html").setChunked(true);
                VertxOutputStream vos = new VertxOutputStream(ctx.response());
                try {
                    vos.write(fileCont.getBytes("UTF-8"));
                    vos.close();
                    return;
                } catch (IOException ex) {
                    Logger.getLogger(InviteHandler.class.getName()).log(Level.SEVERE, null, ex);
                    ctx.fail(ex);
                    return;
                }
            }
        }
        ctx.next();
    }
    
    protected File getInviteFile() {
        return new File("webroot/invite/", "invitepage.html");
    }
}
