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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.jcodec.common.NIOUtils;
import ru.org.sevn.utilwt.ImageIconSupplier;
import ru.org.sevn.utilwt.ImageUtil;
import javax.swing.ImageIcon;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.api.awt.FrameGrab;

public class ThumbHandler implements io.vertx.core.Handler<RoutingContext> {

    private final String wpath;
    private final String wpathDelim;
    private final String dirpath;
    private final String dirpathThumb;
    private final String dirpathThumbBig;

    public ThumbHandler(String wpath, String wpathDelim, String dirpath, String dirpathThumb, String dirpathThumbBig) {
        this.dirpathThumb = dirpathThumb;
        this.dirpathThumbBig = dirpathThumbBig;
        this.wpath = wpath;
        this.wpathDelim = wpathDelim;
        this.dirpath = dirpath;
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpServerRequest r = ctx.request();
        String imgThmb = r.params().get("imgThmb");
        if (imgThmb != null) {

            String path = r.path();
            try {
                path = java.net.URLDecoder.decode(r.path(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(ThumbHandler.class.getName()).log(Level.SEVERE, null, ex);
            }

            int height = 0;
            String dirpathTh = "";
            String newRoute = "";
            String thumbName = path.substring(wpathDelim.length());
            if ("sm".equals(imgThmb)) {
                height = 160;
                dirpathTh = dirpathThumb;
                newRoute = wpath + "/thumb/" + thumbName;
            } else if ("bg".equals(imgThmb)) {
                height = 736;
                dirpathTh = dirpathThumbBig;
                newRoute = wpath + "/thumbg/" + thumbName;
            }
            if (height > 0) {
                final File img = new File(dirpath, thumbName);
                if (img.exists()) {
                    try {
                        String contentType = Files.probeContentType(img.toPath());
                        if (contentType != null && contentType.startsWith("image")) {
                            File thumbFile = new File(dirpathTh, thumbName);
                            if (ImageUtil.makeThumbs(new ImageIconSupplier() {

                                @Override
                                public ImageIcon getImageIcon() {
                                    return new ImageIcon(img.getPath());
                                }
                            }, thumbFile, height) != null) {
                                ctx.reroute(newRoute);
                                return;
                            }
                        } else if (contentType != null && contentType.startsWith("video")) {
                            thumbName += ".png";
                            newRoute += ".png";
                            File thumbFile = new File(dirpathTh, thumbName);
                            if (ImageUtil.makeThumbs(new ImageIconSupplier() {

                                @Override
                                public ImageIcon getImageIcon() {
                                    try {
                                        FileChannelWrapper grabch = NIOUtils.readableFileChannel(img);
                                        BufferedImage frame = null;
                                        try {
                                            FrameGrab grab = new FrameGrab(grabch);
                                            for (int i = 0; i < 50; i++) {
                                                grab.seekToFrameSloppy(50);
                                                try {
                                                    frame = grab.getFrame();
                                                } catch (Exception e) {
                                                    Logger.getLogger(ThumbHandler.class.getName()).log(Level.SEVERE, null, e);
                                                }
                                            }
                                        } finally {
                                            NIOUtils.closeQuietly(grabch);
                                        }
                                        if (frame != null) {
                                            return new ImageIcon(frame);
                                        }
                                    } catch (Exception ex) {
                                        Logger.getLogger(ThumbHandler.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    return null;
                                }
                            }, thumbFile, height) != null) {
                                ctx.reroute(newRoute);
                                return;
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ThumbHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            //TODO cache image refactor
        }
        ctx.next();
    }

}
