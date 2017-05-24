/*
 * Copyright 2015-2016 Veronika Anokhina.
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
package ru.org.sevn.common.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author VAnokhina
 */
public class JaxbMarshallerManager {
    private static final HashMap<Class, JAXBContext> map = new HashMap<Class, JAXBContext>();

    private static void error(String msg, Throwable ex) {
        Logger.getLogger(JaxbMarshallerManager.class.getName()).log(Level.SEVERE, msg, ex);
    }
    public static String toString(Object obj, Class[] contextClasses) {
        if (obj == null) return null;
        try {
            return marshal(contextClasses, obj, true, true);
        } catch (JAXBException ex) {
            error("error marshalling for " + obj.getClass(), ex);
        }
        return obj.toString();
    }
    
    public static String toString(Object obj) {
        return toString(obj, null);
    }
    public static Marshaller getMarshaller(Object obj) throws JAXBException {
        return getMarshaller(obj, null);
    }
    public static Marshaller getMarshaller(Object obj, Class[] contextClasses) throws JAXBException {
        return getMarshaller(obj.getClass(), contextClasses);
    }
    public static Marshaller getMarshaller(Class cl) throws JAXBException {
        return getMarshaller(cl, null);
    }
    public static Marshaller getMarshaller(Class cl, Class[] contextClasses) throws JAXBException {
        JAXBContext ctx = getContext(cl, contextClasses);
        return ctx.createMarshaller();
    }
    
    public static void marshal(Object obj, File file2wr) throws JAXBException {
        Marshaller marsh = getMarshaller(obj);
        marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marsh.marshal(obj, file2wr);
    }

    public static <T> T unmarshal(Class<T> cl, File file2read, T def) {
        T ret = def;
        try {
            ret = unmarshal(cl, file2read);
        } catch (JAXBException ex) {
            error(null, ex);
        }
        return ret;
    }
    public static <T> T unmarshal(Class<T> cl, File file2read) throws JAXBException {
        JAXBContext ctx = getContext(cl);
        javax.xml.bind.Unmarshaller unmarshaller = ctx.createUnmarshaller();
        return (T)unmarshaller.unmarshal(file2read);
    }
    
    public static <T> T clone(T obj) throws JAXBException {
        String objStr = marshal(obj);
        return (T) unmarshal(obj.getClass(), objStr);
    }
    public static String marshalList(Collection lst, boolean formatted) throws JAXBException {
        StringBuilder sb = new StringBuilder();
        for(Object o : lst) {
            sb.append(marshal(o, formatted, true)).append((formatted) ? "\n" : "");
        }
        return sb.toString();
    }
    //, Class[] contextClasses
    public static String marshal(Object obj, boolean formatted, boolean noHeader) throws JAXBException {
        return marshal(null, obj, formatted, noHeader);
    }
    public static String marshal(Class[] contextClasses, Object obj, boolean formatted, boolean noHeader) throws JAXBException {
        Marshaller marsh = getMarshaller(obj, contextClasses);
        marsh.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
        marsh.setProperty(Marshaller.JAXB_FRAGMENT, noHeader);
        StringWriter sw = new StringWriter();
        marsh.marshal(obj, sw);
        return sw.toString();
    }
    
    public static String marshal(Object obj) throws JAXBException {
        return marshal(obj, true, false);
    }

    public static <T> T unmarshal(Class<T> cl, String xml) throws JAXBException {
        if (xml == null) return null;
        if (xml.trim().length() == 0) return null;
        JAXBContext ctx = getContext(cl);
        javax.xml.bind.Unmarshaller unmarshaller = ctx.createUnmarshaller();
        StringReader sr = new StringReader(xml);
        return (T)unmarshaller.unmarshal(sr);
    }
    
    public static <T> T unmarshal(Class<T> cl, String xml, T def) {
        try {
            return unmarshal(cl, xml);
        } catch (Exception e) {}
        return def;
    }
    
