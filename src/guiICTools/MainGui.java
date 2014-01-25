// Used to initialize the wizard for the plugin

package guiICTools;

import javax.swing.JOptionPane;
import wGUI.Wizard;
import wGUI.WizardPanel;

public class MainGui {
    private static Wizard wizard;
    private static WizardPanel input;
    
    /**
     * Creates and shows main ICTools window using an instance of Input.java
     */
    public static void createAndShowGUI() {
        
        // Create and set up the content pane.
        wizard = new Wizard("GIPro");
        input = new Input(wizard);
        wizard.registerWizardPanel("Input", input);
        wizard.setCurrentPanel(input.getPanelDescriptorIdentifier());
        
    }
	
    public static void reset(){
        if(wizard != null){
            wizard.reset("File Input");
        }
    }
    
    public static void disposeWizard(){
        if(wizard != null){
            wizard.dispose();
        }
    }
}//end MainGui class