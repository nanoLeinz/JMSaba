package com.alpabit.config;

public class JmsConfig {

    private String providerUrl;
    private String connectionFactory;
    private String destination;
    private boolean topic;

    // Durable subscriber fields
    private String clientId;
    private String subscriptionName;

    public JmsConfig() {}

    public JmsConfig(String providerUrl, String connectionFactory, String destination, boolean topic) {
        this.providerUrl = providerUrl;
        this.connectionFactory = connectionFactory;
        this.destination = destination;
        this.topic = topic;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public void setProviderUrl(String providerUrl) {
        this.providerUrl = providerUrl;
    }

    public String getConnectionFactory() {
        return connectionFactory;
    }

    public void setConnectionFactory(String connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public boolean isTopic() {
        return topic;
    }

    public void setTopic(boolean topic) {
        this.topic = topic;
    }

    // -----------------------------
    // Durable Subscriber Get/Set
    // -----------------------------
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSubscriptionName() {
        return subscriptionName;
    }

    public void setSubscriptionName(String subscriptionName) {
        this.subscriptionName = subscriptionName;
    }

    // -----------------------------
    // Helper validation methods
    // -----------------------------
    public boolean isValid() {
        return providerUrl != null && !providerUrl.isEmpty()
                && connectionFactory != null && !connectionFactory.isEmpty()
                && destination != null && !destination.isEmpty();
    }

    public boolean isValidForSubscriber() {
        return isValid()
                && clientId != null && !clientId.isEmpty()
                && subscriptionName != null && !subscriptionName.isEmpty();
    }

    public String type() {
        return topic ? "TOPIC" : "QUEUE";
    }

    @Override
    public String toString() {
        return "JmsConfig{" +
                "providerUrl='" + providerUrl + '\'' +
                ", connectionFactory='" + connectionFactory + '\'' +
                ", destination='" + destination + '\'' +
                ", topic=" + topic +
                ", clientId='" + clientId + '\'' +
                ", subscriptionName='" + subscriptionName + '\'' +
                '}';
    }
}
