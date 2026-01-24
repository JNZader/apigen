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
 * Generates ONNX Runtime inference code.
 *
 * @author APiGen
 * @since 2.12.0
 */
public class RustOnnxGenerator {

    private final RustTypeMapper typeMapper;
    private final RustAxumOptions options;

    public RustOnnxGenerator(RustTypeMapper typeMapper, RustAxumOptions options) {
        this.typeMapper = typeMapper;
        this.options = options;
    }

    /**
     * Generates the inference/mod.rs file.
     *
     * @return the mod.rs content
     */
    public String generateModRs() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! Machine learning inference with ONNX Runtime.\n\n");

        sb.append("mod onnx;\n");
        sb.append("\n");
        sb.append("pub use onnx::OnnxInference;\n");

        return sb.toString();
    }

    /**
     * Generates the ONNX inference module.
     *
     * @return the onnx module content
     */
    public String generateOnnx() {
        StringBuilder sb = new StringBuilder();
        sb.append("//! ONNX Runtime inference implementation.\n\n");

        sb.append("use anyhow::{Context, Result};\n");
        sb.append("use ort::{\n");
        sb.append("    session::{Session, SessionOutputs},\n");
        sb.append("    value::Value,\n");
        sb.append("};\n");
        sb.append("use std::path::Path;\n");
        sb.append("use tracing::info;\n");
        if (options.useNdarray()) {
            sb.append("use ndarray::{Array, ArrayD, IxDyn};\n");
        }
        sb.append("\n");

        // OnnxInference struct
        sb.append("/// ONNX Runtime inference wrapper.\n");
        sb.append("pub struct OnnxInference {\n");
        sb.append("    session: Session,\n");
        sb.append("}\n\n");

        sb.append("impl OnnxInference {\n");

        // Constructor
        sb.append("    /// Creates a new ONNX inference session from a model file.\n");
        sb.append("    pub fn new<P: AsRef<Path>>(model_path: P) -> Result<Self> {\n");
        sb.append("        let session = Session::builder()?\n");
        sb.append(
                "           "
                    + " .with_optimization_level(ort::session::GraphOptimizationLevel::Level3)?\n");
        sb.append("            .with_intra_threads(4)?\n");
        sb.append("            .commit_from_file(model_path.as_ref())?;\n\n");
        sb.append("        info!(\"ONNX model loaded: {:?}\", model_path.as_ref());\n\n");
        sb.append("        Ok(Self { session })\n");
        sb.append("    }\n\n");

        // Constructor from bytes
        sb.append("    /// Creates a new ONNX inference session from model bytes.\n");
        sb.append("    pub fn from_bytes(model_bytes: &[u8]) -> Result<Self> {\n");
        sb.append("        let session = Session::builder()?\n");
        sb.append(
                "           "
                    + " .with_optimization_level(ort::session::GraphOptimizationLevel::Level3)?\n");
        sb.append("            .with_intra_threads(4)?\n");
        sb.append("            .commit_from_memory(model_bytes)?;\n\n");
        sb.append("        info!(\"ONNX model loaded from bytes\");\n\n");
        sb.append("        Ok(Self { session })\n");
        sb.append("    }\n\n");

        // Get input names
        sb.append("    /// Returns the input names of the model.\n");
        sb.append("    pub fn input_names(&self) -> Vec<&str> {\n");
        sb.append("        self.session.inputs.iter().map(|i| i.name.as_str()).collect()\n");
        sb.append("    }\n\n");

        // Get output names
        sb.append("    /// Returns the output names of the model.\n");
        sb.append("    pub fn output_names(&self) -> Vec<&str> {\n");
        sb.append("        self.session.outputs.iter().map(|o| o.name.as_str()).collect()\n");
        sb.append("    }\n\n");

        // Run inference with f32 input
        sb.append("    /// Runs inference with a single f32 tensor input.\n");
        sb.append("    pub fn run_f32(\n");
        sb.append("        &self,\n");
        sb.append("        input_name: &str,\n");
        sb.append("        input_data: Vec<f32>,\n");
        sb.append("        shape: Vec<i64>,\n");
        sb.append("    ) -> Result<Vec<f32>> {\n");
        sb.append(
                "        let input = Value::from_array((shape,"
                        + " input_data.into_boxed_slice()))?;\n\n");
        sb.append("        let outputs: SessionOutputs = self.session.run(\n");
        sb.append("            ort::inputs![input_name => input]?\n");
        sb.append("        )?;\n\n");
        sb.append("        let output = outputs\n");
        sb.append("            .get(self.session.outputs[0].name.as_str())\n");
        sb.append("            .context(\"Failed to get output\")?;\n\n");
        sb.append("        let tensor = output.try_extract_tensor::<f32>()?;\n");
        sb.append("        Ok(tensor.view().iter().copied().collect())\n");
        sb.append("    }\n\n");

        // Run inference with i64 input (for tokenized text)
        sb.append("    /// Runs inference with a single i64 tensor input (for tokenized text).\n");
        sb.append("    pub fn run_i64(\n");
        sb.append("        &self,\n");
        sb.append("        input_name: &str,\n");
        sb.append("        input_data: Vec<i64>,\n");
        sb.append("        shape: Vec<i64>,\n");
        sb.append("    ) -> Result<Vec<f32>> {\n");
        sb.append(
                "        let input = Value::from_array((shape,"
                        + " input_data.into_boxed_slice()))?;\n\n");
        sb.append("        let outputs: SessionOutputs = self.session.run(\n");
        sb.append("            ort::inputs![input_name => input]?\n");
        sb.append("        )?;\n\n");
        sb.append("        let output = outputs\n");
        sb.append("            .get(self.session.outputs[0].name.as_str())\n");
        sb.append("            .context(\"Failed to get output\")?;\n\n");
        sb.append("        let tensor = output.try_extract_tensor::<f32>()?;\n");
        sb.append("        Ok(tensor.view().iter().copied().collect())\n");
        sb.append("    }\n\n");

        // Run with multiple inputs
        sb.append("    /// Runs inference with multiple named inputs.\n");
        sb.append("    pub fn run_multi(\n");
        sb.append("        &self,\n");
        sb.append("        inputs: Vec<(&str, Vec<f32>, Vec<i64>)>,\n");
        sb.append("    ) -> Result<Vec<Vec<f32>>> {\n");
        sb.append("        let mut ort_inputs = Vec::new();\n");
        sb.append("        let mut values = Vec::new();\n\n");
        sb.append("        for (name, data, shape) in inputs {\n");
        sb.append(
                "            let value = Value::from_array((shape, data.into_boxed_slice()))?;\n");
        sb.append("            values.push(value);\n");
        sb.append("        }\n\n");
        sb.append("        for (i, (name, _, _)) in inputs.iter().enumerate() {\n");
        sb.append("            ort_inputs.push((*name, &values[i]));\n");
        sb.append("        }\n\n");
        sb.append(
                "        // Note: This is simplified; actual multi-input would need proper"
                        + " handling\n");
        sb.append("        let outputs: SessionOutputs = self.session.run(\n");
        sb.append("            ort::inputs![\"input\" => &values[0]]?\n");
        sb.append("        )?;\n\n");
        sb.append("        let mut results = Vec::new();\n");
        sb.append("        for output_info in &self.session.outputs {\n");
        sb.append("            if let Ok(output) = outputs.get(output_info.name.as_str()) {\n");
        sb.append("                let tensor = output.try_extract_tensor::<f32>()?;\n");
        sb.append("                results.push(tensor.view().iter().copied().collect());\n");
        sb.append("            }\n");
        sb.append("        }\n\n");
        sb.append("        Ok(results)\n");
        sb.append("    }\n");

        sb.append("}\n\n");

        // Helper functions
        sb.append("/// Softmax function for classification outputs.\n");
        sb.append("pub fn softmax(logits: &[f32]) -> Vec<f32> {\n");
        sb.append("    let max = logits.iter().cloned().fold(f32::NEG_INFINITY, f32::max);\n");
        sb.append("    let exp_sum: f32 = logits.iter().map(|x| (x - max).exp()).sum();\n");
        sb.append("    logits.iter().map(|x| (x - max).exp() / exp_sum).collect()\n");
        sb.append("}\n\n");

        sb.append("/// Argmax function to get the index of the maximum value.\n");
        sb.append("pub fn argmax(values: &[f32]) -> usize {\n");
        sb.append("    values\n");
        sb.append("        .iter()\n");
        sb.append("        .enumerate()\n");
        sb.append("        .max_by(|(_, a), (_, b)| a.partial_cmp(b).unwrap())\n");
        sb.append("        .map(|(i, _)| i)\n");
        sb.append("        .unwrap_or(0)\n");
        sb.append("}\n\n");

        sb.append("/// Top-k function to get the indices of the k highest values.\n");
        sb.append("pub fn top_k(values: &[f32], k: usize) -> Vec<(usize, f32)> {\n");
        sb.append(
                "    let mut indexed: Vec<(usize, f32)> ="
                        + " values.iter().cloned().enumerate().collect();\n");
        sb.append("    indexed.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());\n");
        sb.append("    indexed.into_iter().take(k).collect()\n");
        sb.append("}\n");

        return sb.toString();
    }
}
