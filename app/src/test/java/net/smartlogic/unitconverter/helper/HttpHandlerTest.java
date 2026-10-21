package net.smartlogic.unitconverter.helper;

import android.app.Application;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class HttpHandlerTest {
    @Test public void handlesHttpErrorsWithoutReturningTheirBodiesAsRates() throws Exception {
        HttpHandler handler = new HttpHandler();
        for (int status : new int[]{200, 401, 429, 500}) {
            try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))) {
                server.setSoTimeout(10000);
                AtomicReference<Exception> error = new AtomicReference<>();
                Thread responder = new Thread(() -> {
                    try (var socket = server.accept()) {
                        socket.setSoTimeout(10000);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = reader.readLine()) != null && !line.isEmpty()) { }
                        byte[] body = "{\"message\":\"example\"}".getBytes(StandardCharsets.UTF_8);
                        var output = socket.getOutputStream();
                        output.write(("HTTP/1.1 " + status + " Test\r\nContent-Length: " + body.length
                                + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        output.write(body);
                        output.flush();
                    } catch (Exception failure) { error.set(failure); }
                });
                responder.setDaemon(true);
                responder.start();
                String response = handler.makeServiceCall("http://127.0.0.1:" + server.getLocalPort());
                if (status == 200) assertNotNull(response);
                else assertNull(response);
                responder.join(10000);
                assertFalse(responder.isAlive());
                assertNull(error.get());
            }
        }
        assertNull(handler.makeServiceCall(""));
    }
}
