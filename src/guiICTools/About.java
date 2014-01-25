package guiICTools;


import java.awt.Container;
import java.awt.Dimension;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author omarwagih
 */
public class About extends JDialog {
    private final String VERSION = "1.0";
    
    public About(){
        setModal(true);
        Container c = this.getContentPane();
        JLabel info = new JLabel();
        info.setText("<html>"
                +"<CENTER><font size=\"6\" color=\"GREEN\">ABOUT</font></CENTER>"
                +"<b><font size=\"4\"><CENTER>GIPro Plugin for Cytoscape<br>"
                +"Version: "+VERSION+"</CENTER></font><hr></hr>"
                +"This tool uses physical and genetic interaction data to identify the functional relationships " +
                "between genes. There are two parts to the analysis, and parameters should be provided for each. " +
                "In the first part, relations values are filtered based on calculated or provided cutoff values. " +
                "The enrichment analysis is then run on the data, using either the Fisher Exact Test or simulations. " +
                "You can roll over any of the fields for more information."
                +"<hr></hr><br>Written by:......."
                +"</html>");
        info.setPreferredSize(new Dimension(400, 250));
        c.add(info);
        pack();
        //setSize(400,300);
        setTitle("About ICTools");
        setLocationRelativeTo(null);
        //setResizable(false);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setVisible(true);
    }
}
