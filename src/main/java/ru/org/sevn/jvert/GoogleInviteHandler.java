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

import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static ru.org.sevn.jvert.InviteAuthProvider.INV_SYSTEM;

public class GoogleInviteHandler implements io.vertx.core.Handler<RoutingContext> {

    private final String appName;
    private final UserMatcher userMatcher;
    private final String servPath;
    
    public GoogleInviteHandler(String servPath, String appName, UserMatcher um) {
        this.appName = appName;
        this.userMatcher = um;
        this.servPath = servPath;
    }
    
    @Override
    public void handle(RoutingContext ctx) {
        User user = ctx.user();
        if (user instanceof AccessTokenImpl) {
            AccessTokenImpl atoken = (AccessTokenImpl)user;
            String inv = ctx.request().path().substring((servPath+"invite/").length());
            try {
                inv = java.net.URLDecoder.decode(inv, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
            }
            ExtraUser euser = new ExtraUser(INV_SYSTEM, new PlainUser(inv));
            ExtraUser.upgradeUserInfo(userMatcher, euser);
            ExtraUser guser;
            try {
                guser = GoogleUserOAuth2AuthHandlerImpl.makeGoogleUser(appName, atoken, ctx);
                if (userMatcher.updateUser(euser, guser.getId(), null)) {
                    GoogleUserOAuth2AuthHandlerImpl.upgradeUser(userMatcher, guser, ctx);
                }
            } catch (IOException ex) {
                Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        ctx.next();
    }
    
}
