package Server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ServerImplementationRMI extends RemoteServer implements ServerInterfaceRMI {

    private ConcurrentHashMap<String, User> userList;
    private File fileUtenti;

    public ServerImplementationRMI(ConcurrentHashMap<String, User> userList, File fileUtenti) {
        this.userList = userList;
        this.fileUtenti = fileUtenti;
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
        //Aggiungo al file json l'utente che si e registrato
        try {
            JSONParser parser = new JSONParser();
            try{
                //Se il file non contiene nulla creo la struttura json, JSONObject("ListaUtenti", JSONArray[JSONObject1,
                //JSONObject2, ... , JSONObjectN] dove JSONObject(i) contengono (username, passoword))
                if(fileUtenti.length() == 0){
                    JSONObject obj1 = new JSONObject();
                    JSONObject obj2 = new JSONObject();
                    JSONArray jsonArray = new JSONArray();
                    obj2.put(username, password);
                    jsonArray.add(obj2);
                    obj1.put("ListaUtenti", jsonArray);
                    FileWriter fileWriter = new FileWriter(fileUtenti);
                    fileWriter.write(obj1.toJSONString());
                    fileWriter.close();
                }else{
                    Object object = parser.parse(new FileReader("LISTA_UTENTI/ListaUtenti.json"));
                    JSONObject jsonObject1 = (JSONObject) object;
                    JSONArray jsonArray = (JSONArray) jsonObject1.get("ListaUtenti");
                    //ottengo la lista (json array di json obj) gia` scritta nel file e aggiungo il nuovo utente(json obj)
                    //al json array e lo aggiungo ad un nuovo json obj che scrivo nel file

                    JSONObject jsonObject2 = new JSONObject();
                    jsonObject2.put(username, password);
                    jsonArray.add(jsonObject2);

                    FileWriter fileWriter = new FileWriter(fileUtenti);
                    JSONObject jsonObject3 = new JSONObject();
                    jsonObject3.put("ListaUtenti", jsonArray);
                    fileWriter.write(jsonObject3.toJSONString());
                    fileWriter.close();
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }






        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("USER--> " + username + " correttamente registrato al gioco");
        return true;
    }
}
