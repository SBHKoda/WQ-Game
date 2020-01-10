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
    private InetAddress inetAddress;
    private int port;

    public Game(JTextArea insertArea,DatagramSocket datagramSocket, InetAddress inetAddress, int port) throws SocketException {
        this.inetAddress = inetAddress;
        this.socket = new DatagramSocket();
        socket.connect(inetAddress, port);
        this.insertArea = insertArea;
        this.port = port;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket packet;
        System.out.println("GAME READY");
        System.out.println("Porta in cui ricevo : " + port);
        //System.out.println("indirizzo in cui ricevo : " + socket.getLocalAddress());
        System.out.println("indirizzo in cui ricevo forse : " + socket.getInetAddress());
        try {
            //socket.connect(inetAddress, ClientConfig.GAME_PORT);
            while(isRunning) {
                packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("Pacchetto ricevuto ");

                String messaggio = new String(packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println("Ricevuto messaggio : " + messaggio);
                insertArea.append(messaggio + '\n');
            }
        } catch (IOException e1) { e1.printStackTrace(); }





        /*DatagramPacket packet;
        System.out.println("Game attivo ");
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
        isRunning = false;*/
    }
}
