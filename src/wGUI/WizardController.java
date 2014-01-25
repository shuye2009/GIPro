/**
 * 
 * Controller for next, back, and cancel buttons of a Wizard.
 * 
 * Generic for the wizard GUI.
 * 
 */

package wGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class WizardController implements ActionListener{
	
    private Wizard wizard;
    
    /**
     * WizardController constructor
     * @param wizard 
     */
    public WizardController(Wizard wizard) {
        this.wizard = wizard;
    }
    
    /**
     * On action event
     * @param e 
     */
    public void actionPerformed(ActionEvent e) {
    	if("next".equals(e.getActionCommand())){
            wizard.setCurrentPanel(wizard.returnNextId());
    	}
    	else if("back".equals(e.getActionCommand())){
            wizard.setCurrentPanel(wizard.returnPreviousId());
    	}
    	else if("cancel".equals(e.getActionCommand())){
            wizard.dispose();
    	}
    }//end actionPerformed
}//end WizardController