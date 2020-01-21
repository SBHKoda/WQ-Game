package Client;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;


public class ReceiverUDP extends Thread {

    private boolean isRunning = true;
    private int port;
    private ClientUI clientUI;

    public ReceiverUDP(int port, ClientUI clientUI) {
        this.port = port;
        this.clientUI = clientUI;
    }

    public void run(){
        byte[] buffer = new byte[1024];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
        try(DatagramSocket serverSocket = new DatagramSocket(port)){

            while(isRunning){
                serverSocket.receive(receivedPacket);
                String sfidante = new String(receivedPacket.getData(), 0, receivedPacket.getLength(), StandardCharsets.US_ASCII);

                int receivedPacketPort = receivedPacket.getPort();
                InetAddress receivedPacketAddress = receivedPacket.getAddress();

                JOptionPane msg = new JOptionPane("SFIDA ricevuta da : " + sfidante + " accetti? ", JOptionPane.INFORMATION_MESSAGE, JOptionPane.YES_NO_OPTION);
                JDialog dlg = msg.createDialog(clientUI,"SFIDA");

                //Imposto il valore iniziale su NO in modo che quando scatta il timer la sfida viene automaticamente
                // rifiutata
                msg.setInitialSelectionValue(JOptionPane.NO_OPTION);
                dlg.setAlwaysOnTop(true);
                dlg.setDefaultCloseOperation(JOptionPane.NO_OPTION);
                dlg.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentShown(ComponentEvent e) {
                        super.componentShown(e);
                        final Timer t = new Timer(ClientConfig.TIMEOUT, e1 -> dlg.setVisible(false));
                        t.start();
                    }
                });
                dlg.setVisible(true);
                Object input = msg.getValue();
                if(input.equals(JOptionPane.YES_OPTION)){//Caso OK
                    //Invia un pacchetto al server che accetta la sfida
                    String risposta = "accetto";
                    buffer = risposta.getBytes();
                    receivedPacket = new DatagramPacket(buffer, buffer.length, receivedPacketAddress, receivedPacketPort);
                    serverSocket.send(receivedPacket);

                    ClientUI.sfidaAccettata(sfidante);
                }
                else{
                    System.out.println("timeout scattato e catturato");
                    //invia un pacchetto che rifiuta la sfida
                    String risposta = "rifiuto";
                    buffer = risposta.getBytes();
                    receivedPacket = new DatagramPacket(buffer, buffer.length, receivedPacketAddress, receivedPacketPort);
                    serverSocket.send(receivedPacket);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public void stopRunning() {
        this.isRunning = false;
    }
}
