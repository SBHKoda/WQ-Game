package Server;

import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    //Lista parole sfida
    private static HashMap<Integer, ArrayList<String>> listeParole;
    private static HashMap<Integer, ArrayList<String>> paroleTradotte;

    //ServerSocket per iniziare le connessioni con il server
    private static ServerSocket welcomeSocket;
    //ThreadPool
    private static ExecutorService executorService;
    //File json
    private static File fileUtenti;
    private static File fileAmicizie;
    private static File fileParole;

    //--------------------------------------------         MAIN            --------------------------------------------
    public static void main(String[] args){
        try {
            createJsonWordFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initServer();
        initServerCycle();


    }

    private static void initServer() {
        //Inizializzazione delle strutture dati necessarie per il corretto funzionamento del server
        userList = new ConcurrentHashMap<>();
        friendList = new HashMap<>();
        listeParole = new HashMap<>();
        paroleTradotte = new HashMap<>();
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
                JSONObject objectJ = (JSONObject) object;
                JSONArray arrayJ = (JSONArray) objectJ.get("Lista Utenti");

                Iterator<JSONObject> iterator = arrayJ.iterator();
                while(iterator.hasNext()){
                    String username, password;
                    JSONObject tempOJ = iterator.next();
                    username = tempOJ.get("username").toString();
                    password = tempOJ.get("password").toString();
                    User user = new User(username, password);

                    userList.put(username, user);
                }
            }
            //Creo il file json nel caso non esistesse, altrimenti lo leggo per ricreare il database amicizie utenti
            fileAmicizie = new File("UTENTI/Amicizie.json");
            if(!fileAmicizie.exists())fileAmicizie.createNewFile();
            else{
                JSONParser parser = new JSONParser();
                Object object = parser.parse(new FileReader("UTENTI/Amicizie.json"));
                JSONObject objectJ = (JSONObject) object;

                Enumeration<String> keys = userList.keys();
                Iterator<String> iterator = keys.asIterator();

                while(iterator.hasNext()){
                    String username = iterator.next();

                    JSONArray arrayJ = (JSONArray) objectJ.get(username);
                    ArrayList<String> arrayList = new ArrayList<>();
                    if(arrayJ != null){
                        arrayList.addAll(arrayJ);
                        //copio le altre vecchie amicizie
                    }
                    friendList.put(username, arrayList);
                }
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        //Creo la ServerSocket per accettare le connessioni
        try {
            InetAddress address = InetAddress.getByName(null);
            System.out.println(address);
            welcomeSocket = new ServerSocket(ServerConfig.PORT, 0, address);
        } catch (IOException e) {
            System.out.println("ERRORE nella creazione della ServerSocket.");
        }

        //Inizzializzo il ThreadPool Executor che gestira` le richieste dei client eseguendo task ClientHandler, ho
        // scelto un CachedThreadPool in modo che si possa gestire autonomamete il numero di thread
        executorService = Executors.newCachedThreadPool();

        //Attivcazione del Servizio RMI per la registrazione
        try{
            //creazione istanza oggetto
            ServerImplementationRMI server = new ServerImplementationRMI(userList, fileUtenti, friendList, fileAmicizie);
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
    // Metodo che viene invocato quando l'utente chiude la finestra
    public static void chiusuraFinestra(String username) {
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
        if(nickUser.equals("") || nickFriend.equals("")){
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
        //Lista delle amicizie, {username, [amico1, amico2, ..., amicoN]}
        if(fileAmicizie.length() == 0){
            //Creo un Json obj e un Json array per ognuno e aggiungo gli username alle rispettive liste degli amioci
            JSONObject objectJ = new JSONObject();
            //sorgente
            JSONArray arrayJ = new JSONArray();
            arrayJ.add(nickFriend);
            objectJ.put(nickUser, arrayJ);
            //destinazione
            JSONArray arrayJ1 = new JSONArray();
            arrayJ1.add(nickUser);
            objectJ.put(nickFriend, arrayJ1);
            try{
                //Scrivo nel file
                FileWriter fileWriter = new FileWriter(fileAmicizie);
                fileWriter.write(objectJ.toJSONString());
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            JSONParser parser = new JSONParser();
            try {
                Object object = parser.parse(new FileReader("UTENTI/Amicizie.json"));
                JSONObject objectJ = (JSONObject) object;
                JSONObject finalObject = new JSONObject();

                Enumeration<String> keys = userList.keys();
                Iterator<String> iterator = keys.asIterator();
                System.out.println(userList.size() + " -- size della lista --");

                while(iterator.hasNext()){
                    System.out.println("---- Inizio nuovo ciclo ----");
                    String username = iterator.next();
                    System.out.println(username + " -- username in aggiornamento --");

                    JSONArray arrayJ = (JSONArray) objectJ.get(username);
                    JSONArray newArrayJ = new JSONArray();
                    if(arrayJ != null){
                        Iterator<String> iterator1 = arrayJ.iterator();
                        //copio le altre vecchie amicizie
                        while(iterator1.hasNext()){
                            System.out.println("-- Ciclo interno iterazione --");
                            String tmp = iterator1.next();
                            System.out.println(tmp + " -- copiato nel nuovo json array --");
                            newArrayJ.add(tmp);
                        }
                    }
                    //Aggiungo le nuove amicizie
                    if(username.equals(nickUser)){
                        newArrayJ.add(nickFriend);
                        System.out.println("Nuova amicizia utente aggiunta");
                    }
                    if(username.equals(nickFriend)){
                        newArrayJ.add(nickUser);
                        System.out.println("Nuova amicizia invitato aggiunta");
                    }
                    System.out.println("---- Fine iterazione ciclo principale ----");
                    finalObject.put(username, newArrayJ);
                }
                System.out.println("---------- Terminato, scrivo nel file ----------");
                //Scrivo nel file
                FileWriter fileWriter = new FileWriter(fileAmicizie);
                fileWriter.write(finalObject.toJSONString());
                fileWriter.close();
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    //------------------------------------------           LISTA AMICI         -----------------------------------------
    //Metodo che restituisce la lista degli amici dell'utente
    public static synchronized ArrayList<String> listaAmici(String nomeUtente){
        return friendList.get(nomeUtente);
    }

    //------------------------------------------           SFIDA         -----------------------------------------
    public static int controlloPreSfida(String nickUser, String nickFriend) throws UnknownHostException {
        if(nickUser == null || nickFriend == null){
            System.out.println("ERRORE, i campi nickUser e nickFirend non possono essere null");
            return 1;
        }
        if(nickUser.equals("") || nickFriend.equals("")){
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
        //Caso in cui i due utenti non sono amici
        if(!friendList.get(nickUser).contains(nickFriend) || !friendList.get(nickFriend).contains(nickUser)){
            System.out.println("ERRORE, nickUser e nickFriend non sono amici, quindi non si possono sfidare");
            return 5;
        }
        if(!userList.get(nickFriend).checkOnlineStatus()){
            System.out.println("ERRORE, Amico OFFLINE, non si puo` sfidare");
            return 6;
        }
        return 0;
    }

    public synchronized static void setParolePartita(int n, int key) {
        JSONParser parser = new JSONParser();

        if(!listeParole.containsKey(key))listeParole.put(key, new ArrayList<>());
        else
            listeParole.get(key).clear(); //Cancello le eventuali parole generate per una vecchia sfida
        try {
            Object object = parser.parse(new FileReader("WORD/word.json"));
            JSONObject objectJ = (JSONObject) object;
            JSONArray arrayJ = (JSONArray) objectJ.get("ListaParole");
            Random random = new Random();
            for (int i = 0; i < n; i++){
                listeParole.get(key).add(arrayJ.get(random.nextInt(104)).toString());
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    public synchronized static void setParoleTradotte(int n, int key) {
        if(!paroleTradotte.containsKey(key))paroleTradotte.put(key, new ArrayList<>());
        else
            paroleTradotte.get(key).clear();////Cancello le eventuali parole tradotte per una vecchia sfida
        for(int i = 0; i < n; i++){
            try{
                paroleTradotte.get(key).add(getHTML(listeParole.get(key).get(i)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static ArrayList<String> getListeParole(int key) {
        return listeParole.get(key);
    }
    public static ArrayList<String> getParoleTradotte(int key){
        return paroleTradotte.get(key);
    }

    private static String getHTML(String parolaDaTradurre) throws Exception {
        StringBuilder result = new StringBuilder();
        String indirizzo = "https://api.mymemory.translated.net/get?q=" + parolaDaTradurre + "&langpair=it|en";
        URL url = new URL(indirizzo);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        rd.close();
        JSONObject object = (JSONObject) new JSONParser().parse(result.toString());
        JSONObject object1 = (JSONObject) object.get("responseData");
        return (String) object1.get("translatedText");
    }

    public static void setPunteggio(String username, int punteggio) {
        userList.get(username).setPunteggio(punteggio);
    }
    public static void resetPunteggioPartita(String username){
        userList.get(username).resetPunteggioPartita();
    }
    public static String getVincitore(String username, String amico) {
        //non fare nulla finche` la partita non e` terminata per entrambi i giocatori
        while(!userList.get(username).isPartitaTerminata()){}
        while (!userList.get(amico).isPartitaTerminata()){}
        //a questo punto preleva i risultati e comunica il vincitore
        int punteggioU = userList.get(username).getPunteggioPartita();
        int punteggioA = userList.get(amico).getPunteggioPartita();

        //reset flag partita terminata
        userList.get(username).setPartitaTerminata(false);
        userList.get(amico).setPartitaTerminata(false);

        if(punteggioU > punteggioA)return username;
        if(punteggioA > punteggioU)return amico;
        else return "PAREGGIO";
    }

    public static void setTerminaPartita(String username){
        userList.get(username).setPartitaTerminata(true);
    }
    public static void addPunteggioBonus(String username){
        userList.get(username).addPunteggioBonus();
    }















    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    private static void createJsonWordFile() throws IOException {
        File directory = new File("WORD/");
        if(!directory.exists()) directory.mkdir();

        fileParole = new File("WORD/word.json");
        if(!fileParole.exists())fileParole.createNewFile();

        JSONObject objectJ = new JSONObject();
        JSONArray arrayJ = new JSONArray();

        arrayJ.addAll(Arrays.asList(ServerConfig.tmp));
        objectJ.put("ListaParole", arrayJ);

        FileWriter fileWriter = new FileWriter(fileParole);
        fileWriter.write(objectJ.toJSONString());
        fileWriter.close();
    }
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------


}
