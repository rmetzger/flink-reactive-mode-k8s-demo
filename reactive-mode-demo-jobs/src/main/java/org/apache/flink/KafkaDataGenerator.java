package org.apache.flink;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Date;
import java.util.Random;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.flink.util.XORShiftRandom;

public class KafkaDataGenerator {

	private static String[] requestType = {"GET", "POST", "PUT", "DELETE"};

	private final static long waitTime = 10;

	//
	//	Program
	//

	public static void main(String[] args) throws Exception {

		// set up the execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		if (args.length != 4) {
			System.out.println(" Usage:");
			System.out.println("\tKafkaDataGenerator <numberOfSenders> <kafkaTopic> <kafkaBrokerAddr>" +
				" <maximumNumberOfElements>");
			return;
		}

		int numberOfSenders = Integer.parseInt(args[0]);
		String kafkaTopic = args[1];
		String kafkaBrokerAddr = args[2];
		final long maximumNumberOfElements = Long.parseLong(args[3]);

		// get input data
		DataStreamSource<Long> seq = env.generateSequence(0, numberOfSenders);
		seq.flatMap(new FlatMapFunction<Long, String>() {
			@Override
			public void flatMap(Long value, Collector<String> out) throws Exception {
				Random rnd = new XORShiftRandom();
				StringBuffer sb = new StringBuffer();
				long element = 0;
				while (element < maximumNumberOfElements) {
					// write ip:
					sb.append("FROM:");
					sb.append(value);
					sb.append(" ELEMENT:");
					sb.append(element++);
					// write ip:
					sb.append(" ");
					sb.append(rnd.nextInt(255)).append('.').append(rnd.nextInt(255)).append('.').append(rnd.nextInt(255)).append('.').append(rnd.nextInt(255));
					sb.append(" - - ["); // some spaces
					sb.append((new Date(Math.abs(rnd.nextLong())).toString()));
					sb.append("] \"");
					sb.append(requestType[rnd.nextInt(requestType.length - 1)]);
					sb.append(' ');
					if (rnd.nextBoolean()) {
						// access to album
						sb.append("/album.php?picture=").append(rnd.nextInt());
					} else {
						// access search
						sb.append("/search.php?term=");
						int terms = rnd.nextInt(8);
						for (int i = 0; i < terms; i++) {
							sb.append("Yolo").append('+');
						}
					}
					sb.append(" HTTP/1.1\" ");
					/*if(sb.charAt(sb.length()-1) != '\n') {
						sb.append('\n');
					} */
					final String str = sb.toString();
					sb.delete(0, sb.length());
					out.collect(str);

					Thread.sleep(waitTime);
				}

			}
		}).addSink(new KafkaSink<String>(kafkaBrokerAddr, kafkaTopic, new KafkaStringSerializationSchema()));

		// execute program
		env.execute("Spill some data into Kafka");
	}

}
