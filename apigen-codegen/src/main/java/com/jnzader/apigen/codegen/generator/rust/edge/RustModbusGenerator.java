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
 * Generates Modbus TCP/RTU client code using tokio-modbus.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings({"java:S1068", "java:S1192"}) // S1068: reserved fields; S1192: template strings
public class RustModbusGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustModbusGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the modbus/mod.rs file.
     *
     * @return the mod.rs content
     */
    public String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Modbus TCP/RTU client.\n\n");

        sb.append("mod client;\n");
        sb.append("\n");
        sb.append("pub use client::ModbusClient;\n");

        return sb.toString();
    }

    /**
     * Generates the Modbus client module.
     *
     * @return the client module content
     */
    public String generateClient() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Modbus TCP client implementation.\n\n");

        sb.append("use anyhow::Result;\n");
        sb.append("use std::net::SocketAddr;\n");
        sb.append("use tokio_modbus::prelude::*;\n");
        sb.append("use tracing::info;\n");
        sb.append("\n");

        // ModbusClient struct
        sb.append("/// Modbus TCP client for reading and writing registers.\n");
        sb.append("pub struct ModbusClient {\n");
        sb.append("    ctx: client::Context,\n");
        sb.append("}\n\n");

        sb.append("impl ModbusClient {\n");

        // Constructor for TCP
        sb.append("    /// Creates a new Modbus TCP client.\n");
        sb.append("    pub async fn new_tcp(addr: &str) -> Result<Self> {\n");
        sb.append("        let socket_addr: SocketAddr = addr.parse()?;\n");
        sb.append("        let ctx = tcp::connect(socket_addr).await?;\n");
        sb.append("        info!(addr = %addr, \"Connected to Modbus TCP device\");\n");
        sb.append("        Ok(Self { ctx })\n");
        sb.append("    }\n\n");

        // Read holding registers
        sb.append("    /// Reads holding registers (function code 0x03).\n");
        sb.append("    pub async fn read_holding_registers(\n");
        sb.append("        &mut self,\n");
        sb.append("        address: u16,\n");
        sb.append("        count: u16,\n");
        sb.append("    ) -> Result<Vec<u16>> {\n");
        sb.append("        let data = self.ctx.read_holding_registers(address, count).await?;\n");
        sb.append("        Ok(data.into_iter().collect())\n");
        sb.append("    }\n\n");

        // Read input registers
        sb.append("    /// Reads input registers (function code 0x04).\n");
        sb.append("    pub async fn read_input_registers(\n");
        sb.append("        &mut self,\n");
        sb.append("        address: u16,\n");
        sb.append("        count: u16,\n");
        sb.append("    ) -> Result<Vec<u16>> {\n");
        sb.append("        let data = self.ctx.read_input_registers(address, count).await?;\n");
        sb.append("        Ok(data.into_iter().collect())\n");
        sb.append("    }\n\n");

        // Read coils
        sb.append("    /// Reads coils (function code 0x01).\n");
        sb.append(
                "    pub async fn read_coils(&mut self, address: u16, count: u16) ->"
                        + " Result<Vec<bool>> {\n");
        sb.append("        let data = self.ctx.read_coils(address, count).await?;\n");
        sb.append("        Ok(data.into_iter().collect())\n");
        sb.append("    }\n\n");

        // Read discrete inputs
        sb.append("    /// Reads discrete inputs (function code 0x02).\n");
        sb.append("    pub async fn read_discrete_inputs(\n");
        sb.append("        &mut self,\n");
        sb.append("        address: u16,\n");
        sb.append("        count: u16,\n");
        sb.append("    ) -> Result<Vec<bool>> {\n");
        sb.append("        let data = self.ctx.read_discrete_inputs(address, count).await?;\n");
        sb.append("        Ok(data.into_iter().collect())\n");
        sb.append("    }\n\n");

        // Write single register
        sb.append("    /// Writes a single register (function code 0x06).\n");
        sb.append(
                "    pub async fn write_single_register(&mut self, address: u16, value: u16) ->"
                        + " Result<()> {\n");
        sb.append("        self.ctx.write_single_register(address, value).await?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Write multiple registers
        sb.append("    /// Writes multiple registers (function code 0x10).\n");
        sb.append("    pub async fn write_multiple_registers(\n");
        sb.append("        &mut self,\n");
        sb.append("        address: u16,\n");
        sb.append("        values: &[u16],\n");
        sb.append("    ) -> Result<()> {\n");
        sb.append("        self.ctx.write_multiple_registers(address, values).await?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Write single coil
        sb.append("    /// Writes a single coil (function code 0x05).\n");
        sb.append(
                "    pub async fn write_single_coil(&mut self, address: u16, value: bool) ->"
                        + " Result<()> {\n");
        sb.append("        self.ctx.write_single_coil(address, value).await?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Write multiple coils
        sb.append("    /// Writes multiple coils (function code 0x0F).\n");
        sb.append("    pub async fn write_multiple_coils(\n");
        sb.append("        &mut self,\n");
        sb.append("        address: u16,\n");
        sb.append("        values: &[bool],\n");
        sb.append("    ) -> Result<()> {\n");
        sb.append("        self.ctx.write_multiple_coils(address, values).await?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n");

        sb.append("}\n\n");

        // Helper functions for data conversion
        sb.append("/// Converts two u16 registers to a f32 (big-endian).\n");
        sb.append("pub fn registers_to_f32(high: u16, low: u16) -> f32 {\n");
        sb.append("    let bytes = [high.to_be_bytes(), low.to_be_bytes()].concat();\n");
        sb.append("    f32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])\n");
        sb.append("}\n\n");

        sb.append("/// Converts a f32 to two u16 registers (big-endian).\n");
        sb.append("pub fn f32_to_registers(value: f32) -> (u16, u16) {\n");
        sb.append("    let bytes = value.to_be_bytes();\n");
        sb.append("    let high = u16::from_be_bytes([bytes[0], bytes[1]]);\n");
        sb.append("    let low = u16::from_be_bytes([bytes[2], bytes[3]]);\n");
        sb.append("    (high, low)\n");
        sb.append("}\n\n");

        sb.append("/// Converts two u16 registers to a i32 (big-endian).\n");
        sb.append("pub fn registers_to_i32(high: u16, low: u16) -> i32 {\n");
        sb.append("    let bytes = [high.to_be_bytes(), low.to_be_bytes()].concat();\n");
        sb.append("    i32::from_be_bytes([bytes[0], bytes[1], bytes[2], bytes[3]])\n");
        sb.append("}\n\n");

        sb.append("/// Converts an i32 to two u16 registers (big-endian).\n");
        sb.append("pub fn i32_to_registers(value: i32) -> (u16, u16) {\n");
        sb.append("    let bytes = value.to_be_bytes();\n");
        sb.append("    let high = u16::from_be_bytes([bytes[0], bytes[1]]);\n");
        sb.append("    let low = u16::from_be_bytes([bytes[2], bytes[3]]);\n");
        sb.append("    (high, low)\n");
        sb.append("}\n");

        return sb.toString();
    }
}
