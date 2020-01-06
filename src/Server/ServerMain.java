package Server;

import Client.ClientUI;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerMain {
    //Lista di tutti gli utenti registrati al gioco
    private static ConcurrentHashMap<String, User> userList;
    //Lista delle amicizie, {username, [amico1, amico2, ..., amicoN]}
    private static HashMap<String, ArrayList<String>> friendList;
    //ServerSocket per iniziare le connessioni con il server
    private static ServerSocket welcomeSocket;
    //ThreadPool
    private static ExecutorService executorService;
    //File json
    private static File fileUtenti;
    private static File fileAmicizie;

    //--------------------------------------------         MAIN            --------------------------------------------
    public static void main(String args[]){
        initServer();
        initServerCycle();
    }

    private static void initServer() {
        //Inizializzazione delle strutture dati necessarie per il corretto funzionamento del server
        userList = new ConcurrentHashMap<>();
        //Creo una directory per per salvare la lista degli utenti registrati a WQ in caso non esistesse
        File directory = new File("UTENTI/");
        if(!directory.exists())directory.mkdir();
        try {
            //Creo il file json nel caso non esistesse, altrimenti lo leggo per ricreare il database utenti
            fileUtenti = new File("UTENTI/ListaUtenti.json");
            if(!fileUtenti.exists())fileUtenti.createNewFile();
            else{
                JSONParser parser = new JSONParser();
                Object object = parser.parse(new FileReader("UTENTI/ListaUtenti.json"));
                JSONObject jsonObject1 = (JSONObject) object;
                JSONArray jsonArray = (JSONArray) jsonObject1.get("Lista Utenti");

                Iterator<JSONObject> iterator = jsonArray.iterator();
                while(iterator.hasNext()){
                    String username, password;
                    JSONObject tempObj = iterator.next();
                    username = tempObj.get("username").toString();
                    password = tempObj.get("password").toString();
                    System.out.println("Username : " + username + ", Password : " + password);
                    User user = new User(username, password);

                    userList.put(username, user);
                }
            }
            //Creo il file json nel caso non esistesse, altrimenti lo leggo per ricreare il database amicizie utenti
            fileAmicizie = new File("UTENTI/Amicizie.json");
            if(!fileAmicizie.exists())fileAmicizie.createNewFile();
            else{
                //TODO: ricrea database amicizie 
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        //Creo la ServerSocket per accettare le connessioni
        try {
            welcomeSocket = new ServerSocket(ServerConfig.PORT, 0, InetAddress.getByName(null));
        } catch (IOException e) {
            System.out.println("ERRORE nella creazione della ServerSocket.");
        }

        //Inizzializzo il ThreadPool Executor che gestira` le richieste dei client eseguendo task ClientHandler, ho
        // scelto un CachedThreadPool in modo che si possa gestire autonomamete il numero di thread
        executorService = Executors.newCachedThreadPool();

        //Attivcazione del Servizio RMI per la registrazione
        try{
            //creazione istanza oggetto
            ServerImplementationRMI server = new ServerImplementationRMI(userList, fileUtenti, friendList);
            //esportazione dell'oggetto
            ServerInterfaceRMI stub = (ServerInterfaceRMI) UnicastRemoteObject.exportObject(server, ServerConfig.REG_PORT);
            //creazione di un registry sulla porta in ServerConfig
            Registry registry = LocateRegistry.createRegistry(ServerConfig.REG_PORT);
            //pubblicazione dello stub nel registry
            registry.bind("Server", stub);
        } catch (RemoteException | AlreadyBoundException e) {
            System.out.println("------------------------         ERRORE ATTIVAZIONE RMI       ------------------------");
        }
    }


    private static void initServerCycle() {
        //In questo ciclo infinito aspetto i client, quando ne arrica uno creo una socket per il client e creo e passo
        // al ThreadPool un task ClientTask per gestire tutte le sue richieste
        System.out.println("------------------------              SERVER PRONTO           ------------------------");
        while (true){
            try{
                Socket clientSocket = welcomeSocket.accept();
                ClientTask clientTask = new ClientTask(clientSocket);
                executorService.execute(clientTask);
                System.out.println("------------------------        CLIENT CONNESSO AL SERVER       ------------------------");
            } catch (IOException e) {
                System.out.println("------------------------         QUALCOSA E` ANDATO STORTO       ------------------------");
            }
        }
    }
    //--------------------------------------------         LOGIN            --------------------------------------------
    //Metodo per effettuare il login al server, restituisce:
    // 0 In caso di login corretto
    // 1 Utente non registrato
    // 2 In caso l'utente fosse gia loggato
    public static int login(String username, String password){
        //Caso in cui l'utente e` offline e password corretta
        if(!userList.get(username).checkOnlineStatus() && userList.get(username).checkPassword(password)){
            userList.get(username).setOnline();
            return 0;
        }
        //Caso in cui l'utente cerca di accedere con un username non registrato o errato
        if(!userList.containsKey(username)){
            System.out.println("ERRORE in fase di login, utente non registrato");
            return 1;
        }
        //Caso in cui l'utente cerca di eseguire un doppio login
        if(userList.get(username).checkOnlineStatus())
            System.out.println("ERRORE in fase di login, utente gia loggato");
        return 2;
    }
    //--------------------------------------------         LOGOUT            --------------------------------------------
    //Metodo per effettuare il logout dal server, restituisce:
    // true In caso di logout effettuato correttamente
    // false In caso di errore (es. utente non online)
    public static boolean logout(String username){
        //Per effettuare il logout l'utente deve essere online
        if(userList.get(username).checkOnlineStatus()){
            userList.get(username).setOffline();
            return true;
        }
        System.out.println("ERRORE in fase di logout, l'utente non era online.");
        return false;
    }
    //--------------------------------------         CHIUSURA FINESTRA            --------------------------------------
    // Metodo che viene invocato quando l'utente chiude la GUI con il tasto apposito
    public static void chiusuraForzata(String username) {
        if(username != null){
            if(userList.get(username).checkOnlineStatus()){
                userList.get(username).setOffline();
            }
        }
    }
    //---------------------------------------         AGGIUNGI AMICO            ----------------------------------------
    // 0 se tutto ok
    // 1 nickUser o nickFriend sono null
    // 2 nickUser o nickFriend sono ""
    // 3 nickUser non esiste
    // 4 nickFriend non esiste
    // 5 i due utenti sono gia` amici
    public static synchronized int aggiungiAmico(String nickUser, String nickFriend){
        if(nickUser == null || nickFriend == null){
            System.out.println("ERRORE, i campi nickUser e nickFirend non possono essere null");
            return 1;
        }
        if(nickUser == "" || nickFriend == ""){
            System.out.println("ERRORE, i campi nickUser e nickFriend non possono essere vuoti");
            return 2;
        }
        if(!userList.containsKey(nickUser)){
            System.out.println("ERRORE, nickUser non esiste");
            return 3;
        }
        if(!userList.containsKey(nickFriend)){
            System.out.println("ERRORE, nickFriend non esiste");
            return 4;
        }
        //Caso in cui l'amicizia e` gia stata chiesta
        if(friendList.get(nickUser).contains(nickFriend) || friendList.get(nickFriend).contains(nickUser)){
            System.out.println("ERRORE, nickUser e nickFriend sono gia` amici");
            return 5;
        }
        //A questo punto i due utenti possono diventare amici
        friendList.get(nickUser).add(nickFriend);
        friendList.get(nickFriend).add(nickUser);

        return 0;
    }
}
