/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jnzader.apigen.codegen.generator.rust.edge;

import com.jnzader.apigen.codegen.generator.rust.RustAxumOptions;
import com.jnzader.apigen.codegen.generator.rust.RustTypeMapper;

/**
 * Generates MQTT client code using rumqttc.
 *
 * @author APiGen
 * @since 2.12.0
 */
public class RustMqttGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustMqttGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the mqtt/mod.rs file.
     *
     * @return the mod.rs content
     */
    public String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! MQTT client for pub/sub messaging.\n\n");

        sb.append("mod publisher;\n");
        sb.append("mod subscriber;\n");
        sb.append("\n");
        sb.append("pub use publisher::MqttPublisher;\n");
        sb.append("pub use subscriber::MqttSubscriber;\n");

        return sb.toString();
    }

    /**
     * Generates the MQTT publisher module.
     *
     * @return the publisher module content
     */
    public String generatePublisher() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! MQTT message publisher.\n\n");

        sb.append("use anyhow::Result;\n");
        sb.append("use rumqttc::{AsyncClient, MqttOptions, QoS};\n");
        sb.append("use serde::Serialize;\n");
        sb.append("use std::env;\n");
        sb.append("use std::time::Duration;\n");
        sb.append("use tracing::{error, info};\n");
        sb.append("\n");

        // Publisher struct
        sb.append("/// MQTT message publisher.\n");
        sb.append("#[derive(Clone)]\n");
        sb.append("pub struct MqttPublisher {\n");
        sb.append("    client: AsyncClient,\n");
        sb.append("}\n\n");

        sb.append("impl MqttPublisher {\n");

        // Constructor
        sb.append("    /// Creates a new MQTT publisher.\n");
        sb.append("    pub async fn new() -> Result<Self> {\n");
        sb.append("        let broker = env::var(\"MQTT_BROKER\")\n");
        sb.append("            .unwrap_or_else(|_| \"localhost\".to_string());\n");
        sb.append("        let port: u16 = env::var(\"MQTT_PORT\")\n");
        sb.append("            .unwrap_or_else(|_| \"1883\".to_string())\n");
        sb.append("            .parse()\n");
        sb.append("            .unwrap_or(1883);\n");
        sb.append("        let client_id = env::var(\"MQTT_CLIENT_ID\")\n");
        sb.append("            .unwrap_or_else(|_| \"rust-api-publisher\".to_string());\n\n");

        sb.append("        let mut options = MqttOptions::new(&client_id, &broker, port);\n");
        sb.append("        options.set_keep_alive(Duration::from_secs(30));\n");
        sb.append("        options.set_clean_session(true);\n\n");

        sb.append("        let (client, mut eventloop) = AsyncClient::new(options, 10);\n\n");

        sb.append("        // Spawn event loop handler\n");
        sb.append("        tokio::spawn(async move {\n");
        sb.append("            loop {\n");
        sb.append("                match eventloop.poll().await {\n");
        sb.append("                    Ok(event) => {\n");
        sb.append("                        tracing::trace!(\"MQTT event: {:?}\", event);\n");
        sb.append("                    }\n");
        sb.append("                    Err(e) => {\n");
        sb.append("                        error!(\"MQTT error: {:?}\", e);\n");
        sb.append("                        tokio::time::sleep(Duration::from_secs(5)).await;\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        });\n\n");

        sb.append(
                "        info!(broker = %broker, port = %port, \"MQTT publisher connected\");\n\n");

        sb.append("        Ok(Self { client })\n");
        sb.append("    }\n\n");

        // Publish method
        sb.append("    /// Publishes a message to a topic.\n");
        sb.append(
                "    pub async fn publish<T: Serialize>(&self, topic: &str, payload: &T) ->"
                        + " Result<()> {\n");
        sb.append("        let json = serde_json::to_vec(payload)?;\n");
        sb.append("        self.client\n");
        sb.append("            .publish(topic, QoS::AtLeastOnce, false, json)\n");
        sb.append("            .await?;\n");
        sb.append("        info!(topic = %topic, \"Published MQTT message\");\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Publish raw bytes
        sb.append("    /// Publishes raw bytes to a topic.\n");
        sb.append(
                "    pub async fn publish_bytes(&self, topic: &str, payload: &[u8]) -> Result<()>"
                        + " {\n");
        sb.append("        self.client\n");
        sb.append("            .publish(topic, QoS::AtLeastOnce, false, payload)\n");
        sb.append("            .await?;\n");
        sb.append(
                "        info!(topic = %topic, bytes = payload.len(), \"Published MQTT"
                        + " bytes\");\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }

    /**
     * Generates the MQTT subscriber module.
     *
     * @return the subscriber module content
     */
    public String generateSubscriber() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! MQTT message subscriber.\n\n");

        sb.append("use anyhow::Result;\n");
        sb.append("use rumqttc::{AsyncClient, Event, MqttOptions, Packet, QoS};\n");
        sb.append("use std::env;\n");
        sb.append("use std::time::Duration;\n");
        sb.append("use tokio::sync::mpsc;\n");
        sb.append("use tracing::{error, info};\n");
        sb.append("\n");

        // Message struct
        sb.append("/// Received MQTT message.\n");
        sb.append("#[derive(Debug, Clone)]\n");
        sb.append("pub struct MqttMessage {\n");
        sb.append("    pub topic: String,\n");
        sb.append("    pub payload: Vec<u8>,\n");
        sb.append("}\n\n");

        // Subscriber struct
        sb.append("/// MQTT message subscriber.\n");
        sb.append("pub struct MqttSubscriber {\n");
        sb.append("    client: AsyncClient,\n");
        sb.append("    receiver: mpsc::Receiver<MqttMessage>,\n");
        sb.append("}\n\n");

        sb.append("impl MqttSubscriber {\n");

        // Constructor
        sb.append("    /// Creates a new MQTT subscriber.\n");
        sb.append("    pub async fn new(topics: Vec<String>) -> Result<Self> {\n");
        sb.append("        let broker = env::var(\"MQTT_BROKER\")\n");
        sb.append("            .unwrap_or_else(|_| \"localhost\".to_string());\n");
        sb.append("        let port: u16 = env::var(\"MQTT_PORT\")\n");
        sb.append("            .unwrap_or_else(|_| \"1883\".to_string())\n");
        sb.append("            .parse()\n");
        sb.append("            .unwrap_or(1883);\n");
        sb.append("        let client_id = env::var(\"MQTT_CLIENT_ID\")\n");
        sb.append("            .unwrap_or_else(|_| \"rust-api-subscriber\".to_string());\n\n");

        sb.append("        let mut options = MqttOptions::new(&client_id, &broker, port);\n");
        sb.append("        options.set_keep_alive(Duration::from_secs(30));\n");
        sb.append("        options.set_clean_session(true);\n\n");

        sb.append("        let (client, mut eventloop) = AsyncClient::new(options, 100);\n\n");

        sb.append("        // Subscribe to topics\n");
        sb.append("        for topic in &topics {\n");
        sb.append("            client.subscribe(topic, QoS::AtLeastOnce).await?;\n");
        sb.append("            info!(topic = %topic, \"Subscribed to MQTT topic\");\n");
        sb.append("        }\n\n");

        sb.append("        // Create message channel\n");
        sb.append("        let (sender, receiver) = mpsc::channel(100);\n\n");

        sb.append("        // Spawn event loop handler\n");
        sb.append("        tokio::spawn(async move {\n");
        sb.append("            loop {\n");
        sb.append("                match eventloop.poll().await {\n");
        sb.append("                    Ok(Event::Incoming(Packet::Publish(publish))) => {\n");
        sb.append("                        let msg = MqttMessage {\n");
        sb.append("                            topic: publish.topic.clone(),\n");
        sb.append("                            payload: publish.payload.to_vec(),\n");
        sb.append("                        };\n");
        sb.append("                        if sender.send(msg).await.is_err() {\n");
        sb.append("                            error!(\"Failed to send message to channel\");\n");
        sb.append("                            break;\n");
        sb.append("                        }\n");
        sb.append("                    }\n");
        sb.append("                    Ok(_) => {}\n");
        sb.append("                    Err(e) => {\n");
        sb.append("                        error!(\"MQTT error: {:?}\", e);\n");
        sb.append("                        tokio::time::sleep(Duration::from_secs(5)).await;\n");
        sb.append("                    }\n");
        sb.append("                }\n");
        sb.append("            }\n");
        sb.append("        });\n\n");

        sb.append(
                "        info!(broker = %broker, port = %port, \"MQTT subscriber"
                        + " connected\");\n\n");

        sb.append("        Ok(Self { client, receiver })\n");
        sb.append("    }\n\n");

        // Receive method
        sb.append("    /// Receives the next message.\n");
        sb.append("    pub async fn recv(&mut self) -> Option<MqttMessage> {\n");
        sb.append("        self.receiver.recv().await\n");
        sb.append("    }\n\n");

        // Subscribe method
        sb.append("    /// Subscribes to an additional topic.\n");
        sb.append("    pub async fn subscribe(&self, topic: &str) -> Result<()> {\n");
        sb.append("        self.client.subscribe(topic, QoS::AtLeastOnce).await?;\n");
        sb.append("        info!(topic = %topic, \"Subscribed to MQTT topic\");\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n");

        sb.append("}\n");

        return sb.toString();
    }
}
