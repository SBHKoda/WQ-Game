package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

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
    //private static SocketChannel clientSocketChannel = null;

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

    //private static ClientUI ui;

    //--------------------------------------------    INIZIALIZZAZIONE      --------------------------------------------
    public ClientUI(){
        initClientUI();
    }

    private void initClientUI() {
        try {
            clientSocket = new Socket("localhost", ClientConfig.PORT);
            invioAlServer = new DataOutputStream(clientSocket.getOutputStream());
            ricevoDalServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "ERRORE, server offline", "Server offline", JOptionPane.ERROR_MESSAGE);
        }
        //ui = this;

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

    private void createActionListener() {
        loginB.addActionListener(ae -> login());
        signUpB.addActionListener(ae -> signIn());
        logoutB.addActionListener(ae -> logout());
        invitaB.addActionListener(ae -> {
            try {
                invite();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listB.addActionListener(ae -> {
            try {
                friendList();
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
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                //Invio il comando al server per eseguire l'operazione di chiusura
                try {
                    invioAlServer.write(9);
                    invioAlServer.writeBytes(username + '\n');
                    clientSocket.close();
                    System.out.println("--------     DISCONNESSO     --------");
                    System.exit(1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //--------------------------------------------        SIGN IN        --------------------------------------------
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
                JOptionPane.showMessageDialog(null, "Utente registrato correttamete", "SUCCESSO", JOptionPane.INFORMATION_MESSAGE);
            else
                JOptionPane.showMessageDialog(null, "Utente gia registrato con questo nome", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static boolean checkValue(String username, String password) {
        if(username.length() == 0) {
            JOptionPane.showMessageDialog(null, "Inserisci un username.", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(!username.matches(ClientConfig.VALID_CHARACTERS)) {
            JOptionPane.showMessageDialog(null, "Caratteri non validi nel campo username", "ERRORE", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(username.length() > ClientConfig.MAX_LENGTH) {
            JOptionPane.showMessageDialog(null, "Username troppo lungo", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(password.length() == 0) {
            JOptionPane.showMessageDialog(null, "Inserisci una password.", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        if(!password.matches(ClientConfig.VALID_CHARACTERS)) {
            JOptionPane.showMessageDialog(null, "Caratteri non validi nel campo password", "ERRORE", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if(password.length() > ClientConfig.MAX_LENGTH) {
            JOptionPane.showMessageDialog(null, "Password troppo lunga", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }
    //--------------------------------------------          LOGIN          --------------------------------------------
    private void login() {
        try {
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

                    int port = generaPorta();

                    receiverUDP = new ReceiverUDP(port);
                    receiverUDP.start();
                    break;
                case 1:     //1 caso utente non registrato
                    JOptionPane.showMessageDialog(this, "Non sei registrato", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                case 2:     //2 in caso l'utente fosse gia loggato
                    JOptionPane.showMessageDialog(this, "Utente già connesso", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                case 3:     //3 password non corretta
                    JOptionPane.showMessageDialog(this, "Password non corretta", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(this, "Errore imprevisto in fase di Login", "ERRORE", JOptionPane.ERROR_MESSAGE);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //--------------------------------------------          LOGOUT          --------------------------------------------
    private void logout() {
        try {
            if(!onlineStatus){
                JOptionPane.showMessageDialog(this, "Utente gia offline", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
            //Invio il comando al server per eseguire l'operazione di logout
            invioAlServer.write(1);
            //Invio l'username e attendo il risultato dell'op dal server
            invioAlServer.writeBytes(username + '\n');
            int risultato = ricevoDalServer.read();
            if (risultato == 0){
                onlineStatus = false;
                statusLabel.setText("OFFLINE");
                repaint();
                receiverUDP.stopRunning();

                //clientSocket.close();
                System.out.println("--------     DISCONNESSO     --------");
                //System.exit(1);
            }
            else{
                JOptionPane.showMessageDialog(this, "ERRORE in fase di logout", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //----------------------------------------          AGGIUNGI AMICO          ----------------------------------------
    private void invite() throws IOException {
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi essere online per aggiungere amici", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
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
        int input = JOptionPane.showConfirmDialog(this, panel, "INVITA UTENTE", JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaInvitare = nomeUtenteDaInvitareField.getText();
            if (utenteDaInvitare == null || utenteDaInvitare.equals("")){
                JOptionPane.showMessageDialog(this, "ATTENZIONE, nome utente inserito non valido", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
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
        if(risultato == 0){
            JOptionPane.showMessageDialog(this, "Utente " + utenteDaInvitare + " invitato");
        }
        if(risultato == 1){
            JOptionPane.showMessageDialog(this, "L'utente che si vuole invitare non e` registrato al gioco", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
        if(risultato == 2){
            JOptionPane.showMessageDialog(this, "Siete gia amici", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
    }

    //------------------------------------------          LISTA AMICI          -----------------------------------------
    private void friendList() throws IOException {
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        invioAlServer.write(3);
        invioAlServer.writeBytes(username + '\n');

        String lista = ricevoDalServer.readLine();
        if( lista.equals("vuoto")){
            JOptionPane.showMessageDialog(this, "Non hai ancora nessun amico", "ATTENZIONE", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(lista);
        String prettyLista = gson.toJson(je);
        JOptionPane.showMessageDialog(this, prettyLista, "LISTA AMICI : " + '\n', JOptionPane.INFORMATION_MESSAGE);

    }
    //---------------------------------------------          SFIDA          --------------------------------------------
    private void inviaSfida() throws IOException {
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String utenteDaSfidare;
        //Creo una finestra con 2 campi di testo per inserire il nome del documento e il numero delle sezioni
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Utente da Sfidare ", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField utenteDaSfidareF = new JTextField();
        controls.add(utenteDaSfidareF);
        panel.add(controls, BorderLayout.CENTER);
        //Controllo se viene premuto OK o CANCEL
        int input = JOptionPane.showConfirmDialog(this, panel, "SFIDA", JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaSfidare = utenteDaSfidareF.getText();
            if (utenteDaSfidare == null || utenteDaSfidare.equals("")){
                JOptionPane.showMessageDialog(this, "ATTENZIONE, nome utente inserito non valido", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        else return;

        //Invio il comando al server per inoltrare la richiesta di sfida all'utente inserito
        invioAlServer.write(4);
        invioAlServer.writeBytes(username + '\n');
        invioAlServer.writeBytes(utenteDaSfidare + '\n');

        int risposta = ricevoDalServer.read();
        switch (risposta){
            case 0:
                String rispostaSfida = ricevoDalServer.readLine();
                if(rispostaSfida.equals("accetto")){
                   int N = ricevoDalServer.read();
                   avviaSfida(N);
                }
                else{
                    JOptionPane.showMessageDialog(this, "SFIDA RIFIUTATA", "ESITO SFIDA", JOptionPane.INFORMATION_MESSAGE);
                }
                break;
            case 1:
                JOptionPane.showMessageDialog(this, "I nomi utenti non possono essere null", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 2:
                JOptionPane.showMessageDialog(this, "I nomi utenti non possono essere vuoti", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 3:
                JOptionPane.showMessageDialog(this, "L'utente sfidato non esiste", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 4:
                JOptionPane.showMessageDialog(this, "Non siete amici, non potete ancora sfidarvi", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 5:
                JOptionPane.showMessageDialog(this, "L'utene sfidato non e` online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 6:
                JOptionPane.showMessageDialog(this, "Non puoi sfidare te stesso", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            case 7:
                JOptionPane.showMessageDialog(this, "L'utente sfidato e` gia` impegnato in un altra sfida", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                break;
            default:
        }
    }
    //Metodo che comunica al proprio clientTask che lo sta servendo che sta iniziando la sfida, quindi questo andra` a
    // prelevare le parole sul server e le inviera` alla UI
    public static void sfidaRicevuta(String sfidante) throws IOException {
        invioAlServer.write(5);
        invioAlServer.writeBytes(sfidante + '\n');

        int N = ricevoDalServer.read();
        avviaSfida(N);
    }

    private static void avviaSfida(int N)throws IOException{
        String parolaDT;
        for(int i = 0; i < N; i++){
            parolaDT = ricevoDalServer.readLine();
            //Creo una finestra con 1 campo di testo per inserire il nome utente da invitare
            JPanel panelSfida = new JPanel(new BorderLayout(8, 8));

            JPanel labelSfida = new JPanel(new GridLayout(0, 1, 1, 1));
            labelSfida.add(new JLabel("Parola da Tradurre : " + parolaDT + " ", SwingConstants.RIGHT));
            panelSfida.add(labelSfida, BorderLayout.WEST);

            JPanel controlsSfida = new JPanel(new GridLayout(0, 1, 1, 1));
            JTextField traduzione = new JTextField();
            traduzione.setColumns(8);
            controlsSfida.add(traduzione);
            panelSfida.add(controlsSfida, BorderLayout.CENTER);
            //Controllo se viene premuto OK o CANCEL
            int input1 = JOptionPane.showConfirmDialog(null, panelSfida, "GIOCATORE: "+ usernameField.getText() +" Parola [ " + (i+1) + " ] di [ " + N + " ]", JOptionPane.OK_CANCEL_OPTION);
            if(input1 == 0){//Caso OK
                invioAlServer.writeBytes(traduzione.getText() + '\n');
            }
            else invioAlServer.writeBytes("" + '\n');
        }
        //Finito il ciclo ricevo dal server il nome del vincitore
        String vincitore = ricevoDalServer.readLine();
        System.out.println("VINCITORE : " + vincitore);
        if(vincitore.equals(usernameField.getText()))
            JOptionPane.showMessageDialog(null, "Sei il vincitore della sfida", "Vittoria", JOptionPane.INFORMATION_MESSAGE);
    }
    //-------------------------------------------          PUNTEGGIO          ------------------------------------------
    private void punteggio() {
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            invioAlServer.write(6);
            invioAlServer.writeBytes(username + '\n');
            int punteggio = ricevoDalServer.read();

            JOptionPane.showMessageDialog(this, "Il tuo punteggio totale : " + punteggio, "PUNTEGGIO", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //-------------------------------------------          CLASSIFICA          -----------------------------------------
    private void classifica() throws IOException {
        if(!onlineStatus){
            JOptionPane.showMessageDialog(this, "Devi prima essere online", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            return;
        }
        invioAlServer.write(7);
        String classifica = ricevoDalServer.readLine();
        if(classifica.equals("vuoto")){
            JOptionPane.showMessageDialog(this, "Non hai ancora nessun amico", "ATTENZIONE", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        System.out.println(classifica);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(classifica);
        String prettyClassifica = gson.toJson(je);
        JOptionPane.showMessageDialog(this, prettyClassifica, "CLASSIFICA GIOCATORI", JOptionPane.INFORMATION_MESSAGE);

    }

    //--------------------------------------------          UTILITY          -------------------------------------------
    private SocketChannel createChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        int port = username.hashCode() % 65535;
        if(port < 0)
            port = -port % 65535;
        if(port < 1024)
            port += 1024;
        SocketAddress socketAddr = new InetSocketAddress("localhost", port);
        socketChannel.connect(socketAddr);
        return socketChannel;
    }

    private int generaPorta() {
        int port = username.hashCode() % 65535;
        if(port < 0)
            port = -port % 65535;
        if(port < 1024)
            port += 1024;
        return port + 300;
    }
}
