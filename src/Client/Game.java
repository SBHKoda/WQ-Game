package Client;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class Game extends Thread {

    private boolean isRunning = false;

    private JTextArea insertArea;
    private DatagramSocket socket;
    private byte[] buf = new byte[1024];
    private String address;

    public Game(JTextArea insertArea, String address, int port) throws SocketException {
        this.address = address;
        this.socket = new DatagramSocket(port);
        this.insertArea = insertArea;
    }

    public void run(){
        DatagramPacket packet;
        try {
            while(isRunning) {
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                //TODO: il pacchetto e` stato ricevuto ora va creata la finestra popup per accettare o rifiutare

                String messaggio = new String(packet.getData(), packet.getOffset(), packet.getLength());

                System.out.println("Pacchetto ricevuto : " + messaggio);

                insertArea.append(messaggio + '\n');
            }
            socket.close();
        } catch (IOException e1) { e1.printStackTrace(); }
    }
    public void closeChat() {
        isRunning = false;
    }
}
