package Server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerImplementationRMI extends RemoteServer implements ServerInterfaceRMI {

    private ConcurrentHashMap<String, User> userList;
    private HashMap<String, ArrayList<String>> friendList;
    private File fileUtenti;

    public ServerImplementationRMI(ConcurrentHashMap<String, User> userList, File fileUtenti, HashMap<String, ArrayList<String>> friendList) {
        this.userList = userList;
        this.fileUtenti = fileUtenti;
        this.friendList = friendList;
    }

    @Override
    public boolean registra_utente(String username, String password) {
        //La validita` delle stringhe username e password e` gia stata effettuata

        //Controllo che non siano gia` stati usati da altri utenti
        if(userList.containsKey(username)){
            System.out.println("ERRORE--> USERNAME : " + username + " gia` utilizzato");
            return false;
        }
        //A questo punto le stringhe sono valide e username non e` utilizzato quindi creo il nuovo utente e lo inserisco
        // nella lista utenti
        User user = new User(username, password);
        String punteggio = Integer.toString(user.getPunteggioTotale());

        ArrayList<String> list = new ArrayList<>();
        userList.put(username, user);
        friendList.put(username, list);
        //Aggiungo al file json l'utente che si e registrato
        try {
            JSONParser parser = new JSONParser();
            try{
                //Se il file non contiene nulla creo la struttura json, JSONObject("ListaUtenti", JSONArray[JSONObject1,
                //JSONObject2, ... , JSONObjectN] dove JSONObject(i) contengono username, passoword e punteggio)
                if(fileUtenti.length() == 0){
                    JSONObject object = new JSONObject();
                    JSONArray listaUtentiJ = new JSONArray();
                    JSONArray utenteJ = new JSONArray();
                    JSONObject userJ = new JSONObject();

                    userJ.put("username", username);
                    userJ.put("password", password);
                    userJ.put("punteggio", punteggio);

                    listaUtentiJ.add(userJ);

                    object.put("Lista Utenti", listaUtentiJ);

                    FileWriter fileWriter = new FileWriter(fileUtenti);
                    fileWriter.write(object.toJSONString());
                    fileWriter.close();
                }else{
                    Object object = parser.parse(new FileReader("UTENTI/ListaUtenti.json"));
                    JSONObject objectJ = (JSONObject) object;
                    JSONArray listaUtentiJ = (JSONArray) objectJ.get("Lista Utenti");
                    //ottengo la lista (json array di json obj) gia` scritta nel file e aggiungo il nuovo utente(json obj)
                    //al json array e lo aggiungo ad un nuovo json obj che scrivo nel file

                    JSONObject userJ = new JSONObject();
                    userJ.put("username", username);
                    userJ.put("password", password);
                    userJ.put("punteggio", punteggio);

                    listaUtentiJ.add(userJ);

                    FileWriter fileWriter = new FileWriter(fileUtenti);
                    JSONObject newObjectJ = new JSONObject();
                    newObjectJ.put("Lista Utenti", listaUtentiJ);
                    fileWriter.write(newObjectJ.toJSONString());
                    fileWriter.close();
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("USER --> " + username + " :: correttamente registrato al gioco");
        return true;
    }
}
