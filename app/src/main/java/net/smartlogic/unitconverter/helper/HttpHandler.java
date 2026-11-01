package net.smartlogic.unitconverter.helper;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class HttpHandler {

    private static final String TAG = HttpHandler.class.getSimpleName();

    public HttpHandler() {
    }

    public String makeServiceCall(String reqUrl) {
        String response = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(reqUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int status = conn.getResponseCode();
            if (status < 200 || status >= 300) {
                Log.w(TAG, "Currency HTTP status: " + status);
                InputStream error = conn.getErrorStream();
                if (error != null) error.close();
                return null;
            }
            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            response = convertStreamToString(in);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Malformed currency endpoint");
        } catch (ProtocolException e) {
            Log.e(TAG, "Currency protocol failure");
        } catch (IOException e) {
            Log.e(TAG, "Currency transport failure: " + e.getClass().getSimpleName());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return response;
    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            return null;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                Log.w(TAG, "Currency response stream close failed");
            }
        }
        return sb.toString();
    }
}
