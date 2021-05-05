/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoJob {

    protected static final Logger LOG = LoggerFactory.getLogger(DemoJob.class);

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);
        env.disableOperatorChaining();
        env.enableCheckpointing(30_000L);

        DataStream<String> stream =
                env.addSource(
                                new FlinkKafkaConsumer<>(
                                        params.get("topic"),
                                        new SimpleStringSchema(),
                                        params.getProperties()))
                        .setMaxParallelism(1)
                        .name("Kafka");

        stream.flatMap(new ThroughputLogger(16, params.getLong("logfreq", 10_000)))
                .setMaxParallelism(1)
                .name("Throughput Logger");

        stream.rebalance()
                .map(new CPULoadMapper(params))
                .name("Expensive Computation")
                .addSink(new DiscardingSink<>())
                .name("Sink")
                .setParallelism(9999);
        env.execute("Rescalable Demo Job");
    }

    private static class CPULoadMapper extends RichMapFunction<String, String> {
        private final ParameterTool params;
        private boolean firstMessage = false;

        public CPULoadMapper(ParameterTool params) {
            this.params = params;
        }

        // Let's waste some CPU cycles
        @Override
        public String map(String s) throws Exception {
            if (!firstMessage) {
                LOG.info("Received message " + s);
                firstMessage = true;
            }
            if (s.equals("fail")) {
                throw new RuntimeException("Artificial failure");
            }
            double res = 0;
            for (int i = 0; i < params.getInt("iterations", 500); i++) {
                res += Math.sin(StrictMath.cos(res)) * 2;
            }
            return s + res;
        }
    }

    private static class ThroughputLogger<T> implements FlatMapFunction<T, Integer> {

        private static final Logger LOG = LoggerFactory.getLogger(ThroughputLogger.class);

        private long totalReceived = 0;
        private long lastTotalReceived = 0;
        private long lastLogTimeMs = -1;
        private int elementSize;
        private long logfreq;

        public ThroughputLogger(int elementSize, long logfreq) {
            this.elementSize = elementSize;
            this.logfreq = logfreq;
        }

        @Override
        public void flatMap(T element, Collector<Integer> collector) throws Exception {
            totalReceived++;
            if (totalReceived % logfreq == 0) {
                // throughput over entire time
                long now = System.currentTimeMillis();

                // throughput for the last "logfreq" elements
                if (lastLogTimeMs == -1) {
                    // init (the first)
                    lastLogTimeMs = now;
                    lastTotalReceived = totalReceived;
                } else {
                    long timeDiff = now - lastLogTimeMs;
                    long elementDiff = totalReceived - lastTotalReceived;
                    double ex = (1000 / (double) timeDiff);
                    LOG.info(
                            "During the last {} ms, we received {} elements. That's {} elements/second/core. {} MB/sec/core. GB received {}",
                            timeDiff,
                            elementDiff,
                            elementDiff * ex,
                            elementDiff * ex * elementSize / 1024 / 1024,
                            (totalReceived * elementSize) / 1024 / 1024 / 1024);
                    // reinit
                    lastLogTimeMs = now;
                    lastTotalReceived = totalReceived;
                }
            }
        }
    }
}
