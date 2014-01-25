/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import cytoscape.Cytoscape;
import guiICTools.UserManualPanel;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 *
 * @author omarwagih
 */
public class UserManualLabel extends JLabel {
    JFrame frame;
    public UserManualLabel(JFrame frame){
        this.frame = frame;
        setText("<html><font color=BLUE size=2><u>User Manual</u></font><html>");
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                UserManualPanel ump = new UserManualPanel(Cytoscape.getDesktop(), true, UserManualLabel.this.frame);
                ump.setVisible(true);
//                try{
//                    BrowserControl.getBrowserControl().
//                            displayURL("http://wodaklab.org/webstarts/gipro/GIPro_UserManual_1.0.pdf");
//                }catch(Exception e){
//                    JOptionPane.showMessageDialog(null, "<html>The user manual can be downloaded from "
//                            + "<font color=blue><b><u>http://wodaklab.org/webstarts/gipro/GIPro_UserManual_1.0.pdf"
//                            + "</u></b></font></html>", 
//                            "Message", JOptionPane.INFORMATION_MESSAGE);
//                }
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
