package com.jnzader.apigen.core.benchmark;

import com.jnzader.apigen.core.application.util.Result;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

/**
 * JMH benchmarks for the Result class. Measures performance of success/failure creation and
 * transformation operations.
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ResultBenchmark {

    private Result<String, String> successResult;
    private Result<String, String> failureResult;

    @Setup
    public void setup() {
        successResult = Result.success("test-value");
        failureResult = Result.failure("test-error");
    }

    @Benchmark
    public void benchmarkCreateSuccess(Blackhole bh) {
        bh.consume(Result.success("value"));
    }

    @Benchmark
    public void benchmarkCreateFailure(Blackhole bh) {
        bh.consume(Result.failure("error"));
    }

    @Benchmark
    public void benchmarkMap(Blackhole bh) {
        bh.consume(successResult.map(String::toUpperCase));
    }

    @Benchmark
    public void benchmarkFlatMap(Blackhole bh) {
        bh.consume(successResult.flatMap(v -> Result.success(v.toUpperCase())));
    }

    @Benchmark
    public void benchmarkMapError(Blackhole bh) {
        bh.consume(failureResult.mapError(String::toUpperCase));
    }

    @Benchmark
    public void benchmarkIsSuccess(Blackhole bh) {
        bh.consume(successResult.isSuccess());
        bh.consume(failureResult.isSuccess());
    }

    @Benchmark
    public void benchmarkOrElse(Blackhole bh) {
        bh.consume(successResult.orElse("default"));
        bh.consume(failureResult.orElse("default"));
    }

    @Benchmark
    public void benchmarkFold(Blackhole bh) {
        bh.consume(successResult.fold(v -> "success", e -> "error"));
        bh.consume(failureResult.fold(v -> "success", e -> "error"));
    }

    @Benchmark
    public void benchmarkRecover(Blackhole bh) {
        bh.consume(failureResult.recover(e -> "recovered"));
    }

    @Benchmark
    public void benchmarkFilter(Blackhole bh) {
        bh.consume(successResult.filter(v -> v.length() > 3, () -> "too short"));
    }
}
