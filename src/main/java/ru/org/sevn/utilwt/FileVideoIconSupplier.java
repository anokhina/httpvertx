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
package ru.org.sevn.utilwt;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.jcodec.api.awt.FrameGrab;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;

public class FileVideoIconSupplier implements ImageIconSupplier {

    private final File img;
    private ImageIcon imageIcon;

    public FileVideoIconSupplier(File fl) {
        this.img = fl;
    }

    @Override
    public ImageIcon getImageIcon() {
        if (imageIcon == null) {
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
                            Logger.getLogger(FileVideoIconSupplier.class.getName()).log(Level.SEVERE, null, e);
                        }
                    }
                } finally {
                    NIOUtils.closeQuietly(grabch);
                }
                if (frame != null) {
                    imageIcon = new ImageIcon(frame);
                }
            } catch (Exception ex) {
                Logger.getLogger(FileVideoIconSupplier.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return imageIcon;
    }
}
