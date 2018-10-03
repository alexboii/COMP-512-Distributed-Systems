package Tcp;

import RM.IResourceManager;
import RM.ResourceManager;

import java.io.BufferedOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by alex on 10/2/18.
 */
public class ProcessRequestRunnable implements Runnable {
    Socket client;
    IResourceManager handler;


    public ProcessRequestRunnable(Socket client, IResourceManager handler){
        this.client = client;
        this.handler = handler;
    }

    @Override
    public void run() {

    }
}
