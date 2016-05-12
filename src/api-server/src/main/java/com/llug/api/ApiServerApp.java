package com.llug.api;

import lombok.extern.slf4j.Slf4j;

import org.eclipse.jetty.nosql.memcached.MemcachedSessionIdManager;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.core.io.ClassPathResource;

import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

@Slf4j
public class ApiServerApp {
    private static final String LISTEN_PORT = "server.port";
    private static final String MAXTHREADS = "server.maxThreads";

    public static void main(String[] args) throws Exception {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        Configuration config = new Configuration("api-server.properties");

        final int port = config.getIntValue(LISTEN_PORT, "9000");
        final int maxThreads = config.getIntValue(MAXTHREADS, "20");
        final String memcacheCluster = config.getStringValue("memcache.cluster", "localhost:11211");

        final ThreadPool threadPool = new QueuedThreadPool(maxThreads);
        //final Server server = new Server(port);

        // http://stackoverflow.com/questions/23329135/how-do-you-set-both-port-and-thread-pool-using-embedded-jetty-v-9-1-0
        final Server server = new Server(threadPool);
        final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory());
        connector.setPort(port);
        server.addConnector(connector);

        // http://wiki.eclipse.org/Jetty/Tutorial/Embedding_Jetty
        // http://stackoverflow.com/questions/4390093/add-web-application-context-to-jetty
        final WebAppContext wac = new WebAppContext();
        String rsrcBase = new ClassPathResource(".").getURI().toString();

        if (rsrcBase.endsWith("/")) {
            rsrcBase = rsrcBase.substring(0, rsrcBase.length() - 1);
        }

        // back out one level...
        rsrcBase = rsrcBase.substring(0, rsrcBase.lastIndexOf("/"));

        String webapp = rsrcBase + "/webapp";
        log.warn("web root = " + webapp);
        wac.setResourceBase(webapp);
        wac.setDescriptor(webapp + "/WEB-INF/web.xml");
        wac.setContextPath("/");
        wac.setParentLoaderPriority(true);

        // https://github.com/yyuu/jetty-nosql-memcached
        // Configuring "session ID manager" 
        MemcachedSessionIdManager memcachedSessionIdManager = new MemcachedSessionIdManager(server);
        memcachedSessionIdManager.setServerString(memcacheCluster);
        memcachedSessionIdManager.setKeyPrefix("session:");
        server.setSessionIdManager(memcachedSessionIdManager);
        server.setAttribute("memcachedSessionIdManager", memcachedSessionIdManager);

        server.setHandler(wac);

        server.start();
        server.join();

        server.setStopTimeout(5000);
        server.setStopAtShutdown(true);
    }
}