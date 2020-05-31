package org.molkex.spring.minimalrest;

import org.molkex.spring.minimalrest.errors.ISE;
import org.molkex.spring.minimalrest.tools.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
public class MinimalMessageBroker implements WebSocketConfigurer, WebSocketHandler {
    private static Logger log = LoggerFactory.getLogger(MinimalMessageBroker.class);

    protected static final String PUBLISH = "PUBLISH";
    protected static final String SUBSCRIBE = "SUBSCRIBE";
    protected static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    protected static final String CLOSE = "CLOSE";

    protected Topic root = new Topic(null,"/", null);

    public void publish(String topic, Object payload) {
        handle(null, Message.publish(topic, Json.write(payload)));
    }

    public <T> Client subscribe(String topic, Class<T> type, Consumer<T> handler) {
        return new Client(this, topic, m -> handler.accept(Json.read(m.getPayload(), type)));
    }

    protected void handle(Client client, Message message) {
        switch (message.type) {
            case PUBLISH:
                onPublish(client, message);
                root.publish(message.topic, message);
                afterPublish(client, message);
                break;

            case SUBSCRIBE:
                onSubscribe(client, message);
                root.register(message.topic, client);
                afterSubscribe(client, message);
                break;

            case UNSUBSCRIBE:
                onUnsubscribe(client, message);
                root.unregister(message.topic, client);
                afterUnsubscribe(client, message);
                break;

            case CLOSE:
                onClose(client);
                root.unregister(null, client);
                afterClose(client);
                break;

            default:
                // NoOp
        }
    }

    protected void onClose(Client client) {
    }

    protected void afterClose(Client client) {
    }

    protected void onUnsubscribe(Client client, Message message) {
    }

    protected void afterUnsubscribe(Client client, Message message) {
    }

    protected void onSubscribe(Client client, Message message) {
    }

    protected void afterSubscribe(Client client, Message message) {
    }

    protected void onPublish(Client client, Message message) {
    }

    protected void afterPublish(Client client, Message message) {
    }

    protected List<String> getTopics() {
        return getTopics("", root.topics, new ArrayList<>());
    }

    private List<String> getTopics(String prefix, Map<String, Topic> topics, ArrayList<String> list) {
        if (topics.isEmpty())
            list.add(prefix);
        else
            topics.forEach((name, topic) ->
                getTopics(prefix+'/'+name, topic.topics, list));
        return list;
    }

    protected static class Message {

        public Message() {}

        String type;

        String topic;

        String payload;

        Message(String type, String topic, String payload) {
            this.type = type;
            this.topic = topic;
            this.payload = payload;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }

        public <T> T parse(Class<T> type) {
            return Json.read(payload, type);
        }

        String cache;
        String cached() {
            if (cache == null) cache = Json.write(this);
            return cache;
        }

        static Message close() {
            return new Message(CLOSE, "", null);
        }

        public static Message publish(String topic, String payload) {
            return new Message(PUBLISH, topic, payload);
        }

        public static Message subscribe(String topic) {
            return new Message(SUBSCRIBE, topic, null);
        }

        public static Message unsubscribe(String topic) {
            return new Message(UNSUBSCRIBE, topic, null);
        }

        public static Message from(String json) {
            return Json.read(json, Message.class);
        }
    }

    private static class Topic {
        Topic parent;
        String name;
        Map<String, Topic> topics = new HashMap<>();
        Map<String, Client> clients = new HashMap<>();
        Map<String, List<Topic>> registry;


        Topic(Topic parent, String name, Map<String, List<Topic>> registry) {
            this.parent = parent;
            this.name = name;
            if (registry == null) registry = new HashMap<>();
            this.registry = registry;
        }

        void register(String topic, Client client) {
            if (topic.isEmpty()) {
                clients.put(client.getId(), client);
                registry.computeIfAbsent(client.getId(), k -> new ArrayList<>()).add(this);
            } else {
                String key = getKey(topic);
                topics.computeIfAbsent(key, k -> new Topic(this, key, registry))
                        .register(topic.substring(key.length()+1), client);
            }
        }

        void unregister(String topic, Client client) {
            if (topic == null) {
                if (registry.containsKey(client.getId())) {
                    registry.get(client.getId())
                            .forEach(t -> t.unregister("", client));
                    registry.remove(client.getId());
                }
            } else if (topic.isEmpty()) {
                clients.remove(client.getId());
                cleanup();
            } else {
                String key = getKey(topic);
                if (topics.containsKey(key))
                    topics.get(key).unregister(topic.substring(key.length()+1), client);
            }
        }

        void cleanup() {
            if(parent != null && clients.isEmpty() && topics.isEmpty()) {
                parent.topics.remove(name);
                parent.cleanup();
            }
        }

        void deliver(Message message) {
            try {
                new ArrayList<>(clients.values())
                        .parallelStream()
                        .forEach(
                                client -> {
                                    try {
                                        client.deliver(message);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                        client.close();
                                    }
                                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void delegate(String topic, Message message) {
            String key = getKey(topic);
            new ArrayList<>(topics.entrySet())
                    .parallelStream()
                    .filter(
                            e -> key.startsWith("*")
                                    || e.getKey().equals(key)
                                    || e.getKey().startsWith("*"))
                    .forEach(
                            e -> e.getValue().publish(
                                    topic.substring(key.length() + 1), message));

        }

        void publish(String topic, Message message) {
            if (topic.isEmpty() || name.equals("**") || name.equals(getKey(topic)))
                deliver(message);

            if (name.equals("*") || !topic.isEmpty())
                delegate(topic, message);

        }

        String getKey(String topic) {
            String key = topic.substring(1);
            if (key.indexOf('/') > 0)
                key = key.substring(0, key.indexOf('/'));
            return key;
        }
    }

    public static class Client {
        WebSocketSession session;
        Client(WebSocketSession session) {
            this.session = session;
        }

        String topic;
        Consumer<Message> consumer;
        MinimalMessageBroker broker;
        Client(MinimalMessageBroker broker, String topic, Consumer<Message> consumer) {
            this.topic = topic;
            this.consumer = consumer;
            this.broker = broker;
            broker.handle(this, Message.subscribe(topic));
        }

        public void close() {
            if (session != null) {
                try {
                    session.close();
                } catch (Exception e) {
                    // NoOp
                }
            } else {
                broker.handle(this, Message.unsubscribe(topic));
            }
        }

        public String getId() {
            return session != null ? "R"+session.getId() : "L"+hashCode();
        }

        public void deliver(Message message) {
            if (session == null) {
                consumer.accept(message);

            } else if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(message.cached()));
                    }
                } catch (Exception e) {
                    throw new ISE(e);
                }
            }
        }
    }

    /* Websocket stuff */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(this, getWebsocketRoot());
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    protected String getWebsocketRoot() {
        return "/broker";
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        handle(new Client(session), Message.from(message.getPayload().toString()));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        try { session.close(); } catch (IOException e) { }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        handle(new Client(session), Message.close());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

}
