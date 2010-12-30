package org.jkiss.dbeaver.utils.dummyserver;

import java.net.ServerSocket;
import java.net.Socket;

/**
 * DummyServer
 */
public class DummyServer
{
    public static void main(String[] args)
        throws Exception
    {
        if (args.length < 1) {
            System.out.println("Port not specified");
            System.exit(1);
        }
        int portNum = Integer.parseInt(args[0]);
        ServerSocket serverSocket = new ServerSocket(portNum);
        Socket socket = serverSocket.accept();
        Thread.sleep(60 * 1000);
        socket.close();
    }
}
