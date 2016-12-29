import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.sboiko.smpp.client.DefaultSmppClientHandler;
import com.sboiko.smpp.data.SmppConfiguration;
import com.sboiko.smpp.router.RouterQueue;
import com.sboiko.smpp.router.RouterSQS;
import com.sboiko.smpp.server.DefaultSmppServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class main {
    private static final Logger logger = LoggerFactory.getLogger(main.class);


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

//        RouterSQS router = new RouterSQS(executor, logger);
//        router.setPollingInterval(5);
        RouterQueue router = new RouterQueue(executor, logger);

        SmppServerConfiguration config = SmppConfiguration.getServerConfiguration();
        DefaultSmppServerHandler serverHandler = new DefaultSmppServerHandler(logger);
        serverHandler.setRouter(router);

        DefaultSmppServer server = new DefaultSmppServer(config,
                serverHandler, executor, monitorExecutor);

        DefaultSmppClientHandler client = new DefaultSmppClientHandler(executor,1, monitorExecutor, logger);
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
