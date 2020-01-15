package Client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;


public class ReceiverUDP extends Thread {

    private boolean isRunning = true;

    private int port;

    public ReceiverUDP(int port) {
        this.port = port;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        try(DatagramSocket serverSocket = new DatagramSocket(port)){

            while(isRunning){
                serverSocket.receive(receivedPacket);
                String sfidante = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), StandardCharsets.US_ASCII);
                System.out.println("Messaggio ricevuto - : " + sfidante);

                int receivedPacketPort = receivedPacket.getPort();
                System.out.println("porta ricevuta : " + receivedPacketPort);
                InetAddress receivedPacketAddress = receivedPacket.getAddress();
                System.out.println("indirizzo ricevuto : " + receivedPacketAddress);

                JPanel panel = new JPanel(new BorderLayout(5, 5));

                JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
                label.add(new JLabel("SFIDA ricevuta da : " + sfidante + " accetti? ", SwingConstants.RIGHT));
                panel.add(label, BorderLayout.WEST);

                JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));

                panel.add(controls, BorderLayout.CENTER);
                //Controllo se viene premuto OK o CANCEL
                int input = JOptionPane.showConfirmDialog(null, panel, "SFIDA", JOptionPane.OK_CANCEL_OPTION);

                if(input == 0){//Caso OK
                    System.out.println(" Premuto ok");
                    //Invia un pacchetto al server che accetta la sfida
                    String risposta = "accetto";
                    System.out.println(risposta);
                    buffer = risposta.getBytes();
                    receivedPacket = new DatagramPacket(buffer, buffer.length, receivedPacketAddress, receivedPacketPort);
                    serverSocket.send(receivedPacket);

                    ClientUI.avviaSfida(sfidante);
                }
                else{

                    //invia un pacchetto che rifiuta la sfida
                    String risposta = "rifiuto";
                    System.out.println(risposta);
                    buffer = risposta.getBytes();
                    receivedPacket = new DatagramPacket(buffer, buffer.length, receivedPacketAddress, receivedPacketPort);
                    serverSocket.send(receivedPacket);
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
