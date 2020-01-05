package Server;

import Client.ClientUI;

import java.io.File;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    //Lista di tutti gli utenti registrati al gioco
    private static ConcurrentHashMap<String, User> userList;
    //ServerSocket per iniziare le connessioni con il server
    private static ServerSocket welcomeSocket;
    //ThreadPool
    private static ExecutorService executorService;
    //File json
    private static File fileUtenti;

    //--------------------------------------------         MAIN            --------------------------------------------
    public static void main(String args[]){
        initServer();
        initServerCycle();
        //TEST
    }

    private static void initServer() {
        //Inizializzazione delle strutture dati necessarie per il corretto funzionamento del server
        userList = new ConcurrentHashMap<>();
        //Creo una directory per per salvare la lista degli utenti registrati a WQ
        File directory = new File("LISTA_UTENTI/");
        if(!directory.exists())directory.mkdir();
        try {
            fileUtenti = new File("LISTA_UTENTI/ListaUtenti.json");
            if(!fileUtenti.exists())fileUtenti.createNewFile();
            else{
                
            }
        } catch (IOException e) {
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
            ServerImplementationRMI server = new ServerImplementationRMI(userList);
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
        System.out.println("------------------------          SERVER PRONTO        ------------------------");
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
}
