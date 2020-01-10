package Client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;


public class Game extends Thread {

    private boolean isRunning = true;

    private int port;

    public Game(int port) {
        this.port = port;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        try(DatagramSocket serverSocket = new DatagramSocket(port)){

            while(isRunning){
                serverSocket.receive(receivedPacket);
                String messaggio = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), StandardCharsets.US_ASCII);
                System.out.println("Messaggio ricevuto : " + messaggio);

                JPanel panel = new JPanel(new BorderLayout(5, 5));

                JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
                label.add(new JLabel("SFIDA ricevuta da : " + messaggio + " accetti? ", SwingConstants.RIGHT));
                panel.add(label, BorderLayout.WEST);

                JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));

                panel.add(controls, BorderLayout.CENTER);
                //Controllo se viene premuto OK o CANCEL
                int input = JOptionPane.showConfirmDialog(null, panel, "SFIDA", JOptionPane.OK_CANCEL_OPTION);
                int port = receivedPacket.getPort();
                InetAddress address = receivedPacket.getAddress();

                DatagramPacket sendPacket;

                if(input == 0){//Caso OK
                    //Invia un pacchetto al server che accetta la sfida
                    String risposta = "accetto";
                    buffer = risposta.getBytes();
                    sendPacket = new DatagramPacket(buffer, buffer.length, address, port);
                    serverSocket.send(sendPacket);
                }
                else{
                    //invia un pacchetto che rifiuta la sfida
                    String risposta = "rifiuto";
                    buffer = risposta.getBytes();
                    sendPacket = new DatagramPacket(buffer, buffer.length, address, port);
                    serverSocket.send(sendPacket);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRunning() {
        this.isRunning = false;
    }
}
