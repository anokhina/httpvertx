package ru.org.sevn.jvert;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.impl.OAuth2AuthHandlerImpl;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import static ru.org.sevn.jvert.ExtraUser.upgradeUserInfo;

public class GoogleUserOAuth2AuthHandlerImpl extends OAuth2AuthHandlerImpl {

    private final String appName;
    private UserMatcher userMatcher;

    public GoogleUserOAuth2AuthHandlerImpl(OAuth2Auth authProvider, String callbackURL, String appName, UserMatcher um) {
        super(authProvider, callbackURL);
        this.appName = appName;
        this.userMatcher = um;
    }

    @Override
    public void handle(RoutingContext ctx) {
        User user = ctx.user();
        if (user != null) {
            if (user instanceof AccessTokenImpl) {
                try {
                    upgradeGoogleUser((AccessTokenImpl) user, ctx);
                    user = ctx.user();
                } catch (IOException ex) {
                    Logger.getLogger(HttpVerticle.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (user instanceof ExtraUser) {
                authoriseMe((ExtraUser) user, ctx);
            } else {
                ctx.fail(HttpResponseStatus.FORBIDDEN.code());//403
            }
        } else {
            super.handle(ctx);
        }
    }

    private void upgradeGoogleUser(AccessTokenImpl user, RoutingContext context) throws IOException {
        GoogleCredential credential = new GoogleCredential().setAccessToken(user.principal().getString("access_token"));
        Oauth2 oauth2 = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credential).setApplicationName(appName).build();
        Userinfoplus userinfo = oauth2.userinfo().get().execute();

        ExtraUser euser = new ExtraUser("google", user);
        euser.setExtraData(new JsonObject(userinfo.toPrettyString()));

        upgradeUser(userMatcher, euser, context);
    }

    public static void upgradeUser(UserMatcher userMatcher, ExtraUser euser, RoutingContext context) throws IOException {
        if (upgradeUserInfo(userMatcher, euser) != null) {
            context.setUser(euser);
            Session session = context.session();
            if (session != null) {
                // the user has upgraded from unauthenticated to authenticated
                // session should be upgraded as recommended by owasp
                session.regenerateId();
            }
        }
    }

    
    protected void authoriseMe(ExtraUser user, RoutingContext context) {
        //super.authorise(user, context);
        context.next();
    }

}
