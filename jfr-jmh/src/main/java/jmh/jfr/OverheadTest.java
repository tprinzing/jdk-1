package jmh.jfr;

import jdk.internal.event.SocketWriteEvent;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Baseline test of boilerplate code to quantify the overhead of
 * it without the latencies of the I/O code.  This is intended to
 * facilitate experiments with consolidating the boilerplate to
 * improve maintainability.  It also enables calculations of the
 * jfr overhead.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Thread)
public class OverheadTest {

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
            .include(SocketWrite.class.getSimpleName())
            .forks(1)
            .jvmArgs("-ea")
            .build();

        new Runner(opt).run();
        //org.openjdk.jmh.Main.main(args);
    }

    @Fork(value = 1, jvmArgsAppend = {
        "--add-exports",
        "java.base/jdk.internal.event=ALL-UNNAMED",
        "-server",
        "-XX:+UseParallelGC"})
    @Benchmark
    public void testJFRDisabled(SkeletonFixture fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {
        "--add-exports",
        "java.base/jdk.internal.event=ALL-UNNAMED",
        "-server",
        "-XX:+UseParallelGC",
        "-XX:StartFlightRecording:jdk.SocketWrite#enabled=false"})
    @Benchmark
    public void testJFREnabledEventDisabled(SkeletonFixture fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {
        "--add-exports",
        "java.base/jdk.internal.event=ALL-UNNAMED",
        "-server",
        "-XX:+UseParallelGC",
        "-XX:StartFlightRecording:jdk.SocketWrite#enabled=true,jdk.SocketWrite#threshold=1s"})
    @Benchmark
    public void testJFREnabledEventNotEmitted(SkeletonFixture fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {
        "--add-exports","java.base/jdk.internal.event=ALL-UNNAMED", 
        "-server", 
        "-XX:+UseParallelGC", 
        "-XX:StartFlightRecording:jdk.SocketWrite#enabled=true,jdk.SocketWrite#threshold=0ms,disk=false,jdk.SocketWrite#stackTrace=false"})
    @Benchmark
    public void testJFREnabledEventEmitted(SkeletonFixture fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    /**
     * Fixture with boilerplate code for managing jfr events.  No
     * actual work is done to eliminate the I/O portion and measure
     * the overhead of fetching and using the service.
     */
    @State(Scope.Thread)
    public static class SkeletonFixture {

       private InetAddress remote;

       public InetAddress getInetAddress() {
           return remote;
       }

       public int getPort() {
           return 5000;
       }

       @Setup(Level.Trial)
       public void setup() throws IOException {
           remote = InetAddress.getLocalHost();
       }

       @TearDown(Level.Trial)
       public void teardown() throws Exception {

       }

       public void write(byte[] b, int off, int len) throws IOException {
           if (!SocketWriteEvent.enabled()) {
               writeMeasured(b, off, len);
               return;
           }
           int bytesWritten = 0;
           long start = 0;
           try {
               start = SocketWriteEvent.timestamp();
               writeMeasured(b, off, len);
               bytesWritten = len;
           } finally {
               long duration = SocketWriteEvent.timestamp() - start;
               if (SocketWriteEvent.shouldCommit(duration)) {
                   SocketWriteEvent.commit(
                       start,
                       duration,
                       remote.getHostName(),
                       remote.getHostAddress(),
                       getPort(),
                       bytesWritten);
               }
           }
       }

       private void writeMeasured(byte[] b, int off, int len) throws IOException {
       }
    }

    public final byte[] writeBuf = "Sample Message".getBytes();
}
