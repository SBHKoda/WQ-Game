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
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;

import Server.ServerInterfaceRMI;

public class ClientUI extends JFrame {

    //Per la comunizazione tra client e server
    private static Socket clientSocket;
    private static BufferedReader ricevoDalServer;
    private static DataOutputStream invioAlServer;
    private static SocketChannel clientSocketChannel = null;

    //Strutture necessarie per la creazione della UI
    private static JPasswordField passwordField;
    private static JTextField usernameField;
    private static JButton loginB;
    private static JButton signUpB;
    private static JButton logoutB;
    private static JButton invitaB;
    private JButton creaTdocB;
    private JButton showSectionB;
    private JButton showDocumentB;
    private JButton editB;
    private static JButton listB;
    private JButton endEditB;
    private static JLabel statusLabel;

    private boolean onlineStatus = false;
    private String username;

    //Per la chat durante la modifica delle sezioni di file
    private MulticastSocket chatSocket;
    private InetAddress group;
    private Game game;
    private Calendar calendar;

    //Per attivare il servizio calback per le notifiche
    private ServerInterfaceRMI stub;
    private ArrayBlockingQueue<String> msgList;
    //private NotifyReceiver receiver;

    //Strutture necessarie per la game UI
    private JButton inviaRispostaB;
    private JTextArea insertArea, areaParolaDaTradurre;

    //TODO: aggiungi altre strutture per i punteggi


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

        setLayout(null);			                        //Per gestire manualmente tutta l'interfaccia

        usernameField = new JTextField("username");
        passwordField = new JPasswordField("password");

        statusLabel = new JLabel("OFFLINE");
        statusLabel.setForeground(Color.white);

        loginB = new JButton("Login");
        signUpB = new JButton("Sign In");
        logoutB = new JButton("Logout");
        invitaB = new JButton("Aggiungi Amico");
        listB = new JButton("Lista Amici");

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
    }

    private static void posComponent() {
        statusLabel.setBounds(350, 10, 90, 20);

        usernameField.setBounds(10, 10, 150, 20);
        usernameField.setColumns(10);
        passwordField.setBounds(170, 10, 150, 20);

        signUpB.setBounds(640, 10, 140, 30);
        loginB.setBounds(640, 60, 140, 30);
        logoutB.setBounds(640,110,140,30);
        invitaB.setBounds(640, 160, 140, 30);
        listB.setBounds(640, 210, 140, 30);
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
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                //Invio il comando al server per eseguire l'operazione di chiusura
                try {
                    invioAlServer.write(9);
                    invioAlServer.writeBytes(username + '\n');
                    clientSocket.close();
                    if(clientSocketChannel != null)clientSocketChannel.close();
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
                stub = (ServerInterfaceRMI) registry.lookup("Server");
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
            System.out.println("----    Risultato ottenuto : " + risultato);
            switch (risultato){
                case 0:     //0 in caso di login corretto
                    onlineStatus = true;
                    statusLabel.setText("ONLINE");
                    repaint();

                    if(clientSocketChannel == null)
                        clientSocketChannel = createChannel();
                    break;
                case 1:     //1 in caso di credenziali non corrette
                    JOptionPane.showMessageDialog(null, "Username o Password non validi", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                case 2:     //2 in caso l'utente fosse gia loggato
                    JOptionPane.showMessageDialog(null, "Utente già connesso.", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                    break;
                default:
                    JOptionPane.showMessageDialog(null, "Errore imprevisto", "ERRORE", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(null, "Utente gia offline", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
            //Invio il comando al server per eseguire l'operazione di logout
            invioAlServer.write(1);
            //Invio l'username e attendo il risultato dell'op dal server
            invioAlServer.writeBytes(username + '\n');
            int risultato = ricevoDalServer.read();
            if (risultato == 0){
                clientSocket.close();
                clientSocketChannel.close();
                System.out.println("--------     DISCONNESSO     --------");
                System.exit(1);
            }
            else{
                JOptionPane.showMessageDialog(null, "ERRORE in fase di logout", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //----------------------------------------          AGGIUNGI AMICO          ----------------------------------------
    private void invite() throws IOException {
        String utenteDaInvitare;

        //Creo una finestra con 1 campo di testo per inserire il nome utente da invitare
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Utente da invitare", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField nomeUtenteDaInvitareField = new JTextField();
        controls.add(nomeUtenteDaInvitareField);
        panel.add(controls, BorderLayout.CENTER);
        //Controllo se viene premuto OK o CANCEL
        int input = JOptionPane.showConfirmDialog(null, panel, "INVITE", JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaInvitare = nomeUtenteDaInvitareField.getText();
            if (utenteDaInvitare == null || utenteDaInvitare.equals("")){
                JOptionPane.showMessageDialog(null, "ATTENZIONE, nome utente inserito non valido", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        else return;
        //Invio al server il comando e le informazioni necessarie
        invioAlServer.write(2);

        invioAlServer.writeBytes(username + '\n');
        invioAlServer.writeBytes(utenteDaInvitare + '\n');

        int risultato = ricevoDalServer.read();
        if(risultato == 0){
            JOptionPane.showMessageDialog(null, "Utente " + utenteDaInvitare + " invitato");
        }
        if(risultato == 1){
            JOptionPane.showMessageDialog(null, "L'utente che si vuole invitare non e` registrato al gioco", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
        if(risultato == 2){
            JOptionPane.showMessageDialog(null, "Siete gia amici", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
        }
    }

    //------------------------------------------          LISTA AMICI          -----------------------------------------
    private void friendList() throws IOException {
        invioAlServer.write(3);        //Comando 5 per la visualizzazione della lista amici
        invioAlServer.writeBytes(username + '\n');

        String tmp= ricevoDalServer.readLine();
        JOptionPane.showMessageDialog(null, "Lista Amici: " + '\n' + tmp);
    }
    //---------------------------------------------          SFIDA          --------------------------------------------
    private void sfida() throws IOException {
        String utenteDaSfidare;
        //Creo una finestra con 2 campi di testo per inserire il nome del documento e il numero delle sezioni
        JPanel panel = new JPanel(new BorderLayout(5, 5));

        JPanel label = new JPanel(new GridLayout(0, 1, 2, 2));
        label.add(new JLabel("Utente da Sfidare", SwingConstants.RIGHT));
        panel.add(label, BorderLayout.WEST);

        JPanel controls = new JPanel(new GridLayout(0, 1, 2, 2));
        JTextField utenteDaSfidareF = new JTextField();
        controls.add(utenteDaSfidareF);
        panel.add(controls, BorderLayout.CENTER);
        //Controllo se viene premuto OK o CANCEL
        int input = JOptionPane.showConfirmDialog(null, panel, "SFIDA", JOptionPane.OK_CANCEL_OPTION);
        if(input == 0){//Caso OK
            utenteDaSfidare = utenteDaSfidareF.getText();
            if (utenteDaSfidare == null || utenteDaSfidare.equals("")){
                JOptionPane.showMessageDialog(null, "ATTENZIONE, nome utente inserito non valido", "ATTENZIONE", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        else return;

        //Invio il comando al server per inoltrare la richiesta di sfida all'utente inserito
        invioAlServer.write(4);
        invioAlServer.writeBytes(username + '\n');
        invioAlServer.writeBytes(utenteDaSfidare + '\n');


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
