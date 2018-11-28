package TCP;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Created by alex on 10/2/18.
 */
public interface IServer {
    void start(int port);

    /**
     * Method that each server must implement in order to process requests properly
     * @param request
     * @param writer
     */
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException;
}
