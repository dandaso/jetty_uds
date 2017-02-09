import java.io.IOException;
import java.io.File;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.unixsocket.UnixSocketConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class JettyUdsServer extends HttpServlet {

    private static void tcp() throws Exception {
        Server server = new Server(1234);
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(JettyUdsServer.class, "/");
        server.setHandler(handler);
        server.start();
        server.join();
    }

    private static void uds() throws Exception {
        QueuedThreadPool pool = new QueuedThreadPool(200, 30);
        pool.setDetailedDump(false);

        Server server = new Server(pool);
        HttpConnectionFactory http = new HttpConnectionFactory();
        //ProxyConnectionFactory proxy = new ProxyConnectionFactory(http.getProtocol());
        UnixSocketConnector connector = new UnixSocketConnector(server, http);

        connector.setIdleTimeout(100);
        connector.setAcceptQueueSize(1000);

        String sock_name = "/var/www/netty.sock";

        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(JettyUdsServer.class, "/");
        server.setHandler(handler);

        connector.setUnixSocket(sock_name);
        server.addConnector(connector);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> { 
           File sock = new File(sock_name);
           sock.delete();
        }));
        server.start();
        Process proc = Runtime.getRuntime().exec("chmod 777 " + sock_name);
        server.join();
    }

    public static void main(String[] args) throws Exception {
        try {  
	  uds();
          //tcp();
        } catch(Exception e) {

        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setStatus(200);
        res.getWriter().println("Servlet on Jetty.");
    }
}

