package com.github.freeacs.stun;

import java.net.*;
import java.net.*;
import java.nio.ByteBuffer;

public class StunTestClient {
    public static void main(String[] args) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            // Cabeçalho STUN Binding Request (20 bytes)
            ByteBuffer buffer = ByteBuffer.allocate(20);
            buffer.putShort((short) 0x0001); // Message Type: Binding Request
            buffer.putShort((short) 0x0000); // Message Length: 0 attributes
            buffer.putInt(0x2112A442);       // Magic Cookie (RFC 5389)
            buffer.putLong(System.currentTimeMillis()); // Transaction ID (part 1)
            buffer.putInt(0x12345678);       // Transaction ID (part 2)

            byte[] data = buffer.array();
            InetAddress address = InetAddress.getByName("127.0.0.1");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, 3478);

            socket.send(packet);
            System.out.println("STUN Packet sent from port: " + socket.getLocalPort());

            // Aumenta o timeout para 5 segundos para dar tempo ao servidor
            socket.setSoTimeout(5000);

            byte[] responseBuf = new byte[1024];
            DatagramPacket response = new DatagramPacket(responseBuf, responseBuf.length);

            try {
                socket.receive(response);
                System.out.println("Success! Received response from STUN server (" + response.getLength() + " bytes)");
            } catch (SocketTimeoutException e) {
                System.err.println("Timeout: Server received the packet but didn't send a valid STUN response back.");
                System.err.println("Check if 'activeStunClients' in Actuator has an entry for port: " + socket.getLocalPort());
            }
        }
    }
}
