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
package ru.org.sevn.jvert.wwwgen;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.glxn.qrgen.core.exception.QRGenerationException;
import net.glxn.qrgen.javase.QRCode;
import ru.org.sevn.jvert.VertxOutputStream;
import ru.org.sevn.jvert.auth.InviteHandler;

public class QRGenHandler implements io.vertx.core.Handler<RoutingContext> {
    //https://github.com/kenglxn/QRGen
    
    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest r = ctx.request();
        String url2code = r.getParam("url");
                
        if (url2code != null) {
            ctx.response().putHeader("content-type", "image/png").setChunked(true);
            VertxOutputStream vos = new VertxOutputStream(ctx.response());

            try {
                //.withSize(250, 250)
                QRCode.from(url2code).withCharset("UTF-8").to(net.glxn.qrgen.core.image.ImageType.PNG).writeTo(vos);
                vos.close();
                return;
            } catch (IOException | QRGenerationException ex) {
                Logger.getLogger(InviteHandler.class.getName()).log(Level.SEVERE, null, ex);
                ctx.fail(ex);
                return;
            }
        } else {
            String s = "<form url='"+ctx.request().absoluteURI()+"'>url:<input type='text' name='url'>"+"</form>";
            ctx.response().putHeader("content-type", "text/html").end(s);
        }
    }    
}
