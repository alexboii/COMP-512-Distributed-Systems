package Tcp;

import RM.IResourceManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
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
        BufferedReader reader = null;
        OutputStreamWriter writer = null;
        try {
            reader = new BufferedReader(new InputStreamReader(this.client.getInputStream(), "UTF-8"));
            writer = new OutputStreamWriter(client.getOutputStream(), "UTF-8");

            String line = null;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);
                handler.handleRequest(new JSONObject(line), writer);
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
