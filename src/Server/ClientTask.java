package Server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;

public class ClientTask implements Runnable {
    private Socket clientSocket;
    private BufferedReader ricevoDalClient;
    private DataOutputStream invioAlClient;

    private String username = "";
    private  String avversario;

    public ClientTask(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.ricevoDalClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.invioAlClient = new DataOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //Task che gestisce le richeste da parte del client che gli viene assegnato. Legge il comando ricevuto, legge
    //eventualmente i vari input in base al comando ricevuto, invocando un metodo per eseguire la richiesta e inoltra il
    //risultato al client
    @Override
    public void run() {
        boolean flag = true;
        while(flag){
            String password;
            int comandoRicevuto;
            try {
                comandoRicevuto = ricevoDalClient.read();
                //----------------------------------------         LOGIN         ---------------------------------------
                if (comandoRicevuto == 0) {
                    this.username = ricevoDalClient.readLine();
                    password = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LOGIN ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Password : " + password);
                    System.out.println("-------------------------------------");

                    int risultato = ServerMain.login(username, password);

                    //Invio il messaggio di risposta al client
                    invioAlClient.write(risultato);
                    if (risultato == 0)
                        System.out.println("Utente " + username + " CONNESSO");
                }
                //--------------------------------------------     LOGOUT   --------------------------------------------
                if (comandoRicevuto == 1){
                    this.username = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LOGOUT ricevuto  ----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-------------------------------------");

                    boolean tmp = ServerMain.logout(username);
                    int risultato = -1;

                    if(tmp){
                        System.out.println("Utente " + username + " DISCONNESSO");
                        risultato = 0;
                        this.username = "";
                    }
                    invioAlClient.write(risultato);
                }
                //---------------------------------------    AGGIUNGI AMICO      ---------------------------------------
                if(comandoRicevuto == 2){
                    username = ricevoDalClient.readLine();
                    String invitato = ricevoDalClient.readLine();

                    System.out.println("-----   Comando AGGIUNGI AMICO ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Username aggiunto : " + invitato);
                    System.out.println("----------------------------------------------");

                    int risultato = ServerMain.aggiungiAmico(username, invitato);
                    System.out.println("Ricevuto risultato : " + risultato);
                    invioAlClient.write(risultato);
                }
                //------------------------------------------   LISTA AMICI    ------------------------------------------
                if(comandoRicevuto == 3) {
                    username = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LISTA AMICI ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-------------------------------------------");
                    ArrayList<String> lista = ServerMain.listaAmici(username);

                    if (lista.isEmpty()) {
                        invioAlClient.writeBytes("vuoto" + '\n');
                    }else{
                        JSONArray arrayJ = new JSONArray();
                        arrayJ.addAll(lista);
                        JSONObject objectJ = new JSONObject();
                        objectJ.put(username, arrayJ);


                        String listaDaInviare = objectJ.toJSONString();
                        invioAlClient.writeBytes(listaDaInviare + '\n');
                    }
                }
                //--------------------------------------        INVIA SFIDA       --------------------------------------
                if(comandoRicevuto == 4){
                    //Ricevo il nome del giocatore da sfidare
                    avversario = ricevoDalClient.readLine();

                    System.out.println("-----   Comando SFIDA ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Utente sfidato : " + avversario);
                    System.out.println("-------------------------------------");
                    //Controllo che la sfida possa avvenire
                    int risultato = ServerMain.controlloPreSfida(username, avversario);
                    //Invio al client la risposta del controllo pre sfida
                    invioAlClient.write(risultato);
                    if(risultato == 0){
                        //A questo punto preparo e invio il datagramma all'utente da sfidare contenete il proprio nome
                        //(nome dello sfidante) cosi da poter accettare o meno la sfida
                        DatagramSocket clientSocket = new DatagramSocket();
                        byte[] buffer;
                        buffer = username.getBytes();
                        InetAddress address = InetAddress.getByName("127.0.0.1");
                        DatagramPacket sendPacket = new DatagramPacket(buffer, buffer.length, address, generaPorta(avversario));
                        clientSocket.send(sendPacket);
                        //Attendo il datagram di risposta e setto un timeout cosi che la sfida venga automaticamente rifiutata
                        byte[] buffer2 = new byte[1024];
                        String risposta;
                        sendPacket = new DatagramPacket(buffer2, buffer2.length);

                        clientSocket.setSoTimeout(ServerConfig.T1);
                        try{
                            clientSocket.receive(sendPacket);
                            risposta = new String(sendPacket.getData(), 0, sendPacket.getLength());
                            System.out.println(risposta + " [ricevuto come risposta]");
                        } catch (SocketTimeoutException e) {
                            risposta = "rifiuto";
                            System.out.println(risposta + " [generata da eccezione lanciata e catturata]");
                        }
                        if(risposta.equals("accetto")){
                            //Sfida accettata quindi faccio scegliere le N parole al server e ottengo anche le traduzioni
                            ServerMain.setParolePartita(ServerConfig.N, username.hashCode());
                            ServerMain.setParoleTradotte(ServerConfig.N, username.hashCode());
                        }
                        invioAlClient.writeBytes(risposta + '\n');
                    }
                }
                //------------------------------------        SFIDA AVVIATA       -------------------------------------
                if (comandoRicevuto == 5) {
                    String tmp = ricevoDalClient.readLine();
                    invioAlClient.write(ServerConfig.N);

                    ServerMain.resetPunteggioPartita(username);
                    ServerMain.setInSfida(username);

                    Thread.sleep(1000);
                    ArrayList<String> listaParole = ServerMain.getListeParole(tmp.hashCode());
                    ArrayList<String> paroleTradotte = ServerMain.getParoleTradotte(tmp.hashCode());

                    //a questo punto inizia la sfida, mando una parola alla volta al client
                    GameThreadServer gameThread = new GameThreadServer(invioAlClient, ricevoDalClient, listaParole, paroleTradotte, username, Thread.currentThread());
                    gameThread.start();
                    try{
                        Thread.sleep(ServerConfig.T2);
                        System.out.print("------- Tempo scaduto, sfida interrotta -------");
                        gameThread.interrupt();
                    } catch (InterruptedException e) {
                        System.out.println("------- Sfida Terminata in tempo -------");
                    }
                    ServerMain.resetInSfida(username);
                    String tmp2;
                    if(tmp.equals(username))tmp2 = avversario;
                    else tmp2 = tmp;
                    String vincitore = ServerMain.getVincitore(username, tmp2);
                    System.out.println("------- Vincitore : " + vincitore);
                    if(username.equals(vincitore))ServerMain.addPunteggioBonus(vincitore);
                    invioAlClient.writeBytes(vincitore + '\n');

                    if(username.equals(vincitore))ServerMain.updatePunteggi(username, tmp2);
                }
                //----------------------------------------      PUNTEGGIO       ----------------------------------------
                if(comandoRicevuto == 6){
                    int punteggio = ServerMain.getPunteggio(username);
                    System.out.println("-----   Comando PUNTEGGIO ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Punteggio : " + punteggio);
                    System.out.println("-----------------------------------------");

                    invioAlClient.write(punteggio);
                }
                //----------------------------------------      CLASSIFICA       ---------------------------------------
                if(comandoRicevuto == 7){
                    System.out.println("-----   Comando CLASSIFICA ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("------------------------------------------");
                    String classifica = ServerMain.getClassifica(username);
                    invioAlClient.writeBytes(classifica + '\n');
                }
                //----------------------------------------   CHIUSURA FINESTRA   ----------------------------------------
                if(comandoRicevuto == 9){
                    this.username = ricevoDalClient.readLine();
                    System.out.println("-----   CHIUSURA FINESTRA ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----------------------------------------");
                    ServerMain.chiusuraFinestra(username);

                    //Chiudo tutto dato che ho terminato
                    invioAlClient.close();
                    ricevoDalClient.close();
                    clientSocket.close();
                    System.out.println("-----  Utente : " + username + " DISCONNESSO");
                    flag = false;
                }
            }catch (Exception e) {
                try {
                    //Nel caso venisse lanciata una qualunque eccezione chiudo e termino il ciclo
                    invioAlClient.close();
                    ricevoDalClient.close();
                    clientSocket.close();
                    flag = false;
                }catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void avviaSfida(ArrayList<String> listaParole, ArrayList<String> paroleTradotte) throws IOException {
        int i = 0;
        int punteggio = 0;
        String rispostaRicevuta;
        while( i < listaParole.size()){
            int k = i + 1;
            invioAlClient.writeBytes(listaParole.get(i) + '\n');
            System.out.println("Giocatore [ " + username + " ] --- Parola[ " + k + " ] = " + listaParole.get(i));
            rispostaRicevuta = ricevoDalClient.readLine();
            System.out.println("Giocatore [ " + username + " ] --- ParolaTradotta[ " + k + " ] = " + rispostaRicevuta);
            System.out.println("Soluzione : " + paroleTradotte.get(i));
            if(rispostaRicevuta.equals(paroleTradotte.get(i)))
                punteggio += ServerConfig.X;
            else punteggio -= ServerConfig.Y;
            i++;
        }
        try {
            ServerMain.setPunteggio(username, punteggio);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Giocatore [ " + username + " ] --- Punteggio Finale Ottenuto : " + punteggio);
        System.out.println("--------------------------------------");
    }

    //Con questo metodo posso generare la stessa porta sia dal lato client che dal lato server
    private int generaPorta(String username) {
        int port = username.hashCode() % 65535;
        if(port < 0) port = -port % 65535;
        if(port < 1024) port += 1024;// porte Well-Known
        return port + 300;
    }
 }
