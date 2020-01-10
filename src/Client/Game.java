package Client;

import javax.swing.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


public class Game extends Thread {

    private boolean isRunning = true;

    private JTextArea insertArea;
    private DatagramSocket serverSock;
    private InetAddress inetAddress;
    private int port;

    public Game(JTextArea insertArea,DatagramSocket datagramSocket, InetAddress inetAddress, int port) throws SocketException {
        this.inetAddress = inetAddress;
        this.insertArea = insertArea;
        this.port = port;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        try(DatagramSocket serverSocket = new DatagramSocket(port)){
            System.out.println("Game active");

            while(isRunning){
                serverSocket.receive(receivedPacket);
                String messaggio = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), StandardCharsets.US_ASCII);
                System.out.println("Messaggio ricevuto : " + messaggio);
                insertArea.append(messaggio + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
