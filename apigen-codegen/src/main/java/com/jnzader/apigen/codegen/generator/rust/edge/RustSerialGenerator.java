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
 * Generates serial port communication code using serialport-rs.
 *
 * @author APiGen
 * @since 2.12.0
 */
@SuppressWarnings("UnusedVariable") // Reserved for future feature flags
public class RustSerialGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustSerialGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the serial/mod.rs file.
     *
     * @return the mod.rs content
     */
    public String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Serial port communication.\n\n");

        sb.append("mod port;\n");
        sb.append("\n");
        sb.append("pub use port::SerialPort;\n");

        return sb.toString();
    }

    /**
     * Generates the serial port module.
     *
     * @return the port module content
     */
    public String generatePort() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Serial port implementation.\n\n");

        sb.append("use anyhow::Result;\n");
        sb.append("use serialport::{self, DataBits, FlowControl, Parity, StopBits};\n");
        sb.append("use std::io::{Read, Write};\n");
        sb.append("use std::time::Duration;\n");
        sb.append("use tracing::info;\n");
        sb.append("\n");

        // SerialPort struct
        sb.append("/// Serial port wrapper for communication.\n");
        sb.append("pub struct SerialPort {\n");
        sb.append("    port: Box<dyn serialport::SerialPort>,\n");
        sb.append("}\n\n");

        sb.append("impl SerialPort {\n");

        // Constructor
        sb.append("    /// Opens a serial port with the specified configuration.\n");
        sb.append("    pub fn open(\n");
        sb.append("        port_name: &str,\n");
        sb.append("        baud_rate: u32,\n");
        sb.append("    ) -> Result<Self> {\n");
        sb.append("        let port = serialport::new(port_name, baud_rate)\n");
        sb.append("            .data_bits(DataBits::Eight)\n");
        sb.append("            .parity(Parity::None)\n");
        sb.append("            .stop_bits(StopBits::One)\n");
        sb.append("            .flow_control(FlowControl::None)\n");
        sb.append("            .timeout(Duration::from_millis(1000))\n");
        sb.append("            .open()?;\n\n");
        sb.append(
                "        info!(port = %port_name, baud_rate = %baud_rate, \"Serial port"
                        + " opened\");\n\n");
        sb.append("        Ok(Self { port })\n");
        sb.append("    }\n\n");

        // Open with full config
        sb.append("    /// Opens a serial port with full configuration.\n");
        sb.append("    pub fn open_with_config(\n");
        sb.append("        port_name: &str,\n");
        sb.append("        baud_rate: u32,\n");
        sb.append("        data_bits: DataBits,\n");
        sb.append("        parity: Parity,\n");
        sb.append("        stop_bits: StopBits,\n");
        sb.append("        timeout_ms: u64,\n");
        sb.append("    ) -> Result<Self> {\n");
        sb.append("        let port = serialport::new(port_name, baud_rate)\n");
        sb.append("            .data_bits(data_bits)\n");
        sb.append("            .parity(parity)\n");
        sb.append("            .stop_bits(stop_bits)\n");
        sb.append("            .flow_control(FlowControl::None)\n");
        sb.append("            .timeout(Duration::from_millis(timeout_ms))\n");
        sb.append("            .open()?;\n\n");
        sb.append(
                "        info!(port = %port_name, baud_rate = %baud_rate, \"Serial port"
                        + " opened\");\n\n");
        sb.append("        Ok(Self { port })\n");
        sb.append("    }\n\n");

        // Write
        sb.append("    /// Writes data to the serial port.\n");
        sb.append("    pub fn write(&mut self, data: &[u8]) -> Result<usize> {\n");
        sb.append("        let bytes_written = self.port.write(data)?;\n");
        sb.append("        self.port.flush()?;\n");
        sb.append("        Ok(bytes_written)\n");
        sb.append("    }\n\n");

        // Write string
        sb.append("    /// Writes a string to the serial port.\n");
        sb.append("    pub fn write_str(&mut self, data: &str) -> Result<usize> {\n");
        sb.append("        self.write(data.as_bytes())\n");
        sb.append("    }\n\n");

        // Read
        sb.append("    /// Reads data from the serial port.\n");
        sb.append("    pub fn read(&mut self, buffer: &mut [u8]) -> Result<usize> {\n");
        sb.append("        let bytes_read = self.port.read(buffer)?;\n");
        sb.append("        Ok(bytes_read)\n");
        sb.append("    }\n\n");

        // Read line
        sb.append("    /// Reads a line (until newline) from the serial port.\n");
        sb.append("    pub fn read_line(&mut self) -> Result<String> {\n");
        sb.append("        let mut buffer = Vec::new();\n");
        sb.append("        let mut byte = [0u8; 1];\n\n");
        sb.append("        loop {\n");
        sb.append("            match self.port.read(&mut byte) {\n");
        sb.append("                Ok(1) => {\n");
        sb.append("                    if byte[0] == b'\\n' {\n");
        sb.append("                        break;\n");
        sb.append("                    }\n");
        sb.append("                    buffer.push(byte[0]);\n");
        sb.append("                }\n");
        sb.append("                Ok(_) => break,\n");
        sb.append("                Err(e) if e.kind() == std::io::ErrorKind::TimedOut => break,\n");
        sb.append("                Err(e) => return Err(e.into()),\n");
        sb.append("            }\n");
        sb.append("        }\n\n");
        sb.append("        Ok(String::from_utf8_lossy(&buffer).trim().to_string())\n");
        sb.append("    }\n\n");

        // Read available
        sb.append("    /// Reads all available data from the serial port.\n");
        sb.append("    pub fn read_available(&mut self) -> Result<Vec<u8>> {\n");
        sb.append("        let available = self.port.bytes_to_read()? as usize;\n");
        sb.append("        if available == 0 {\n");
        sb.append("            return Ok(Vec::new());\n");
        sb.append("        }\n\n");
        sb.append("        let mut buffer = vec![0u8; available];\n");
        sb.append("        let bytes_read = self.port.read(&mut buffer)?;\n");
        sb.append("        buffer.truncate(bytes_read);\n");
        sb.append("        Ok(buffer)\n");
        sb.append("    }\n\n");

        // Clear buffers
        sb.append("    /// Clears the input and output buffers.\n");
        sb.append("    pub fn clear_buffers(&self) -> Result<()> {\n");
        sb.append("        self.port.clear(serialport::ClearBuffer::All)?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n\n");

        // Set timeout
        sb.append("    /// Sets the read/write timeout.\n");
        sb.append("    pub fn set_timeout(&mut self, timeout_ms: u64) -> Result<()> {\n");
        sb.append("        self.port.set_timeout(Duration::from_millis(timeout_ms))?;\n");
        sb.append("        Ok(())\n");
        sb.append("    }\n");

        sb.append("}\n\n");

        // List available ports
        sb.append("/// Lists all available serial ports.\n");
        sb.append("pub fn list_ports() -> Result<Vec<String>> {\n");
        sb.append("    let ports = serialport::available_ports()?;\n");
        sb.append("    Ok(ports.into_iter().map(|p| p.port_name).collect())\n");
        sb.append("}\n");

        return sb.toString();
    }
}
