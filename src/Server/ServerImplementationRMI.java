package Server;

import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

public class ServerImplementationRMI extends RemoteServer implements ServerInterfaceRMI {

    private ConcurrentHashMap<String, User> userList;
    public ServerImplementationRMI(ConcurrentHashMap<String, User> userList) {
        this.userList = userList;
    }

    @Override
    public boolean registra_utente(String username, String password) {
        //Controllo base della validita` delle stringhe username e password
        if(username == null || password == null){
            System.out.println("ERRORE--> USERNAME o PASSWORD non possono essere NULL");
            return false;
        }
        if(username == "" || password == ""){
            System.out.println("ERRORE--> USERNAME o PASSWORD  non possono essere vuoti");
            return false;
        }
        //Controllo che non siano gia` stati usati da altri utenti
        if(userList.containsKey(username)){
            System.out.println("ERRORE--> USERNAME : " + username + " gia` utilizzato");
            return false;
        }
        //A questo punto le stringhe sono valide e username non e` utilizzato quindi creo il nuovo utente e lo inserisco
        // nella lista utenti
        User user = new User(username, password);
        userList.put(username, user);
        System.out.println("USER--> " + username + " correttamente registrato al gioco");
        return true;
    }
}
