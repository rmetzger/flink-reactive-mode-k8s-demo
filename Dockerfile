FROM rmetzger/flink:1.13.0-reactive-timeouts-v2-57d9b95fac95a649bd8295c294b77aeee5df12fc

COPY reactive-mode-demo-jobs/target/reactive-mode-demo-jobs-1.0-SNAPSHOT.jar /opt/flink/lib/reactive-mode-demo-jobs-1.0-SNAPSHOT.jar