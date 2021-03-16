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

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.DiscardingSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoJob {

    protected static final Logger LOG = LoggerFactory.getLogger(DemoJob.class);

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.getConfig().setGlobalJobParameters(params);
        env.enableCheckpointing(30_000L);

        DataStream<String> stream =
                env.addSource(
                        new FlinkKafkaConsumer<>(
                                params.get("topic"),
                                new SimpleStringSchema(),
                                params.getProperties()));

        stream.map(new CPULoadMapper(params)).addSink(new DiscardingSink<>());
        env.execute("Rescalable Demo Job");
    }

    private static class CPULoadMapper extends RichMapFunction<String, String> {
        private final ParameterTool params;

        public CPULoadMapper(ParameterTool params) {
            this.params = params;
        }

        // Let's waste some CPU cycles
        @Override
        public String map(String s) throws Exception {
            LOG.info("Received message " + s);
            double res = 0;
            for (int i = 0; i < params.getInt("iterations", 500); i++) {
                res += Math.sin(StrictMath.cos(res)) * 2;
            }
            return s + res;
        }
    }
}
