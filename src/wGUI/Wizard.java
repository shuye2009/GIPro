package wGUI;

import cytoscape.Cytoscape;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;

/**
 * 
 * The shell of the wizard containing the outer frame box as well as the next, back, and 
 * cancel buttons.
 * 
 * Generic for the wizard GUI.
 *
 */
public class Wizard {
    private WizardModel wizardModel;
    private WizardController wizardController;

    private JFrame wizard;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    private JButton backButton;
    private JButton nextButton;
    private JButton cancelButton;
    private JButton aboutButton;
    
    Toolkit tk = Toolkit.getDefaultToolkit();  
        Dimension dim = tk.getScreenSize();  
        int screenX = ((int) tk.getScreenSize().getWidth());
        int screenY = ((int) tk.getScreenSize().getHeight());
        
    /**
     * Wizard constructor
     * @param title Title of Wizard
     */
    public Wizard(String title) {
            wizardModel = new WizardModel();
            wizard = new JFrame(title);
            wizard.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            wizard.toFront();
            wizard.setSize(750, 720);
            wizard.setLocation(screenX/4, screenY/4);
            wizard.setResizable(false);
            wizard.setAlwaysOnTop(false);
            wizardController = new WizardController(this);
            initComponents();
    }
    
    /**
     * Dispose Wizard
     */
    public void dispose() {
        wizard.dispose();
    }
    
    public void repaint(){
        wizard.repaint();
    }

    public Object returnNextId(){
        return wizardModel.getCurrentPanel().getNextPanelDescriptor();
    }

    public Object returnPreviousId(){
        return wizardModel.getCurrentPanel().getBackPanelDescriptor();
    }

    public void registerWizardPanel(Object id, WizardPanel panel) {
        cardPanel.add(panel.getPanelComponent(), id); 
        wizardModel.registerPanel(id, panel);
    }

    public void setBackButtonEnabled(boolean b) {
        backButton.setEnabled(b);
        backButton.setVisible(b);
    }

    public void setNextButtonEnabled(boolean b) {
        nextButton.setEnabled(b);
    }

    public void reset(Object firstPanelID){
        this.changeNextButtonText("Next");
        this.setCurrentPanel(firstPanelID);
    }

    public void setCurrentPanel(Object id) {
        WizardPanel oldPanelDescriptor = wizardModel.getCurrentPanel();

        if (oldPanelDescriptor != null) {
            oldPanelDescriptor.aboutToHidePanel();
            if(id.equals("FINISH")){
                oldPanelDescriptor.finishingMove();
                return;
            }
        }

        wizardModel.setCurrentPanel(id);
        wizardModel.getCurrentPanel().aboutToDisplayPanel();
        cardLayout.show(cardPanel, id.toString());
        wizardModel.getCurrentPanel().displayingPanel();
    }
    
    public void changeNextButtonText(String text) {
        this.nextButton.setText(text);
    }
    
    public JButton getNextButton(){
        return nextButton;
    }
    /**
     * Initialize components
     */
    private void initComponents() {			
        JPanel buttonPanel = new JPanel();
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        //Box aboutBox = new Box(BoxLayout.X_AXIS);

        cardPanel = new JPanel();
        cardPanel.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));

        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);
        backButton = new JButton("Back");
        nextButton = new JButton("Next");
        cancelButton = new JButton("Cancel");
        aboutButton = new JButton("About");

        backButton.addActionListener(wizardController);
        nextButton.addActionListener(wizardController);
        cancelButton.addActionListener(wizardController);
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new AboutDialog(Cytoscape.getDesktop(), true, wizard);
            }
        });

        backButton.setActionCommand("back");
        nextButton.setActionCommand("next");
        cancelButton.setActionCommand("cancel");

        buttonPanel.setLayout(new BorderLayout());
        buttonPanel.add(new JSeparator(), BorderLayout.NORTH);

        //About panel
//        aboutBox.setBorder(new EmptyBorder(new Insets(3, 3, 3, 3)));
//        aboutBox.add(aboutButton, BorderLayout.WEST);
//        
//        URL url = getClass().getClassLoader().getResource("images/help_icon.gif"); 
//        ImageIcon logoImage = new ImageIcon(url);
//        JLabel helpLabel = new JLabel("<html><font size=2><b>Need help? </b>Hover over items for more information or get the </font></html>");
//        helpLabel.setIcon(logoImage);
//        aboutBox.add(Box.createHorizontalStrut(30), BorderLayout.CENTER);
//        aboutBox.add(helpLabel, BorderLayout.CENTER);
//        aboutBox.add(new UserManualLabel(), BorderLayout.CENTER);
        
        //Button load files/cancel
        //buttonBox.setBorder(new EmptyBorder(new Insets(3, 3, 3, 3)));
        buttonBox.add(backButton);
        buttonBox.add(nextButton);
        buttonBox.add(cancelButton);
        
        
        //userManualPanel.add(helpLabel);
        //userManualPanel.add(new UserManualLabel());
        
        //Add all
        JPanel buttonPanel2 = new JPanel();
        buttonPanel2.add(buttonBox);
        buttonPanel.add(buttonPanel2);
//        buttonPanel.add(aboutBox, BorderLayout.WEST);
        //buttonPanel.add(userManualPanel, BorderLayout.PAGE_END);
        wizard.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        wizard.getContentPane().add(cardPanel, BorderLayout.CENTER);
        wizard.setVisible(true);
        wizard.toFront();
    }
    
    public JFrame getWizardFrame(){
        return wizard;
    }

    
	
}//end Wizard