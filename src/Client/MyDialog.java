package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class MyDialog extends JDialog implements ActionListener, PropertyChangeListener {
    private String typedText = null;
    private JTextField textField;

    private JOptionPane optionPane;

    private String btnString1 = "Invia";
    private String btnString2 = "Cancel";

    private ClientUI parent;

    public MyDialog(Frame aFrame, String domanda, ClientUI parent){
        super(aFrame, true);
        this.parent = parent;

        setTitle("TEST TITLE");
        textField = new JTextField(15);

        //Create an array of the text and components to be displayed.
        Object[] array = {domanda, textField};

        //Create an array specifying the number of dialog buttons
        //and their text.
        Object[] options = {btnString1, btnString2};

        //Create the JOptionPane.
        optionPane = new JOptionPane(array,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null,
                options,
                options[0]);

        //Make this dialog display it.
        setContentPane(optionPane);
        setSize(400, 250);

        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                /*
                 * Instead of directly closing the window,
                 * we're going to change the JOptionPane's
                 * value property.
                 */
                optionPane.setValue(JOptionPane.CLOSED_OPTION);
            }
        });
        //Ensure the text field always gets the first focus.
        addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                textField.requestFocusInWindow();
            }
        });

        //Register an event handler that puts the text into the option pane.
        textField.addActionListener(this);

        //Register an event handler that reacts to option pane state changes.
        optionPane.addPropertyChangeListener(this);
    }
    /** This method handles events for the text field. */
    @Override
    public void actionPerformed(ActionEvent e) {
        optionPane.setValue(btnString1);
    }
    /** This method reacts to state changes in the option pane. */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();
       if (isVisible()
                && (e.getSource() == optionPane)
                && (JOptionPane.VALUE_PROPERTY.equals(prop) ||
                JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
            Object value = optionPane.getValue();

            if (value == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }
/*
            //Reset the JOptionPane's value.
            //If you don't do this, then if the user
            //presses the same button next time, no
            //property change event will be fired.
            optionPane.setValue(
                    JOptionPane.UNINITIALIZED_VALUE);

            if (btnString1.equals(value)) {
                typedText = textField.getText();
                //String ucText = typedText.toUpperCase();
                if (risposta.equals(ucText)) {
                    //we're done; clear and dismiss the dialog
                    clearAndHide();
                }
            } else { //user closed dialog or clicked cancel

                typedText = null;
                clearAndHide();
            }*/
        }
    }
    /** This method clears the dialog and hides it. */
    public void clearAndHide() {
        textField.setText(null);
        setVisible(false);
    }
}
