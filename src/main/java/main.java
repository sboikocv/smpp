import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.sboiko.smpp.client.DefaultSmppClientHandler;
import com.sboiko.smpp.data.SmppConfiguration;
import com.sboiko.smpp.router.Router;
import com.sboiko.smpp.server.DefaultSmppServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class main {
    private static final Logger logger = LoggerFactory.getLogger("Java-Smpp");


    static public void main(String[] args) throws Exception {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        ScheduledThreadPoolExecutor monitorExecutor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private AtomicInteger sequence = new AtomicInteger(0);

            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("Java-Smpp-Pool-" + sequence.getAndIncrement());
                return t;
            }
        });

        Router router = new Router(executor, logger);
        router.setPollingInterval(5);

        SmppServerConfiguration config = SmppConfiguration.getServerConfiguration();
        DefaultSmppServerHandler serverHandler = new DefaultSmppServerHandler(logger);
        serverHandler.setRouter(router);

        DefaultSmppServer server = new DefaultSmppServer(config,
                serverHandler, executor, monitorExecutor);

        DefaultSmppClientHandler client = new DefaultSmppClientHandler(executor,2, monitorExecutor, logger);
        client.setReconnectDelay(5);
        client.setRouter(router);
        logger.info("Starting smpp client");
        client.start(SmppConfiguration.getSessionConfiguration());
        logger.info("Starting smpp server");
        server.start();
        logger.info("Starting router");
        router.start();

        System.in.read();
        server.destroy();
        client.destroy();
        router.destroy();

        executor.shutdownNow();
        monitorExecutor.shutdownNow();
    }

}
