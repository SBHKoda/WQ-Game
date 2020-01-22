package Server;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GameThreadServer extends Thread {
    private DataOutputStream invioAlClient;
    private BufferedReader ricevoDalClient;
    private ArrayList<String> listaParole;
    private ArrayList<String> paroleTradotte;
    private String username;
    private Thread main;


    public GameThreadServer(DataOutputStream invioAlClient, BufferedReader ricevoDalClient, ArrayList<String> listaParole, ArrayList<String> paroleTradotte, String username, Thread main){
        this.invioAlClient = invioAlClient;
        this.ricevoDalClient = ricevoDalClient;
        this.listaParole = listaParole;
        this.paroleTradotte = paroleTradotte;
        this.username = username;
        this.main = main;
    }

    @Override
    public void run(){
        String risposta;
        int punteggio = 0;
        int i = 0;
        while(!isInterrupted() && i < listaParole.size()){
            try {
                invioAlClient.writeBytes(listaParole.get(i) +'\n');
                risposta = ricevoDalClient.readLine();
                System.out.println("RISPOSTA RICEVUTA DA CLIENT : " + risposta + " la soluzione e` : " + paroleTradotte.get(i));
                if(risposta.equals(paroleTradotte.get(i)))punteggio += ServerConfig.X;
                else punteggio -= ServerConfig.Y;
                System.out.println("Punteggio : " + punteggio);
                i++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("--------------------------------------");
        try {
            ServerMain.setPunteggio(username, punteggio);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Giocatore [ " + username + " ] --- Punteggio Finale Ottenuto : " + punteggio);
        System.out.println("--------------------------------------");
       if(!isInterrupted()) main.interrupt();
    }
}