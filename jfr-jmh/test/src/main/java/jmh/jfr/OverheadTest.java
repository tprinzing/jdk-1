package jmh.jfr;

import jdk.internal.event.EventServiceLookup;
import jdk.internal.event.SocketWriteLogger;
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

    @Fork(value = 1, jvmArgsAppend = {"-server", "-XX:+UseParallelGC"})
    @Benchmark
    public void testJFRDisabled(SkeletonFixture fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {"-server", "-XX:+UseParallelGC", "-XX:StartFlightRecording:jdk.SocketWrite#enabled=false"})
    @Benchmark
    public void testJFREnabledEventDisabled(JFREnabledEventDisabled fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {"-server", "-XX:+UseParallelGC", "-XX:StartFlightRecording:jdk.SocketWrite#enabled=true,jdk.SocketWrite#threshold=1s"})
    @Benchmark
    public void testJFREnabledEventNotEmitted(JFREnabledEventNotEmitted fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    @Fork(value = 1, jvmArgsAppend = {"-server", "-XX:+UseParallelGC", "-XX:StartFlightRecording:jdk.SocketWrite#enabled=true,jdk.SocketWrite#threshold=0ms"})
    @Benchmark
    public void testJFREnabledEventEmitted(JFREnabledEventEmitted fixture) throws IOException {
        fixture.write(writeBuf, 0, writeBuf.length);
    }

    /**
     * Fixture with boilerplate code for managing jfr events.  No
     * actual work is done to eliminate the I/O portion and measure
     * the overhead of fetching and using the service.
     */
    @State(Scope.Thread)
    public static class SkeletonFixture {

        public SkeletonFixture() {
            this(false);
        }

        public SkeletonFixture(boolean eventEnabled) {
            this.eventEnabled = eventEnabled;
        }

       private InetAddress remote;
       private final boolean eventEnabled;

       public InetAddress getInetAddress() {
           return remote;
       }

       public int getPort() {
           return 5000;
       }

       @Setup(Level.Trial)
       public void setup() throws IOException {
           remote = InetAddress.getLocalHost();
           boolean enabled = EventServiceLookup.lookup().socketWrite().isEnabled();
           if (enabled != eventEnabled) {
               throw new IllegalStateException("Event enabled was expected to be: "+eventEnabled);
           }
       }

       @TearDown(Level.Trial)
       public void teardown() throws Exception {

       }

       public void write(byte[] b, int off, int len) throws IOException {
           SocketWriteLogger writeEvents = EventServiceLookup.lookup().socketWrite();
           if (!writeEvents.isEnabled()) {
               writeMeasured(b, off, len);
               return;
           }
           Throwable thrown = null;
           int bytesWritten = 0;
           long start = 0;
           try {
               start = writeEvents.timestamp();
               writeMeasured(b, off, len);
               bytesWritten = len;
           } catch (Throwable t) {
               thrown = t;
               throw t;
           } finally {
               long duration = writeEvents.timestamp() - start;
               if (writeEvents.shouldCommit(duration)) {
                   InetAddress remote = getInetAddress();
                   writeEvents.commit(
                       start,
                       duration,
                       remote.getHostName(),
                       remote.getHostAddress(),
                       getPort(),
                       bytesWritten, writeEvents.stringifyOrNull(thrown));
               }
           }
       }

       private void writeMeasured(byte[] b, int off, int len) throws IOException {
       }
    }

    public static class JFREnabledEventDisabled extends SkeletonFixture {
    }
    public static class JFREnabledEventEmitted extends SkeletonFixture {
        public JFREnabledEventEmitted() {
            super(true);
        }
    }
    static public class JFREnabledEventNotEmitted extends SkeletonFixture {
        public JFREnabledEventNotEmitted() {
            super(true);
        }
    }
    public final byte[] writeBuf = "Sample Message".getBytes();
}
