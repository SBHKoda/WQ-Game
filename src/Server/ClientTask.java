package Server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientTask implements Runnable {
    private Socket clientSocket;
    private BufferedReader ricevoDalClient;
    private DataOutputStream invioAlClient;
    private SocketChannel clientSocketChannel = null;
    private ServerSocketChannel serverSocketChannel = null;

    private String username = "";
    private String docName = "";

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
                //--------------------------------------    LOGIN   --------------------------------------
                if (comandoRicevuto == 0) {
                    this.username = ricevoDalClient.readLine();
                    password = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LOGIN ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Password : " + password);
                    int risultato = ServerMain.login(username, password);
                    System.out.println("Ricevuto risultato : " + risultato);
                    //Invio il messaggio di risposta al client
                    invioAlClient.write(risultato);

                    if (risultato == 0) {
                        ///Ottengo gli inviti ricevuti mentre l'utente era offline e li invio al client
                        System.out.println("Utente " + username + " CONNESSO");
                        //Vengono creati i channel in caso non esistessero
                        if (serverSocketChannel == null)
                            creaServerSocketChannel();
                        if (clientSocketChannel == null)
                            clientSocketChannel = accettaServerSocketChannel();
                    }
                }
                //--------------------------------------    LOGOUT   --------------------------------------
                if (comandoRicevuto == 1){
                    this.username = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LOGOUT ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    boolean tmp = ServerMain.logout(username);
                    int risultato = -1;
                    System.out.println("Ricevuto risultato : " + tmp);
                    if(tmp){
                        System.out.println("Utente " + username + " DISCONNESSO");
                        risultato = 0;
                        this.username = "";
                    }
                    invioAlClient.write(risultato);
                    //Chiudo tutto dato che ho terminato
                    invioAlClient.close();
                    ricevoDalClient.close();
                    serverSocketChannel.close();
                    clientSocket.close();
                    clientSocketChannel.close();
                    flag = false;
                }
                //----------------------------------------  LISTA AMICI    ----------------------------------------
                if(comandoRicevuto == 5) {
                    username = ricevoDalClient.readLine();
                    System.out.println("-----   Comando LISTA AMICI ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    ArrayList<String> listaRisultato = ServerMain.listaAmici(username);



                    invioAlClient.write(listaRisultato.size());
                    if (listaRisultato.size() == 0) {
                        invioAlClient.writeBytes("Ancora nessun amico");
                    } else {
                        for (String risultato : listaRisultato)
                            invioAlClient.writeBytes(risultato + '\n');
                    }
                }
                //----------------------------------------  AGGIUNGI AMICO   ----------------------------------------
                if(comandoRicevuto == 8){
                    username = ricevoDalClient.readLine();
                    String invitato = ricevoDalClient.readLine();

                    System.out.println("-----   Comando AGGIUNGI AMICO ricevuto  -----");
                    System.out.println("-----   Username : " + username);
                    System.out.println("-----   Username aggiunto : " + invitato);

                    int risultato = ServerMain.aggiungiAmico(username, invitato);
                    System.out.println("Ricevuto risultato : " + risultato);
                    invioAlClient.write(risultato);
                }

                //----------------------------------------  CHIUSURA FORZATA   ----------------------------------------
                if(comandoRicevuto == 9){
                    this.username = ricevoDalClient.readLine();
                    System.out.println("-----   Chiusura forzata ricevuta  -----");
                    System.out.println("-----   Username : " + username);
                    ServerMain.chiusuraForzata(username);

                    //Chiudo tutto dato che ho terminato
                    invioAlClient.close();
                    ricevoDalClient.close();
                    serverSocketChannel.close();
                    clientSocket.close();
                    clientSocketChannel.close();
                    System.out.println("-----  Utente : " + username + " DISCONNESSO");
                    flag = false;
                }
            }catch (Exception e) {
                try {
                    //Nel caso venisse lanciata una qualunque eccezione chiudo tutto e nel caso rilascio la lock della
                    // sezione, e termino il ciclo
                    clientSocket.close();
                    if(clientSocketChannel != null)
                        clientSocketChannel.close();
                    if(serverSocketChannel != null)
                        serverSocketChannel.close();
                    flag = false;
                }catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    private SocketChannel accettaServerSocketChannel() throws IOException {
        SocketChannel clientSocketChannel;
        clientSocketChannel = serverSocketChannel.accept();
        return clientSocketChannel;
    }

    private void creaServerSocketChannel() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        int port = username.hashCode() % 65535;
        if(port < 0)
            port = -port % 65535;
        if(port < 1024)
            port += 1024;
        serverSocketChannel.socket().bind(new InetSocketAddress(port));
    }
}