    public static JAXBContext getContext(Class cl) throws JAXBException {
        return getContext(cl, null);
    }
    public static JAXBContext getContext(Class cl, Class[] contextClasses) throws JAXBException {
        synchronized (map) {
            JAXBContext ctx = map.get(cl);
            if (ctx == null) {
                if (contextClasses != null) {
                    Class[] cls = new Class[contextClasses.length + 1];
                    System.arraycopy(contextClasses, 0, cls, 0, contextClasses.length);
                    cls[cls.length - 1] = cl;
                    ctx = JAXBContext.newInstance(cls);
                } else {
                    ctx = JAXBContext.newInstance(cl);
                }
                map.put(cl, ctx);
            }
            return ctx;
        }
    }
    
    public static String mergeXmlStr(String... files) {
        try {
            Document doc = mergeXml(files);
            try {
                return xml2str(doc);
            } catch (TransformerConfigurationException ex) {
                error( null, ex);
            } catch (TransformerException ex) {
                error( null, ex);
            }
        } catch (ParserConfigurationException ex) {
            error( null, ex);
        } catch (SAXException ex) {
            error( null, ex);
        } catch (IOException ex) {
            error( null, ex);
        }
        return null;
    }
    
    public static Document mergeXml(String... files) throws ParserConfigurationException, SAXException, IOException {
        InputStream[] is = new InputStream[files.length];
        for(int i = 0; i < is.length; i++) {
            is[i] = new ByteArrayInputStream(files[i].getBytes("UTF-8"));
        }
        return mergeXml(is);
    }
    
    private static boolean eqNode(Node n1, Node n2) {
        boolean ret = false;
        String naId1 = null;
        if (n1.getAttributes() != null) {
            Node n = n1.getAttributes().getNamedItem("id");
            if (n!=null) {
                naId1 = n.getNodeValue();
            }
        }
        String naId2 = null;
        if (n2.getAttributes() != null) {
            Node n = n2.getAttributes().getNamedItem("id");
            if (n!=null) {
                naId2 = n.getNodeValue();
            }
        }
        if (n1.getNodeType()== Node.ELEMENT_NODE && n1.getNodeName().equals(n2.getNodeName())) {
            if (naId1== null && naId2==null) {
                ret = true;
            } else
            if (naId1!= null && naId2!=null) {
                if (naId1.equals(naId2)) {
                    ret = true;
                }
            }
        }
        //System.out.println("eq>" + ret+":"+n1.getNodeName() + ":" + n2.getNodeName() + ":" + naId1 + ":" + naId2);
        
        return ret;
    }
    
