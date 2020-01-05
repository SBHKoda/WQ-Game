package Client;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
    private JButton invitaB;
    private JButton creaTdocB;
    private JButton showSectionB;
    private JButton showDocumentB;
    private JButton editB;
    private JButton listB;
    private JButton endEditB;
    private static JLabel statusLabel;

    private boolean onlineStatus = false;

    //Per attivare il servizio calback per le notifiche
    private ServerInterfaceRMI stub;
    private ArrayBlockingQueue<String> msgList;
    //private NotifyReceiver receiver;

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

        posComponent();
        createActionListener();

        add(usernameField);
        add(passwordField);
        add(statusLabel);
        add(signUpB);
        add(loginB);
        add(logoutB);
    }

    private static void posComponent() {
        statusLabel.setBounds(350, 10, 90, 20);

        usernameField.setBounds(10, 10, 150, 20);
        usernameField.setColumns(10);
        passwordField.setBounds(170, 10, 150, 20);

        signUpB.setBounds(640, 10, 140, 30);
        loginB.setBounds(640, 60, 140, 30);
        logoutB.setBounds(640,110,140,30);
    }

    private void createActionListener() {
        loginB.addActionListener(ae -> login());
        signUpB.addActionListener(ae -> signIn());
        logoutB.addActionListener(ae -> logout());
    }
    //--------------------------------------    SIGN IN   --------------------------------------
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

    //--------------------------------------    LOGIN   --------------------------------------
    private static void login() {
        try {
            invioAlServer.write(0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void logout() {
    }
}
