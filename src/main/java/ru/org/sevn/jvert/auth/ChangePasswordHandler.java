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

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.org.sevn.jvert.ExtraUser;
import ru.org.sevn.jvert.FileAuthProvider;
import ru.org.sevn.jvert.UserMatcher;
import ru.org.sevn.jvert.VertxOutputStream;

public class ChangePasswordHandler implements io.vertx.core.Handler<RoutingContext> {
    
    private final FileAuthProvider fileAuthProvider;
    
    public ChangePasswordHandler(FileAuthProvider fileAuthProvider) {
        this.fileAuthProvider = fileAuthProvider;
    }
    
    public UserMatcher getUserMatcher() {
        return fileAuthProvider.getUserMatcher();
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        User user = ctx.user();
        JsonObject userInfo = getUserMatcher().getUserInfo(user);
        if (userInfo != null) {
            ExtraUser euser = (ExtraUser)user;
            
            HttpServerRequest r = ctx.request();
            String password0 = r.getParam("password0");
            String password1 = r.getParam("password1");
            String password2 = r.getParam("password2");
            
            if (password0 == null || password1 == null || password2 == null) {
                showFileForm(ctx, "Введите текущий и новый пароли /<br> Enter current and new passwords"); //TODO localize me
            } else {
                String username = fileAuthProvider.getUserName(euser.getId());
                if (fileAuthProvider.authenticated(userInfo, password0, username)) {
                    if (password1.equals(password2)) {
                        userInfo.put("token", fileAuthProvider.getAuth().getHashString(password1, username));
                        
                        if (getUserMatcher().updateUser(user, userInfo)) {
                            showFile(ctx, getOkFile(), "Пароль изменен /<br> New password set successfully"); //TODO localize me
                        } else {
                            showFileForm(ctx, "Не могу изменить пароль/<br> Can't set new password"); //TODO localize me
                        }
                    } else {
                        showFileForm(ctx, "Новые пароли не совпадают /<br> New passwords mismatch"); //TODO localize me
                    }
                } else {
                    showFileForm(ctx, "Не верно введен текущий пароль /<br> Wrong current password"); //TODO localize me
                }
            }
            return;
        }
        ctx.next();
    }
    
    private void showFileForm(RoutingContext ctx, String msg) {
        showFile(ctx, getInviteFile(), msg);
    }
    private void showFile(RoutingContext ctx, File fl, String msg) {
        String fileCont = null;
        try {
            fileCont = new String(Files.readAllBytes(fl.toPath()), "UTF-8");
            fileCont = fileCont.replace("inv:invitation", msg);
        } catch (Exception ex) {
            Logger.getLogger(ChangePasswordHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (fileCont != null) {
            ctx.response().putHeader("content-type", "text/html").setChunked(true);
            VertxOutputStream vos = new VertxOutputStream(ctx.response());
            try {
                vos.write(fileCont.getBytes("UTF-8"));
                vos.close();
                return;
            } catch (IOException ex) {
                Logger.getLogger(ChangePasswordHandler.class.getName()).log(Level.SEVERE, null, ex);
                ctx.fail(ex);
                return;
            }
        }
    }
    
    protected File getInviteFile() {
        return new File("webroot/profile/", "invitepage.html");
    }
    protected File getOkFile() {
        return new File("webroot/profile/", "ok.html");
    }
}
