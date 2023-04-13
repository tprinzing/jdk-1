package jmh.jfr;

import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@State(Scope.Thread)
public class SocketFixture {

    private ServerSocket ss;
    public final byte[] writeBuf;
    private Socket s;
    public OutputStream out;
    private Thread reader;

    public SocketFixture(byte[] buff) {
        writeBuf = buff;
    }

    public SocketFixture() {
        this("Sample Message".getBytes());
    }


    @Setup(Level.Trial)
    public void setup() throws IOException {
        ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(null);

        reader = new Thread(() -> {
            try {
                byte[] bs = new byte[writeBuf.length];
                try (Socket s = ss.accept(); InputStream is = s.getInputStream()) {
                    while (true) {
                        int bytesRead = is.read(bs, 0, bs.length);
                        if (bs[0] == 'q') {
                            break;
                        }
                    }
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
                System.exit(1);
            }
        });
        s = new Socket();
        s.connect(ss.getLocalSocketAddress());
        out = s.getOutputStream();
        reader.start();
    }

    @TearDown(Level.Trial)
    public void teardown() throws Exception {
        out.write("q".getBytes());
        out.flush();
        out.close();
        s.close();
        ss.close();
        out = null;
        s = null;
        ss = null;
        reader = null;
    }

}
