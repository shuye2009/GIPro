/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import edu.stanford.genetics.treeview.BrowserControl;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author omarwagih
 */
public class UserManualLabel extends JLabel {
    public UserManualLabel(){
        setText("<html><font color=BLUE size=2><u>User Manual</u></font><html>");
        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
                try{
                    BrowserControl.getBrowserControl().
                            displayURL("http://www.omarwagih.com/ICToolsUserManual.pdf");
                }catch(Exception e){
                    JOptionPane.showMessageDialog(null, "<html>The user manual can be downloaded from "
                            + "<font color=blue><b><u>http://www.wodaklab.org/ICTools/UserManual.pdf</u></b></font></html>", 
                            "Message", JOptionPane.INFORMATION_MESSAGE);
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {}

            @Override
            public void mouseReleased(MouseEvent me) {}

            //Hand cursor over link
            @Override
            public void mouseEntered(MouseEvent me) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            //Return to default cursor
            @Override
            public void mouseExited(MouseEvent me) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
    }
}
