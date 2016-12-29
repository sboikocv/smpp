package com.sboiko.smpp.router;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.*;
import com.sboiko.smpp.session.DefaultSessionHandler;
import com.sboiko.smpp.data.Msg;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class RouterSQS implements AbstractRouter {

    private final ArrayList<DefaultSessionHandler> sessions;
    private long counter = 0;
    private AWSCredentials credentials = null;
    private AmazonSQS sqs = null;
    private String queueUrl = null;
    private ThreadPoolExecutor executor = null;
    private Logger logger = null;
    private AtomicBoolean running = new AtomicBoolean(false);
    private Integer pollingInterval = 0;

    public RouterSQS(ThreadPoolExecutor executor, Logger logger) {
        Region usWest = Region.getRegion(Regions.US_WEST_2);
        CreateQueueRequest createQueueRequest = new CreateQueueRequest("SmppQueue");
        this.logger = logger;
        sessions = new ArrayList<>();
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that your credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
        sqs = new AmazonSQSClient(credentials);
        sqs.setRegion(usWest);
        queueUrl = sqs.createQueue(createQueueRequest).getQueueUrl();
        this.executor = executor;

    }

    public void setPollingInterval(Integer interval) {
        pollingInterval = interval;
    }

    public void start() {
        running.set(true);
        executor.getThreadFactory().newThread(new Runnable() {
            public void run() {
                    consume();
            }
        }).start();
    }

    private void consume() {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
        while (running.get()) {
            logger.info("Polling: ", queueUrl);
            List<Message> messages = sqs.receiveMessage(receiveMessageRequest.withWaitTimeSeconds(pollingInterval))
                    .getMessages();
            for (Message message : messages) {
                logger.info("  Message");
                logger.info("    Body:          " + message.getBody());
                Msg msg = Msg.fromString(message.getBody());
                logger.info("msg: {}", msg.getDestAddress());
                route(msg);
                deleteMessage(message);
            }
        }
    }

    private void deleteMessage(Message message) {
        String messageRcpthandle = message.getReceiptHandle();
        sqs.deleteMessage(new DeleteMessageRequest(queueUrl, messageRcpthandle));
    }

    public void produce(Msg msg) {
        logger.info("Send message: ", msg.toString());
        sqs.sendMessage(new SendMessageRequest(queueUrl, msg.toString()));
    }

    private void route(Msg msg) {
        DefaultSessionHandler session;
        if (msg == null || sessions.size() == 0) {
            logger.info("Drop message");
            return;
        }

        synchronized (sessions) {
            do {
                ++counter;
                if (counter < 0) {
                    counter = 0;
                }
                session = sessions.get((int) (counter % sessions.size()));
            } while (!session.send(msg));
        }
    }

    public void add(DefaultSessionHandler sessionHandler) {
        synchronized (sessions) {
            sessions.add(sessionHandler);
        }
    }

    public boolean remove(DefaultSessionHandler sessionHandler) {
        synchronized (sessions) {
            return sessions.remove(sessionHandler);
        }
    }

    public void failToSend(Msg msg) {
        logger.error("Fail to send message: {}", msg);
    }

    public void destroy() {
        running.compareAndSet(true, false);
//        sqs.deleteQueue(queueUrl);
    }
}
