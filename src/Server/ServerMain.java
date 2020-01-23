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

    //Lista parole sfida
    private static ConcurrentHashMap<Integer, ArrayList<String>> listeParole;
    private static ConcurrentHashMap<Integer, ArrayList<String>> paroleTradotte;

    //ServerSocket per iniziare le connessioni con il server
    private static ServerSocket welcomeSocket;

    //ThreadPool
    private static ExecutorService executorService;

    //File json
    private static File fileUtenti;
    private static File fileAmicizie;

    //--------------------------------------------         MAIN            --------------------------------------------
    public static void main(String[] args){
        initServer();
        initServerCycle();
    }

    private static void initServer() {
        //Inizializzazione delle strutture dati necessarie per il corretto funzionamento del server
        userList = new ConcurrentHashMap<>();
        friendList = new HashMap<>();
        listeParole = new ConcurrentHashMap<>();
        paroleTradotte = new ConcurrentHashMap<>();
        //Creo una directory per per salvare la lista degli utenti registrati a WQ in caso non esistesse
        File directory = new File("UTENTI/");
        if(!directory.exists())directory.mkdir();
        try {
            //Creo il file json nel caso non esistesse, altrimenti lo leggo per ricreare il database utenti
            fileUtenti = new File("UTENTI/ListaUtenti.json");
            if(!fileUtenti.exists() || fileUtenti.length() == 0)fileUtenti.createNewFile();
            else{
                JSONParser parser = new JSONParser();
                Object object = parser.parse(new FileReader("UTENTI/ListaUtenti.json"));
                JSONObject objectJ = (JSONObject) object;
                JSONArray arrayJ = (JSONArray) objectJ.get("Lista Utenti");

                for (JSONObject jsonObject : (Iterable<JSONObject>) arrayJ) {
                    String username, password;
                    int punteggio;
                    username = jsonObject.get("username").toString();
                    password = jsonObject.get("password").toString();
                    punteggio = Integer.parseInt(jsonObject.get("punteggio").toString());
                    User user = new User(username, password);
                    user.ripristinaPunteggioTotale(punteggio);

                    userList.put(username, user);
                }
            }

            //Creo il file json nel caso non esistesse, altrimenti lo leggo per ricreare il database amicizie utenti
            fileAmicizie = new File("UTENTI/Amicizie.json");
            if(!fileAmicizie.exists() || fileAmicizie.length() == 0)fileAmicizie.createNewFile();
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
                        arrayList.addAll(arrayJ);//copio le altre vecchie amicizie
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
            welcomeSocket = new ServerSocket(ServerConfig.PORT, 0, address);
        } catch (IOException e) {
            System.out.println("ERRORE nella creazione della ServerSocket.");
        }

        //Inizzializzo il ThreadPool Executor che gestira` le richieste dei client eseguendo task ClientTask, ho
        // scelto un CachedThreadPool in modo che possa gestire autonomamete il numero di thread
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
        //In questo ciclo infinito aspetto i client, quando ne arriva uno creo una socket per il client e creo e passo
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
    // 3 password non corretta
    public static int login(String username, String password){
        //controllo se username e` registrato a WQ
        if(userList.containsKey(username)){
            if(!userList.get(username).checkOnlineStatus()){
               if(userList.get(username).checkPassword(password)){
                   userList.get(username).setOnline();
                   return 0;
               }
               //password non valida
               return 3;
            }
            //utente gia online
            return 2;
        }
        //utente non registrato
        return 1;
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
            //Creo un Json obj e un Json array per ognuno e aggiungo gli username alle rispettive liste degli amici
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

                while(iterator.hasNext()){
                    String username = iterator.next();

                    JSONArray arrayJ = (JSONArray) objectJ.get(username);
                    JSONArray newArrayJ = new JSONArray();
                    if(arrayJ != null){
                        //copio le altre vecchie amicizie
                        for (String s : (Iterable<String>) arrayJ) newArrayJ.add(s);
                    }
                    //Aggiungo le nuove amicizie
                    if(username.equals(nickUser)){
                        newArrayJ.add(nickFriend);
                    }
                    if(username.equals(nickFriend)){
                        newArrayJ.add(nickUser);
                    }
                    finalObject.put(username, newArrayJ);
                }
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
        if(friendList.isEmpty())return new ArrayList<>();
        return friendList.get(nomeUtente);
    }

    //------------------------------------------       CONTROLLO SFIDA         -----------------------------------------
    // 0 se tutto ok
    // 1 nickUser o nickFriend sono null
    // 2 nickUser o nickFriend sono ""
    // 3 nickFriend non esiste
    // 4 i due utenti non sono amici quindi non possono sfidarsi
    // 5 nickFirend offline non puo` essere sfidato
    // 6 nickUser == nickFriend non puoi sfidare te stesso
    // 7 nickFriend gia` impegnato in una sfida
    public static int controlloPreSfida(String nickUser, String nickFriend) {
        if(nickUser == null || nickFriend == null){
            System.out.println("ERRORE, i campi nickUser e nickFirend non possono essere null");
            return 1;
        }
        if(nickUser.equals("") || nickFriend.equals("")){
            System.out.println("ERRORE, i campi nickUser e nickFriend non possono essere vuoti");
            return 2;
        }
        if(!userList.containsKey(nickFriend)){
            System.out.println("ERRORE, nickFriend non esiste");
            return 3;
        }
        //Caso in cui i due utenti non sono amici
        if(!friendList.get(nickUser).contains(nickFriend) || !friendList.get(nickFriend).contains(nickUser)){
            System.out.println("ERRORE, nickUser e nickFriend non sono amici, quindi non si possono sfidare");
            return 4;
        }
        if(!userList.get(nickFriend).checkOnlineStatus()){
            System.out.println("ERRORE, Amico OFFLINE, non si puo` sfidare");
            return 5;
        }
        if(nickUser.equals(nickFriend)){
            System.out.println("ERRORE, stai cercando di sfidare te stesso");
            return 6;
        }
        if(userList.get(nickFriend).getInSfida()){
            System.out.println("ERRORE, Amico gia` impegnato in una sfida");
            return 7;
        }
        return 0;
    }
    //-------------------------------------------           SFIDA          ---------------------------------------------
    //Metodo che genera le N parole della sfida e le salva nella hash map listeParole usando come key username.hashCode
    // dell'utente che ha inviato la richiesta di sfida
    public synchronized static void setParolePartita(int n, int key) {
        JSONParser parser = new JSONParser();
        if(!listeParole.containsKey(key))listeParole.put(key, new ArrayList<>());
        else
            listeParole.get(key).clear(); //Cancello le eventuali parole generate per una vecchia sfida
        try {
            //Leggo il file word.json e ottengo un JSONArray che contiene tutte le parole e ne estraggo N a caso usando
            //Random
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
    //Metodo che ottienela lista delle parole tradotte
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

    public static void setPunteggio(String username, int punteggio) throws IOException, ParseException {
        userList.get(username).setPunteggio(punteggio);
    }
    public static void resetPunteggioPartita(String username) {
        userList.get(username).resetPunteggioPartita();
    }
    public static String getVincitore(String username, String amico) throws InterruptedException {
        //non fare nulla finche` la partita non e` terminata anche per l'avversario
        int i = 0;
        while (userList.get(amico).getInSfida()){
            //Thread.sleep(1000);
            //System.out.println("ATTESA[" + i + "]");
            //i++;
        }
        //a questo punto preleva i risultati e comunica il vincitore
        int punteggioU = userList.get(username).getPunteggioPartita();
        System.out.println("PunteggioU : " + punteggioU + " di " + username);
        int punteggioA = userList.get(amico).getPunteggioPartita();
        System.out.println("PunteggioA : " + punteggioA + " di " + amico);

        if(punteggioU > punteggioA)return username;
        if(punteggioA > punteggioU)return amico;
        else return "PAREGGIO";
    }

    public static void addPunteggioBonus(String username){
        userList.get(username).addPunteggioBonus();
    }
    public static void setInSfida(String username){
        userList.get(username).setInSfida(true);
    }
    public static void resetInSfida(String username){
        userList.get(username).setInSfida(false);
    }
    //Metodo usato per inviare le richieste GET HTML per ottenere la traduzione delle parole da italiano a inglese
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
    //-------------------------------------------           PUNTEGGIO         ------------------------------------------
    public static int getPunteggio(String username) {
            return userList.get(username).getPunteggioTotale();
    }
    //Metodo che aggiorna il file listaUtenti.json modificando il punteggio totale degli utenti
    public static void updatePunteggi(String utente1, String utente2) throws IOException, ParseException {

        //Ottengo il nuovo punteggio totale aggiornato
        int punteggioTotale1 = userList.get(utente1).getPunteggioTotale();
        int punteggioTotale2 = userList.get(utente2).getPunteggioTotale();

        JSONParser parser = new JSONParser();
        try {
            Object object = parser.parse(new FileReader("UTENTI/ListaUtenti.json"));
            JSONObject objectJ = (JSONObject) object;
            JSONArray arrayJ = (JSONArray) objectJ.get("Lista Utenti");

            CopyOnWriteArrayList<JSONObject> a = new CopyOnWriteArrayList<>(arrayJ);
            JSONArray newArrayJ = new JSONArray();

            for (JSONObject tempOJ : a) {
                String usernameTmp = tempOJ.get("username").toString();
                if (usernameTmp.equals(utente1)) {
                    tempOJ.put("punteggio", punteggioTotale1);
                }
                if (usernameTmp.equals(utente2)) {
                    tempOJ.put("punteggio", punteggioTotale2);
                }
                newArrayJ.add(tempOJ);
            }
        //Creo un nuovo JSON obj in cui inserisco la l'array JSON appena creato e lo scrivo nel file
        FileWriter fileWriter = new FileWriter(fileUtenti);
        JSONObject newObjectJ = new JSONObject();
        newObjectJ.put("Lista Utenti", newArrayJ);
        fileWriter.write(newObjectJ.toJSONString());
        fileWriter.close();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }
    //-------------------------------------------           CLASSIFICA         -----------------------------------------
    public static String getClassifica(String username) {
        //Ottengo la lista degli amici dell'utente e creo una classifica temporanea non ordinata con una hash map di
        // <Username, PunteggioTotale>

        //Ritorno "vuoto" nel caso in cui non ho nessun amico, quindi la classifica e` vuota
        if(friendList.isEmpty()) return "vuoto";

        ArrayList<String> amici = new ArrayList<>(friendList.get(username));
        ArrayList<User> list = new ArrayList<>();
        for (String s : amici) {
            list.add(userList.get(s));
        }
        list.add(userList.get(username));

        //Ordino la lista in base al punteggio totale
        Collections.sort(list, Comparator.comparing(User::getPunteggioTotale));
        Collections.reverse(list);
        JSONObject object1 = new JSONObject();

        int pos = 1;
        String posizione;

        for(User u : list){
            JSONObject object = new JSONObject();
            posizione = "posizione [ " + pos + " ]";
            System.out.println(u.getUsername() + "\t" + u.getPunteggioTotale());
            object.put(u.getUsername() , u.getPunteggioTotale());
            object1.put(posizione, object);
            pos++;
        }
        //Ottengo in questo modo un JSONObject con la classifica, che ritorno in formato String
        return object1.toJSONString();
    }
}
