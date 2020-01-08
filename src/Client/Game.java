package Client;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Game extends Thread {

    private boolean isRunning = false;

    private JTextArea insertArea;
    private MulticastSocket socket;
    private InetAddress inetAddress;

    public Game(JTextArea insertArea, MulticastSocket socket, InetAddress inetAddress){
        this.inetAddress = inetAddress;
        this.socket = socket;
        this.insertArea = insertArea;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket packet;
        try {
            socket.joinGroup(inetAddress);
            while(isRunning) {
                packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String messaggio = new String(packet.getData(), packet.getOffset(), packet.getLength());
                insertArea.append(messaggio + '\n');
            }
        } catch (IOException e1) { e1.printStackTrace(); }
    }
    public void closeChat() {
        isRunning = false;
    }
}
