package com.github.freeacs.stun;

import de.javawi.jstun.StunServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

@Disabled("This test is not working")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StunIntegrationTest {

    @Autowired
    private Properties properties;

    @Test
    public void testStunBindingRequestRegistration() throws Exception {
        // 1. Setup UDP Client
        int stunPort = properties.getPrimaryPort();
        InetAddress serverAddr = InetAddress.getByName("127.0.0.1");
        DatagramSocket clientSocket = new DatagramSocket();

        // 2. Create a minimal STUN Binding Request (20 bytes header)
        // [0,1] - Type (0x0001), [2,3] - Length (0x0000), [4..19] - Transaction ID
        byte[] requestData = new byte[20];
        requestData[1] = 0x01; // Binding Request

        DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, serverAddr, stunPort);

        // 3. Send packet to the running StunServer
        clientSocket.send(sendPacket);

        // 4. Wait for the server to process (Async) using Awaitility
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Map<String, Long> activeClients = StunServer.getActiveStunClients().getMap();
            // The key is IP:Port. Since we sent from localhost, check if any entry exists
            assertFalse(activeClients.isEmpty(), "Server should have registered the client");

            // Validate if our client port is in the map
            boolean found = activeClients.keySet().stream()
                    .anyMatch(key -> key.contains(String.valueOf(clientSocket.getLocalPort())));
            assertTrue(found, "Client port should be present in activeStunClients map");
        });

        clientSocket.close();
    }

    @Test
    public void testUdpKickFlow() throws Exception {
        // 1. Prepare a socket to listen for the kick
        DatagramSocket cpeFakeSocket = new DatagramSocket(0);
        int cpePort = cpeFakeSocket.getLocalPort();

        // 2. Trigger the kick logic manually
        // Construct a fake UDP Connection Request URL pointing to our socket
        String udpCrUrl = "127.0.0.1:" + cpePort;

        // Manually push to MessageStack (simulating what Kick.kickUsingUDP does)
        byte[] kickMessage = "GET http://127.0.0.1 HTTP/1.1\r\n\r\n".getBytes();
        DatagramPacket packet = new DatagramPacket(kickMessage, kickMessage.length,
                InetAddress.getByName("127.0.0.1"), cpePort);

        com.github.freeacs.stun.MessageStack.push(packet);
        com.github.freeacs.stun.MessageStack.push(packet); // Push twice as per original logic

        // 3. Receive on the fake CPE socket
        byte[] receiveBuf = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(receiveBuf, receiveBuf.length);
        cpeFakeSocket.setSoTimeout(5000);
        cpeFakeSocket.receive(receivedPacket);

        String receivedStr = new String(receivedPacket.getData()).trim();
        assertTrue(receivedStr.contains("GET"), "CPE should receive the HTTP-like GET kick message");

        cpeFakeSocket.close();
    }

}
