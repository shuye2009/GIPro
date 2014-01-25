/**
 * 
 * Maps wizard panels to ID's.  Also keeps track of the current panel descriptor.
 * 
 * Generic for the wizard GUI.
 *
 */

package wGUI;

import java.util.HashMap;
import java.util.Map;

public class WizardModel {

    private Map<Object, WizardPanel> panels;
    
    private Object currentPanelId;

    public WizardModel(){
        panels = new HashMap<Object, WizardPanel>();
        currentPanelId = null;
    }

    public void registerPanel(Object id, WizardPanel panel) {
        panels.put(id, panel);
    }

    public void setCurrentPanel(Object id) {
        currentPanelId = id;
    }

    public WizardPanel getCurrentPanel() {
        return panels.get(currentPanelId);
    }
	
}//end WizardModel