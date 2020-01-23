package Server;

import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GameThread extends Thread {
    private DataOutputStream invioAlClient;
    private BufferedReader ricevoDalClient;
    private ArrayList<String> listaParole;
    private ArrayList<String> paroleTradotte;
    private String username;
    private Thread main;

    //Classe che implementa Thread e si occupa di inviare al client le parole da tradurre e di ricevere le traduzioni
    public GameThread(DataOutputStream invioAlClient, BufferedReader ricevoDalClient, ArrayList<String> listaParole, ArrayList<String> paroleTradotte, String username, Thread main){
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
        boolean interruzione = false;
        int i = 0;
        while(i < listaParole.size()){
            try {
                invioAlClient.writeBytes(listaParole.get(i) +'\n');
                //Il thread rimane in attesa di ricevere una parola tradotta, se invece ricevo "INTERRUZIONE" allora
                // il timer del client e` scaduto quindi interrompo il ciclo
                risposta = ricevoDalClient.readLine();
                if(!risposta.equals("INTERRUZIONE")){
                    System.out.println("RISPOSTA RICEVUTA DA CLIENT :: " + risposta + " :: la soluzione e` --> " + paroleTradotte.get(i));
                    if(risposta.equals(paroleTradotte.get(i)))punteggio += ServerConfig.X;
                    else punteggio -= ServerConfig.Y;
                    System.out.println("Punteggio : " + punteggio);
                    i++;
                }
                else {
                    interruzione = true;
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("----------------------------------------");
        try {
            //Terminato il ciclo aggiorno il punteggio dell'utente
            ServerMain.setPunteggio(username, punteggio);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        //Se non ho ricevuto "INTERRUZIONE" allora lancio una interruzione sul thread principale in modo da svegliarlo
        // in anticipo
       if(!interruzione) main.interrupt();
    }
}
