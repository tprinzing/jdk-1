package jmh.jfr;

public interface EventNames {

    static String EVENT_PREFIX = "jdk.";
    static String EVENT_SOCKET_READ = EVENT_PREFIX+ "SocketRead";
    static String EVENT_SOCKET_WRITE = EVENT_PREFIX+ "SocketWrite";

    static String EVENT_FILE_WRITE = EVENT_PREFIX+ "FileWrite";
    static String EVENT_FILE_READ = EVENT_PREFIX+ "FileRead";
}
