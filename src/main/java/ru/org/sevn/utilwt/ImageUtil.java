/*******************************************************************************
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
 *******************************************************************************/
package ru.org.sevn.utilwt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ImageUtil {
    public static ImageIcon createImageIcon(String path, Class resourceClass) {
        java.net.URL imgURL = resourceClass.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Couldn't find file: " + path);
            return null;
        }
    }
    
    public static final int SCALE_DEFAULT = Image.SCALE_SMOOTH;
    
    public static ImageIcon getStretchedImageIcon(ImageIcon ii, int w, int h, boolean incr) {
		if (ii != null) {
			ii = new ImageIcon(ii.getImage().getScaledInstance(w, h, SCALE_DEFAULT));
		}
		return ii;
    }
    public static ImageIcon getScaledImageIconHeight(ImageIcon ii, int h, boolean incr) {
		if (ii != null) {
			if (ii.getIconWidth() > h || incr) {
				ii = new ImageIcon(ii.getImage().getScaledInstance(-1, h, SCALE_DEFAULT));
			}
		}
		return ii;
    	
    }
    public static ImageIcon getScaledImageIcon(ImageIcon ii, int w, int h, boolean incr) {
		if (ii != null) {
			if (ii.getIconWidth() > w || incr) {
				ii = new ImageIcon(ii.getImage().getScaledInstance(w, -1, SCALE_DEFAULT));
			}
			if (ii.getIconHeight() > h) {
				ii = new ImageIcon(ii.getImage().getScaledInstance(-1, h, SCALE_DEFAULT));
			}
		}
		return ii;
    }
    public static Icon getScaledIcon(Icon tmpIcon, int w, int h, boolean incr) {
    	ImageIcon ii = null;
    	if (tmpIcon instanceof ImageIcon) {
    		return getScaledImageIcon((ImageIcon)tmpIcon, w, h, incr);
    	}
		return tmpIcon;
    }
    public static ImageIcon imageFlip(int w, int h, ImageIcon ...icons) {
        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        for (ImageIcon icon : icons) {
	        ig2.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
        }
        return new ImageIcon(bi);
    }
    public static ImageIcon imageWithBackground(ImageIcon icon, Color colorBg) {
    	if (icon != null) {
	        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
	        Graphics2D ig2 = bi.createGraphics();
	        if (colorBg != null) {
		        ig2.setColor(colorBg);
		        ig2.fillRect(0, 0, icon.getIconWidth(), icon.getIconHeight());
	        }
	        ig2.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
	        return new ImageIcon(bi);
    	}
    	return icon;
    }
    public static BufferedImage getBufferedImage(ImageIcon icon) {
        BufferedImage bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig2 = bi.createGraphics();
        ig2.drawImage(icon.getImage(), 0, 0, icon.getIconWidth(), icon.getIconHeight(), null);
        return bi;
    }
    public static void writeImage(BufferedImage bi, String format, File fl) throws IOException {
    	ImageIO.write(bi, format, fl);
    }
	public static String getImageUserCommentString(File fileIn, String encoding) throws UnsupportedEncodingException {
		byte[] b = null;
		try {
			b = getImageUserComment(fileIn);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (b != null) {
			return new String(b, encoding);
		}
		return null;
	}
	public static byte[] getImageUserComment(File fileIn) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(fileIn);
        try {
	        Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
	
	        if (readers.hasNext()) {
	
	            // pick the first available ImageReader
	            ImageReader reader = readers.next();
	
	            // attach source to the reader
	            reader.setInput(stream, true);
	
	            // read metadata of first image
	            int idx = reader.getMinIndex(); 
	           	IIOMetadata metadata = reader.getImageMetadata(idx);
	            
	            String[] names = metadata.getMetadataFormatNames();
	            int length = names.length;
	            for (int i = 0; i < length; i++) {
	                //System.out.println( "Format name: " + names[ i ] );
	                Element tree = (Element)metadata.getAsTree(names[i]);
	                
	                NodeList comNL = tree.getElementsByTagName("com");
	                if (comNL.getLength() > 0) {
	                    IIOMetadataNode comNode;
	                    comNode = (IIOMetadataNode) comNL.item(0);
	                    return (byte[])comNode.getUserObject();
	                }
	            }
	        }
        } finally {
            stream.close();
		}
        return null;
	}

    public static File makeThumbs(ImageIconSupplier imgSupplier, File thumbimg, int height) {
        //BufferedImage frame = FrameGrab.getFrame(new File("/Users/jovi/Movies/test.mp4"), i);
        File ret = null;
        // TODO Exception
        if (!thumbimg.exists()) {
            File thumbdir = thumbimg.getParentFile();
            // TODO Exception
            if (!thumbdir.exists()) {
                thumbdir.mkdirs();
            }
            
            System.err.println("generate thumb>>>" + thumbimg);
            try {
                ImageIcon ii = ImageUtil.getScaledImageIconHeight(imgSupplier.getImageIcon(), height, false);
                ImageIO.write(ImageUtil.getBufferedImage(ii), "png", thumbimg);
                ret = thumbimg;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            ret = thumbimg;
        }
        return ret;
    }
}