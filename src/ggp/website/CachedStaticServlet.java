package ggp.website;

import java.io.IOException;
import javax.servlet.http.*;

import java.util.Collections;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

@SuppressWarnings("serial")
public abstract class CachedStaticServlet extends HttpServlet {
    public static final String cacheName = "webCache"; 
    
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        // Initialize the memcache.
        Cache cache;
        try {
            cache = CacheManager.getInstance().getCache(cacheName);
            if (cache == null) {
                cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
                CacheManager.getInstance().registerCache(cacheName, cache);
            }
        } catch (CacheException e) {
            throw new RuntimeException(e);
        }        
        
        // Allow cross-site access to the files, since nothing is mutable.
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "*");
        resp.setHeader("Access-Control-Allow-Age", "86400");        
        
        // Generate the response content.
        String reqURI = req.getRequestURI();
        
        // Generate the content type based on the URL.
        resp.setContentType(getContentType(reqURI));

        // Cache control administrative requests
        if (reqURI.equals("/cache/stats")) {
            String theStatsString = "Cache statistics [v2]: ";
            theStatsString += "Hits(" + cache.getCacheStatistics().getCacheHits() + ") ";
            theStatsString += "Misses(" + cache.getCacheStatistics().getCacheMisses() + ") ";
            theStatsString += "Size(" + cache.size() + ") ";
            resp.getWriter().println(theStatsString);
            resp.setStatus(200);
            return;
        } else if (reqURI.equals("/cache/flush")) {
            cache.clear();
            resp.getWriter().println("Cache flushed.");
            resp.setStatus(200);
            return;            
        }

        // Query the cache for the requested URL, and return the cached
        // value if there's a cache hit.
        if (cache.containsKey(reqURI)) {
            byte[] theCachedValue = (byte[])cache.get(reqURI);
            resp.getOutputStream().write(theCachedValue);
            resp.setStatus(200);
            return;
        }

        // Otherwise, get the bytes for the response.
        byte[] responseBytes = null;
        try {
            responseBytes = getResponseBytesForURI(reqURI);
        } catch (IOException e) {
            ;
        }
        
        // Write the response content.
        if(responseBytes != null && responseBytes.length > 0) {
            // First, cache the content for later use.
            cache.put(reqURI, responseBytes);

            // Then write it out to the client.
            resp.getOutputStream().write(responseBytes);
            resp.setStatus(200);
        } else {
            resp.setStatus(404);
        }
    }
    
    private static String getContentType(String theURL) {
        if (theURL.endsWith(".xml")) {
            return "application/xml";
        } else if (theURL.endsWith(".xsl")) {
            return "application/xml";
        } else if (theURL.endsWith(".js")) {
            return "text/javascript";   
        } else if (theURL.endsWith(".json")) {
            return "text/javascript";
        } else if (theURL.endsWith(".html")) {
            return "text/html";
        } else if (theURL.endsWith(".png")) {
            return "image/png";
        } else if (theURL.endsWith(".ico")) {
            return "image/png";
        } else {
            return "text/html";
        }
    }
    
    protected abstract byte[] getResponseBytesForURI(String reqURI) throws IOException;    
}
