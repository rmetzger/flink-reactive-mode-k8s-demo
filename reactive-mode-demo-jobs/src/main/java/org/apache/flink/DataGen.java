package org.apache.flink;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class DataGen {
    private static volatile boolean sendFailmessage = false;

    public static void main(String[] args) throws Exception {
        System.out.println("Args = " + Arrays.asList(args));
        final String topic = args[0];
        final AtomicLong sleepEvery = new AtomicLong(Long.parseLong(args[1]));
        Properties props = new Properties();
        props.put("bootstrap.servers", args[2]);
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
                                if (sendFailmessage) {
                                    producer.send(new ProducerRecord<>(topic, "fail", "fail"));
                                    sendFailmessage = false;
                                } else {
                                    producer.send(
                                            new ProducerRecord<>(
                                                    topic, Long.toString(i), Long.toString(i++)));
                                }

                                if (i % sleepEvery.get() == 0) {
                                    System.out.println(
                                            "Sleep interval "
                                                    + sleepEvery.get()
                                                    + " Kafka produced: "
                                                    + i);
                                    try {
                                        Thread.sleep(15_000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                        break;
                                    }
                                }
                            }
                        });

        producerThread.start();

        System.out.println("Kafka Producer started.");

        switch (args[3]) {
            case "manual":
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                while (true) {
                    System.out.println("Enter new sleep:");
                    String line = br.readLine();
                    System.out.println("Read " + line);
                    if (!line.isEmpty()) {
                        if (line.equals("fail")) {
                            sendFailmessage = true;
                        } else {
                            sleepEvery.set(Long.parseLong(line));
                        }
                    }
                }
            case "cos":
                long median = 50_000L;
                long current;
                double in = -3;
                int i = 0;

                while (true) {
                    current = median + (long) (median * Math.cos(in));
                    sleepEvery.set(current);
                    in += 0.04;
                    System.out.println("At time " + (i++) + " Setting current " + current);
                    Thread.sleep(60 * 1000L); // once per minute.
                }

            default:
                throw new IllegalArgumentException("unexpected mode " + args[3]);
        }
    }
}
