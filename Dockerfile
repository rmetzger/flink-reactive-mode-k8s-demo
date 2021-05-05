FROM apache/flink:1.13.0

COPY reactive-mode-demo-jobs/target/reactive-mode-demo-jobs-1.0-SNAPSHOT.jar /opt/flink/usrlib/reactive-mode-demo-jobs-1.0-SNAPSHOT.jar