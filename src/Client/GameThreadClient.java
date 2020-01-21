package Client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;

public class GameThreadClient extends Thread{
    private DataOutputStream invioAlServer;
    private BufferedReader ricevoDalServer;
    private ClientUI clientUI;
    private int N;
    private String username;

    public GameThreadClient(DataOutputStream invioAlServer, BufferedReader ricevoDalServer, ClientUI clientUI, int N, String username) {
        this.invioAlServer = invioAlServer;
        this.ricevoDalServer = ricevoDalServer;
        this.clientUI = clientUI;
        this.N = N;
        this.username = username;
    }

    @Override
    public void run() {
        String parolaDT;
        try {
            for(int i = 0; i < N; i++) {
                parolaDT = ricevoDalServer.readLine();
                if (isInterrupted()) {
                    System.out.println("ricevuto fine gioco");
                    break;
                }
                //Creo una finestra con 1 campo di testo per inserire il nome utente da invitare
                JPanel panelSfida = new JPanel(new BorderLayout(8, 8));

                JPanel labelSfida = new JPanel(new GridLayout(0, 1, 1, 1));
                labelSfida.add(new JLabel("Parola da Tradurre : " + parolaDT + " ", SwingConstants.RIGHT));
                panelSfida.add(labelSfida, BorderLayout.WEST);

                JPanel controlsSfida = new JPanel(new GridLayout(0, 1, 1, 1));
                JTextField traduzione = new JTextField();
                traduzione.setColumns(8);
                controlsSfida.add(traduzione);
                panelSfida.add(controlsSfida, BorderLayout.CENTER);
                //Controllo se viene premuto OK o CANCEL
                int input1 = JOptionPane.showConfirmDialog(clientUI, panelSfida, "GIOCATORE: " + username + " Parola [ " + (i + 1) + " ] di [ " + N + " ]", JOptionPane.OK_CANCEL_OPTION);
                if (input1 == 0) invioAlServer.writeBytes(traduzione.getText() + '\n');
                else invioAlServer.writeBytes("" + '\n');
            }
            //Finito il ciclo ricevo dal server il nome del vincitore
            String vincitore = ricevoDalServer.readLine();
            System.out.println("VINCITORE : " + vincitore);
            if(vincitore.equals(username))
                JOptionPane.showMessageDialog(clientUI, "Sei il vincitore della sfida", "Vittoria", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            e.printStackTrace();
        }




    }
}
