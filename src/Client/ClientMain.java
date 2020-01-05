package Client;
import javax.swing.*;
import java.awt.*;

public class ClientMain {
    public static void main(String[] args){
        ClientUI clientWindow = null;
        clientWindow = new ClientUI();
        if(clientWindow != null){
            clientWindow.getContentPane().setBackground(Color.DARK_GRAY);
            clientWindow.setLocation(200, 200);
            clientWindow.setTitle("Word Quizzle");
            clientWindow.setSize(800, 600);
            clientWindow.setVisible(true);
        }
    }




}
