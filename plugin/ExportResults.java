/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author omarwagih
 */
public class ExportResults extends JDialog{
    public static JCheckBox exportWithin, exportBetween, exportComplexMatrix, exportSelectedOnly;
    JLabel files, title;
    Container c;
    JPanel mainPanel;
    SideGuiActionListener sgal;
    
    Font titleFont = new Font("Helvetica", Font.BOLD, 16);
    Font textFont = new Font("Helvetica", Font.PLAIN, 14);
    
    Float aln = Component.LEFT_ALIGNMENT;
    
    public ExportResults(SideGuiActionListener sgal){
        this.sgal = sgal;
        
        setModal(true);
        setLocationRelativeTo(null);
        
        c = this.getContentPane();
        BoxLayout layout = new BoxLayout(c, BoxLayout.Y_AXIS);
        c.setLayout(layout);
        
        //Header
        title = new JLabel("Export results");
        title.setFont(titleFont);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        //Content
        exportSelectedOnly = new JCheckBox("Export selected complexes only");
        exportSelectedOnly.setFont(textFont);
        exportSelectedOnly.setToolTipText("Exports selected complex nodes only. By default, all complexes are exported");
        exportSelectedOnly.setAlignmentX(aln);
        
        files = new JLabel("Files to export:");
        files.setFont(textFont);
        files.setAlignmentX(aln);
        
        
        exportWithin = new JCheckBox("Within-enriched complexes");
        exportWithin.setSelected(true);
        exportWithin.setToolTipText("Exports file containing complexes enriched with genetic interactions");
        exportWithin.setFont(textFont);
        exportWithin.setAlignmentX(aln);
        
        exportBetween = new JCheckBox("Between-enriched complexes");
        exportBetween.setSelected(true);
        exportBetween.setToolTipText("Exports file containing complexe pairs enriched with genetic interactions");
        exportBetween.setFont(textFont);
        exportBetween.setAlignmentX(aln);
        
        exportComplexMatrix = new JCheckBox("Complex matrix");
        exportComplexMatrix.setSelected(true);
        exportComplexMatrix.setToolTipText("Exports file containing all-by-all complex matrix, "
                + "showing positive enrichment as '1', negative enrichment as '-1' and no enrichment as '-'");
        exportComplexMatrix.setFont(textFont);
        exportComplexMatrix.setAlignmentX(aln);
        
        JPanel centerPanel = new JPanel();
        BoxLayout box = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
        centerPanel.setLayout(box);
        centerPanel.add(exportSelectedOnly);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(files);
        centerPanel.add(exportWithin);
        centerPanel.add(exportBetween);
        centerPanel.add(exportComplexMatrix);
        centerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
        
        //Footer
        final JDialog dialog = this;
        JButton save = new JButton("Save");
        save.setActionCommand("output");
        save.addActionListener(sgal);
        save.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                dialog.dispose();
            }
        });
        save.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        c.add(new JSeparator());
        c.add(title);
        c.add(new JSeparator());
        c.add(centerPanel);
        c.add(new JSeparator());
        c.add(save);
        
        pack();
        setResizable(false);
        setSize(250, 230);
        setVisible(true);
    }
    
    public static boolean getExportWithin(){
        return exportWithin.isSelected();
    }
    
    public static boolean getExportBetween(){
        return exportBetween.isSelected();
    }
    
    public static boolean getExportComplexMatrix(){
        return exportComplexMatrix.isSelected();
    }
    
    public static boolean getExportExportSelectedOnly(){
        return exportSelectedOnly.isSelected();
    }
}
