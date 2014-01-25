/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;

/**
 *
 * @author omarwagih
 */
public class LinkLabel extends JLabel {
    String name;
    public LinkLabel(String name){
        setText("<html><font color=BLUE size=2><u>"+name+"</u></font><html>");
        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
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
