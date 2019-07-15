package org.actioncontrollerdemo.jdkhttp;

import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

public class StaticContent implements HttpExchangeHandler {
    private final URL baseResource;
    private final String prefix;

    private static Properties mimeTypes = new Properties();
    static {
        try {
            mimeTypes.load(StaticContent.class.getClassLoader().getResourceAsStream("mime-types.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public StaticContent(URL baseResource, String prefix) throws MalformedURLException {
        if (baseResource.getProtocol().equals("file")) {
            File resourceSrc = new File(baseResource.getPath().replace("/target/classes/", "/src/main/resources/"));
            if (resourceSrc.exists()) {
                this.baseResource = resourceSrc.toURI().toURL();
            } else {
                this.baseResource = baseResource;
            }
        } else {
            this.baseResource = baseResource;
        }
        this.prefix = prefix;
    }

    static URL webJarResource(String webJarName) {
        String prefix = "/META-INF/resources/webjars/" + webJarName;
        Properties properties = new Properties();
        try (InputStream pomProperties = MainWebHttpHandler.class.getResourceAsStream("/META-INF/maven/org.webjars/" + webJarName + "/pom.properties")) {
            properties.load(pomProperties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return MainWebHttpHandler.class.getResource(prefix + "/" + properties.get("version"));
    }

    public static StaticContent createWebJar(String webJarName, String prefix) throws MalformedURLException {
        return new StaticContent(webJarResource(webJarName), prefix);
    }

    public boolean handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        if (!uri.getPath().startsWith(prefix + "/")) {
            return false;
        }
        URL url = new URL(baseResource + uri.getPath().substring(prefix.length()));
        return sendContent(exchange, url);
    }

    private boolean sendContent(HttpExchange exchange, URL url) throws IOException {
        try (InputStream inputStream = url.openStream()) {
            String contentType =  URLConnection.getFileNameMap().getContentTypeFor(url.getPath());
            if (contentType != null) {
                exchange.getResponseHeaders().set("Content-type", contentType);
            } else {
                int lastPeriodPos = url.getPath().lastIndexOf('.');
                int lastSlashPos = url.getPath().lastIndexOf('/');
                if (lastPeriodPos > 0 && lastPeriodPos > lastSlashPos) {
                    String extension = url.getPath().substring(lastPeriodPos + 1);
                    contentType = mimeTypes.getProperty(extension);
                    if (contentType != null) {
                        exchange.getResponseHeaders().set("Content-type", contentType);
                    }
                }
            }

            exchange.sendResponseHeaders(200, 0);

            int c;
            while ((c = inputStream.read()) != -1) {
                exchange.getResponseBody().write((char) c);
            }
            exchange.close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (Exception e) {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write(e.toString().getBytes());
            exchange.close();
            return true;
        }
    }
}