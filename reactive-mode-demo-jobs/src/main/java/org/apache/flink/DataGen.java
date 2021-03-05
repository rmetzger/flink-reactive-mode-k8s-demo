package org.apache.flink;

import org.apache.flink.api.java.utils.ParameterTool;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class DataGen {
    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        final String topic = params.get("topic");
        final AtomicLong sleepEvery = new AtomicLong(params.getLong("sleepEvery", 500));
        Properties props = new Properties();
        props.put("bootstrap.servers", params.get("bootstrap"));
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("linger.ms", 1);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        Thread producerThread =
                new Thread(
                        () -> {
                            Producer<String, String> producer = new KafkaProducer<>(props);
                            long i = 0;
                            while (true) {
                                producer.send(
                                        new ProducerRecord<>(
                                                topic, Long.toString(i), Long.toString(i++)));
                                if (i % sleepEvery.get() == 0) {
                                    try {
                                        Thread.sleep(5);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            }
                        });

        producerThread.start();

        System.out.println("Kafka Producer started.");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.println("Enter new sleep:");
            sleepEvery.set(Long.parseLong(br.readLine()));
        }
    }
}
