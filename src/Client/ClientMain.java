package Client;
import java.awt.*;

public class ClientMain {
    public static void main(String[] args){
        ClientUI clientWindow = null;
        clientWindow = new ClientUI();
        clientWindow.getContentPane().setBackground(Color.DARK_GRAY);
        clientWindow.setLocation(100, 100);
        clientWindow.setTitle("Word Quizzle");
        clientWindow.setSize(370, 420);
        clientWindow.setVisible(true);
    }




}
