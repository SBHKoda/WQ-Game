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
        String risposta = null;
        int punteggio = 0;
        int i = 0;
        boolean flag = true;
        while(!isInterrupted() && i < listaParole.size()){
            try {
                invioAlClient.writeBytes(listaParole.get(i) +'\n');
                //se ricevo -1 allora il tempo per il client e` finito quindi non mi blocco in attesa di una parola
                // tradotta
                risposta = ricevoDalClient.readLine();
                if(!risposta.equals("INTERRUZIONE")){
                    System.out.println("RISPOSTA RICEVUTA DA CLIENT : " + risposta + " la soluzione e` : " + paroleTradotte.get(i));
                    if(risposta.equals(paroleTradotte.get(i)))punteggio += ServerConfig.X;
                    else punteggio -= ServerConfig.Y;
                    System.out.println("Punteggio : " + punteggio);
                    i++;
                }
                else break;
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

       if(!isInterrupted()) main.interrupt();
    }
}
