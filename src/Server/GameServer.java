package Server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GameServer extends Thread {
    private DataOutputStream invioAlClient;
    private BufferedReader ricevoDalClient;
    private ArrayList<String> listaParole;
    private ArrayList<String> paroleTradotte;
    private String username;
    private Thread main;

    public GameServer(DataOutputStream invioAlClient, BufferedReader ricevoDalClient, ArrayList<String> listaParole, ArrayList<String> paroleTradotte, String username, Thread main){
        this.invioAlClient = invioAlClient;
        this.ricevoDalClient = ricevoDalClient;
        this.listaParole = listaParole;
        this.paroleTradotte = paroleTradotte;
        this.username = username;
        this.main = main;
    }

    @Override
    public void run() {
        String rispostaRicevuta;
        int punteggio = 0;
        int i = 0;
        while(!Thread.currentThread().isInterrupted() && i < listaParole.size()){
            try {
                int k = i + 1;
                invioAlClient.writeBytes(listaParole.get(i) + '\n');
                System.out.println("Giocatore [ " + username + " ] --- Parola[ " + k + " ] = " + listaParole.get(i));
                rispostaRicevuta = ricevoDalClient.readLine();
                System.out.println("Giocatore [ " + username + " ] --- ParolaTradotta[ " + k + " ] = " + rispostaRicevuta);
                System.out.println("Soluzione : " + paroleTradotte.get(i));
                if(rispostaRicevuta.equals(paroleTradotte.get(i)))punteggio += 2;
                else punteggio--;
                i++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("--------------------------------------");
        ServerMain.setPunteggio(username, punteggio);
        System.out.println("Giocatore [ " + username + " ] --- Punteggio Finale Ottenuto : " + punteggio);
        System.out.println("--------------------------------------");
        main.interrupt();
    }

}
