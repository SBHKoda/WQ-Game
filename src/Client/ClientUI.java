package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicBoolean;

import Server.ServerInterfaceRMI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ClientUI extends JFrame {

    //Per la comunizazione tra client e server
    private static Socket clientSocket;
    private static BufferedReader ricevoDalServer;
    private static DataOutputStream invioAlServer;

    //Strutture necessarie per la creazione della UI
    private static JPasswordField passwordField;
    private static JTextField usernameField;

    private static JButton loginB;
    private static JButton signUpB;
    private static JButton logoutB;
    private static JButton invitaB;
    private static JButton sfidaB;
    private static JButton mostraPunteggioB;
    private static JButton mostraClassificaB;
    private static JButton listB;

    private static JLabel statusLabel;

    private boolean onlineStatus = false;
    private String username;
    private ReceiverUDP receiverUDP;
    private static ClientUI clientUI;

    //--------------------------------------------    INIZIALIZZAZIONE      --------------------------------------------
    public ClientUI(){
        initClientUI();
    }

    //Inizializza il client e la UI
    private void initClientUI() {
        try {
            clientSocket = new Socket("localhost", ClientConfig.PORT);
            invioAlServer = new DataOutputStream(clientSocket.getOutputStream());
            ricevoDalServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "ERRORE, server offline",
                    "SERVER OFFLINE", JOptionPane.ERROR_MESSAGE);
        }
        clientUI = this;

        setLayout(null); //Per gestire manualmente tutta l'interfaccia

        usernameField = new JTextField("username");
        passwordField = new JPasswordField("password");

        statusLabel = new JLabel("OFFLINE");
        statusLabel.setForeground(Color.white);

        loginB = new JButton("Login");
        signUpB = new JButton("Sign In");
        logoutB = new JButton("Logout");
        invitaB = new JButton("Aggiungi Amico");
        listB = new JButton("Lista Amici");
        sfidaB = new JButton("Sfida");
        mostraPunteggioB = new JButton("Punteggio");
        mostraClassificaB = new JButton("Classifica");

        posComponent();
        createActionListener();

        add(usernameField);
        add(passwordField);
        add(statusLabel);
        add(signUpB);
        add(loginB);
        add(logoutB);
        add(invitaB);
        add(listB);
        add(sfidaB);
        add(mostraPunteggioB);
        add(mostraClassificaB);
    }

    //Metodo in cui viene impostata la posizione (fissa) dei componenti della UI
    private static void posComponent() {
        usernameField.setBounds(10, 10, 150, 20);
        passwordField.setBounds(195, 10, 150, 20);

        statusLabel.setBounds(155, 40, 90, 20);

        signUpB.setBounds(100, 80, 160, 30);
        loginB.setBounds(100, 120, 160, 30);
        logoutB.setBounds(100,160,160,30);
        sfidaB.setBounds(100, 200, 160, 30);

        invitaB.setBounds(15, 240, 150, 40);
        listB.setBounds(195, 240, 150, 40);
        mostraPunteggioB.setBounds(15, 290, 150, 40);
        mostraClassificaB.setBounds(195, 290, 150, 40);
    }

    //Metodo che crea gli ActionListener per i bottoni della UI
    private void createActionListener() {
        loginB.addActionListener(ae -> login());
        signUpB.addActionListener(ae -> signIn());
        logoutB.addActionListener(ae -> logout());
        invitaB.addActionListener(ae -> {
            try {
                aggiungiAmico();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listB.addActionListener(ae -> {
            try {
                listaAmici();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        sfidaB.addActionListener(ae -> {
            try {
                inviaSfida();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        mostraPunteggioB.addActionListener(ae -> punteggio());
        mostraClassificaB.addActionListener(ae -> {
            try {
                classifica();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        //Definisco le operazioni che devono essere eseguite quando la UI viene chiusa
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                //Invio al server il comando per eseguire l'operazione di logout quando la UI viene chiusa
                try {
                    invioAlServer.write(9);
                    invioAlServer.writeBytes(username + '\n');
                    receiverUDP.stopRunning();
                    clientSocket.close();
                    System.out.println("------------     DISCONNESSO     ------------");
                    System.exit(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //---------------------------------------------        SIGN IN         ---------------------------------------------
    //Metodo che viene eseuito quando viene premuto il bottone sign in per la registrazione al gioco usando RMI
    private void signIn() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        //Controllo che i valori inseriti siano validi
        boolean checkResult = checkValue(username, password);
        if(checkResult){
            boolean registrationResult = false;
            try {
                Registry registry = LocateRegistry.getRegistry(ClientConfig.REG_PORT);
                ServerInterfaceRMI stub = (ServerInterfaceRMI) registry.lookup("Server");
                registrationResult = stub.registra_utente(username, password);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
            if(registrationResult)
                JOptionPane.showMessageDialog(this, "Utente registrato correttamete",
                        "SUCCESSO", JOptionPane.INFORMATION_MESSAGE);
            else
                JOptionPane.showMessageDialog(this, "Utente gia registrato con questo nome",
                        "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
    }

    //Metodo che controlla i valori username e password, restituisce
    // true se i valori username e password sono validi
    // false e apre un dialog che mostra il messaggio di errore a seconda del caso
    private boolean checkValue(String username, String password) {
        if(username.length() == 0) {
            JOptionPane.showMessageDialog(this, "Inserisci un username.", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(!username.matches(ClientConfig.VALID_CHARACTERS)) {
            JOptionPane.showMessageDialog(this, "Caratteri non validi nel campo username",
                    "ERRORE", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(username.length() > ClientConfig.MAX_LENGTH) {
            JOptionPane.showMessageDialog(this, "Username troppo lungo", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(password.length() == 0) {
            JOptionPane.showMessageDialog(this, "Inserisci una password.", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(!password.matches(ClientConfig.VALID_CHARACTERS)) {
            JOptionPane.showMessageDialog(this, "Caratteri non validi nel campo password",
                    "ERRORE", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(password.length() > ClientConfig.MAX_LENGTH) {
            JOptionPane.showMessageDialog(this, "Password troppo lunga", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
    //--------------------------------------------          LOGIN          ---------------------------------------------
    //Metodo che viene eseuito quando viene premuto il bottone login
    private void login() {
        try {
            //Invio al server il codice (int) per eseguire il comando desiderato e gli altri eventuali campi neceessari
            invioAlServer.write(0 );
            username = usernameField.getText();
            invioAlServer.writeBytes(username + '\n');
            invioAlServer.writeBytes(new String(passwordField.getPassword()) + '\n');
            //Ricevo dal Server il risultato
            int risultato = ricevoDalServer.read();
            switch (risultato){
                case 0:     //0 in caso di login corretto
                    onlineStatus = true;
                    statusLabel.setText("ONLINE");
                    repaint();

                    receiverUDP = new ReceiverUDP(generaPorta(), this);
                    receiverUDP.start();
                    break;
                case 1:     //1 caso utente non registrato
                    JOptionPane.showMessageDialog(this, "Non sei registrato",
                            "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                case 2:     //2 in caso l'utente fosse gia loggato
                    JOptionPane.showMessageDialog(this, "Utente già connesso",
                            "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                case 3:     //3 password non corretta
                    JOptionPane.showMessageDialog(this, "Password non corretta",
                            "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Errore imprevisto in fase di Login",
                            "ERRORE", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //--------------------------------------------          LOGOUT          --------------------------------------------
    //Metodo che viene eseuito quando viene premuto il bottone logout
    private void logout() {
        try {
            //Posso eseguire il logout solo se sono OnLine
            if(!onlineStatus){
                JOptionPane.showMessageDialog(this, "Utente gia offline", "ATTENZIONE",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            //Invio il comando al server per eseguire l'operazione di logout
            invioAlServer.write(1);
            invioAlServer.writeBytes(username + '\n');
            //Attendo il risultato dell'operazione dal server
            int risultato = ricevoDalServer.read();
            if (risultato == 0){
                onlineStatus = false;
                statusLabel.setText("OFFLINE");
                repaint();
                receiverUDP.stopRunning();
                System.out.println("--------     DISCONNESSO     --------");
            }
            else{
                JOptionPane.showMessageDialog(this, "ERRORE in fase di logout",
                        "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //----------------------------------------          AGGIUNGI AMICO          ----------------------------------------
    //Metodo che viene eseuito quando viene premuto il bottone aggiungi amico
    private void aggiungiAmico() throws IOException {
        //Posso eseguire il comando aggiungi amico solo se sono OnLine
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi essere online per aggiungere amici",
                    "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String utenteDaInvitare;

        //Creo una finestra con 1 campo di testo per inserire il nome utente da invitare
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Utente da invitare : ", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField nomeUtenteDaInvitareField = new JTextField();
        controls.add(nomeUtenteDaInvitareField);
        panel.add(controls, BorderLayout.CENTER);
        //Controllo se viene premuto OK o CANCEL
        int input = JOptionPane.showConfirmDialog(this, panel, "INVITA UTENTE",
                JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaInvitare = nomeUtenteDaInvitareField.getText();
            if (utenteDaInvitare == null || utenteDaInvitare.equals("")){
                JOptionPane.showMessageDialog(this, "ATTENZIONE, username inserito non valido",
                        "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        else return;
        //Invio al server il comando e le informazioni necessarie
        invioAlServer.write(2);
        invioAlServer.writeBytes(username + '\n');
        invioAlServer.writeBytes(utenteDaInvitare + '\n');
        //Attendo il risultato della operazione da parte del server
        int risultato = ricevoDalServer.read();
        //Apro un dialog per comunicare al giocatore l'esito dell'operazione
        if(risultato == 0){
            JOptionPane.showMessageDialog(this, "Utente " + utenteDaInvitare + " invitato");
        }
        if(risultato == 1){
            JOptionPane.showMessageDialog(this,
                    "L'utente che si vuole invitare non e` registrato al gioco", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
        }
        if(risultato == 2){
            JOptionPane.showMessageDialog(this, "Siete gia amici", "ATTENZIONE",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    //------------------------------------------          LISTA AMICI          -----------------------------------------
    //Metodo che viene eseuito quando viene premuto il bottone lista amici
    private void listaAmici() throws IOException {
        //Per poter richiedere al server la lista degli amici devo essere online
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online",
                    "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        //Invio al server le informazioni necessarie
        invioAlServer.write(3);
        invioAlServer.writeBytes(username + '\n');

        String lista = ricevoDalServer.readLine();
        //Se il server mi invia una stringa con scritto "vuoto" significa che la lista amici era vuota => ancora nessun
        // amico
        if( lista.equals("vuoto")){
            JOptionPane.showMessageDialog(this, "Non hai ancora nessun amico",
                    "ATTENZIONE", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        //Uso Gson per stampare su un dialog la lista degli amici in formato json
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(lista);
        String prettyLista = gson.toJson(je);
        JOptionPane.showMessageDialog(this, prettyLista, "LISTA AMICI",
                JOptionPane.INFORMATION_MESSAGE);
    }
    //---------------------------------------------          SFIDA          --------------------------------------------
    private void inviaSfida() throws IOException {
        //Devo essere online per mandare una richiesta di sfida
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String utenteDaSfidare;
        //Creo una finestra con 1 campo di testo per inserire il nome del giocatore da sfidare
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Utente da Sfidare ", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField utenteDaSfidareF = new JTextField();
        controls.add(utenteDaSfidareF);
        panel.add(controls, BorderLayout.CENTER);

        //Controllo se viene premuto OK o CANCEL
        int input = JOptionPane.showConfirmDialog(clientUI, panel, "SFIDA", JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaSfidare = utenteDaSfidareF.getText();
            if (utenteDaSfidare == null || utenteDaSfidare.equals("")){
                JOptionPane.showMessageDialog(this, "ATTENZIONE, nome utente inserito non valido", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        else return;

        //Invio il comando al server per inoltrare la richiesta di sfida
        invioAlServer.write(4);
        invioAlServer.writeBytes(utenteDaSfidare + '\n');

        //Attendo la risposta
        int risposta = ricevoDalServer.read();
        if(risposta == 0) {
            String rispostaSfida = ricevoDalServer.readLine();
            if (rispostaSfida.equals("accetto")) {
                sfidaAccettata(username);
            } else
                JOptionPane.showMessageDialog(this, "SFIDA RIFIUTATA", "ESITO SFIDA", JOptionPane.INFORMATION_MESSAGE);
        }
        if(risposta == 1) {
            JOptionPane.showMessageDialog(this, "I nomi utenti non possono essere null", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 2) {
            JOptionPane.showMessageDialog(this, "I nomi utenti non possono essere vuoti", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 3) {
            JOptionPane.showMessageDialog(this, "L'utente sfidato non esiste", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 4) {
            JOptionPane.showMessageDialog(this, "Non siete amici, non potete ancora sfidarvi", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 5) {
            JOptionPane.showMessageDialog(this, "L'utene sfidato non e` online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 6) {
            JOptionPane.showMessageDialog(this, "Non puoi sfidare te stesso", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if(risposta == 7) {
            JOptionPane.showMessageDialog(this, "L'utente sfidato e` gia` impegnato in un altra sfida", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }

    }
    //Comunico al clientTask che la sfida e` stata accettata e avvio la sfida
    public static void sfidaAccettata(String sfidante) throws IOException {
        invioAlServer.write(5);
        invioAlServer.writeBytes(sfidante + '\n');
        //Ricevo dal server il numero di parole N da tradurre
        int N = ricevoDalServer.read();

        avviaSfida(N);
    }

    private static void avviaSfida(int N)throws IOException{
        AtomicBoolean flag = new AtomicBoolean(false);
        //Imposto un timer che interrompera` il ciclo e inviera` al server un messaggio "INTERRUZIONE"
        Timer timer = new Timer(ClientConfig.TIMEOUT, e -> {
            try {
                invioAlServer.writeBytes("INTERRUZIONE" +'\n');
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            flag.set(true);
        });
        timer.start();
        int i = 0;
        String parolaDaTradurre = "";
        while(!flag.get() && i < N){
            try {
                if(!flag.get()) parolaDaTradurre = ricevoDalServer.readLine();
                String risposta = (String) JOptionPane.showInputDialog(clientUI,"Parola da tradurre["+ (i+1) +"] : " + parolaDaTradurre,
                                       "Giocatore : " + usernameField.getText(), JOptionPane.PLAIN_MESSAGE, null,null, null);
                if(!flag.get() && risposta != null) {
                    invioAlServer.writeBytes(risposta + '\n');
                }
                i++;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        //TODO: testa se funziona lo stesso senza flush
        if(flag.get()) invioAlServer.flush();

        //Finito il ciclo ricevo dal server il nome del vincitore
        String vincitore = ricevoDalServer.readLine();

        System.out.println("VINCITORE : " + vincitore);
        //Se sono il vincitore della sfida apri un dialog che me lo mostra
        if(vincitore.equals(usernameField.getText()))
            JOptionPane.showMessageDialog(clientUI, "Sei il vincitore della sfida", "Vittoria", JOptionPane.PLAIN_MESSAGE);
        //Se la sfida e` finita in pareggio apri un dialog che me lo mostri
        if(vincitore.equals("PAREGGIO"))
            JOptionPane.showMessageDialog(clientUI, "Sfida terminata con un pareggio", "Pareggio", JOptionPane.PLAIN_MESSAGE);
    }

    //-------------------------------------------          PUNTEGGIO          ------------------------------------------
    private void punteggio() {
        //Posso richiedere il mio punteggio totale solo se sono online
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            //Invio la richiesta al server e apro un dialog che mostra il punteggio totale
            invioAlServer.write(6);
            int punteggio = ricevoDalServer.read();
            JOptionPane.showMessageDialog(this, "Il tuo punteggio totale : " + punteggio, "PUNTEGGIO", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //-------------------------------------------          CLASSIFICA          -----------------------------------------
    private void classifica() throws IOException {
        //Posso richiedere la classifica solo se sono online
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        //Invio la richiesta al server e ottengo la classifica in formato String
        invioAlServer.write(7);
        String classifica = ricevoDalServer.readLine();
        //Se il server mi risponde "vuoto" => non ho ancora nessun amico
        if(classifica.equals("vuoto")){
            JOptionPane.showMessageDialog(this, "Non hai ancora nessun amico", "ATTENZIONE", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        //Uso Gson per mostrare la classifica in formato json in un dialog
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(classifica);
        String prettyClassifica = gson.toJson(je);
        JOptionPane.showMessageDialog(this, prettyClassifica, "CLASSIFICA GIOCATORI", JOptionPane.INFORMATION_MESSAGE);
    }

    //--------------------------------------------          UTILITY          -------------------------------------------
    //Metodo per generare una porta usando il proprio username ed evitando le porte well-known
    private int generaPorta() {
        int port = username.hashCode() % 65535;
        if(port < 0)
            port = -port % 65535;
        if(port < 1024)
            port += 1024;
        return port + 300;
    }
}