    public static void merge(Node n1, Node n2) {
        if (eqNode(n1, n2)) {
            if (!n2.hasChildNodes()) {
                return;
            }
            //System.out.println("merge>"+n1.getNodeName()+":"+n2.getNodeName());
            NodeList nl1 = n1.getChildNodes();
            try {
            for (int i = 0; i < nl1.getLength(); i++) {
                Node cn1 = nl1.item(i);
                if (cn1.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList nl2 = n2.getChildNodes();
                    //System.out.println("cnode>" + cn1 + ":" + nl1.getLength()+":" + i+":"+nl2);
                    if (nl2 != null) {
                        for (int j = 0; j < nl2.getLength(); j++) {
                            Node cn2 = nl2.item(j);
                            //System.out.println("cnode2>" + cn2 + ":" + nl2.getLength());
                            if (cn2 != null && eqNode(cn1, cn2)) {
                                //System.out.println("mmmmmmm>" + n2.getChildNodes());
                                merge(cn1, cn2);
                                //System.out.println("mmmmmmm<" + n1.getChildNodes());
                                n2.removeChild(cn2);
                                break;
                            }
                        }
                    }
                } else if (cn1.getNodeType() == Node.TEXT_NODE && nl1.getLength() == 1) {
                    n1.removeChild(cn1);
                }
            }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            //System.out.println("has ch2>" + n2.hasChildNodes()+":"+n1.getChildNodes().getLength());
            try {
            while(n2.hasChildNodes()) {
                Node n = n2.getFirstChild();
                n2.removeChild(n);
                Node nn = n1.getOwnerDocument().importNode(n, true);
                //System.out.println("aaaaaaaaa>"+n.getNodeName()+":"+n1.getNodeName());
                n1.appendChild(nn);

            }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        } else {
            throw new IllegalArgumentException();
        }
    }
    
    public static Document mergeXml(InputStream... files) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document base = docBuilder.parse(files[0]);

        Node results = (Node) base.getFirstChild();

        for (int i = 1; i < files.length; i++) {
            Document merge = docBuilder.parse(files[i]);
            Node nextResults = (Node) merge.getFirstChild();
            merge(results, nextResults);
        }

        return base;
    }
    
    public static void printXml(Document doc, Result result) throws TransformerConfigurationException, TransformerException {
        DOMSource source = new DOMSource(doc);
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.transform(source, result);
    }
    
    public static String xml2str(Document doc) throws TransformerConfigurationException, TransformerException {

        StringWriter sw = new StringWriter();
        printXml(doc, new StreamResult(sw));
        return sw.toString();
    }
    
    public static boolean compile(ClassLoader classLoader, JavaFileObject... source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        //or
        //StandardJavaFileManager.setLocation(StandardLocation.CLASS_PATH, "YOUR_CLASS_PATH")
        ArrayList<String> options = new ArrayList<String>();
        options.add("-classpath");
        {
            StringBuilder sb = new StringBuilder();
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                for (java.net.URL url : urlClassLoader.getURLs()) {
                    String file = url.getFile();
//                    System.out.println("===========================>" + file);
                    sb.append(file).append(File.pathSeparator);
                }
            }
            sb.append(System.getProperty("java.class.path"));
            options.add(sb.toString());
        }

        ArrayList<String> optionList = new ArrayList<String>();
        optionList.addAll(java.util.Arrays.asList("-classpath",System.getProperty("java.class.path")));
//        System.out.println("----------------------------" + System.getProperty("java.class.path"));
//        try {
//            for (java.util.Enumeration<java.net.URL> en = classLoader.getResources("META-INF/MANIFEST.MF"); en.hasMoreElements();) {
//                java.net.URL url = en.nextElement();
//                System.out.println("============================" + url.getFile());
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(MarshallerManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
        final JavaCompiler.CompilationTask task =
                compiler.getTask(null /* default System.err */,
                null /* standard file manager */,
                null /* standard DiagnosticListener */,
                options /* no options */,
                null /* no annotation classes */, 
                java.util.Arrays.asList(source) /* source code */
        );
        return task.call();
    }
    
    public static Class compile(ClassLoader classLoader, String packageName, String className, String src) {
//        System.out.println(src);
        RamJavaFileObject srcFile;
        try {
            srcFile = new RamJavaFileObject(className, src);
            if (compile(classLoader, srcFile)) {
                String clName = packageName + "." + className;
                try {
                    return Class.forName(clName);
                } catch (ClassNotFoundException ex) {
                    //Logger.getLogger(ClassLoaderImpl.class.getName()).log(Level.SEVERE, null, ex);
                }
                return classLoader.loadClass(clName);
            }
        } catch (ClassNotFoundException e) {
            error(null, e);
        } catch (URISyntaxException ex) {
            error(null, ex);
        }
        return null;
    }
    
    static class RamJavaFileObject extends SimpleJavaFileObject {

        /**
         * source text of the program to be compiled
         */
        private final String programText;

        public RamJavaFileObject(String className, String programText) throws URISyntaxException {
            super(new URI(className + ".java"), Kind.SOURCE);
            this.programText = programText;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return programText;
        }
    }
}

