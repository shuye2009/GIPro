package wGUI;

import java.awt.Component;

import javax.swing.JPanel;

/**
 * 
 * Yanqi Hao
 * 
 * A wizard panel that wraps a Component to give it GUI functionality.
 * 
 * Generic for the wizard GUI.
 *
 */
public abstract class WizardPanel {
	
    protected JPanel targetPanel;
    protected Object panelIdentifier;
    public static Wizard wizard;

    public WizardPanel(Wizard wizard){
        this.wizard = wizard;
    }

    public void dispose(){
        this.wizard.dispose();
    }
    
    public void repaint(){
        this.wizard.repaint();
    }
    
    public void setFocusable(){
        targetPanel.setFocusable(true);
    }
    public void requestFocusInWindow(){
        targetPanel.requestFocusInWindow();
    }

    public final Component getPanelComponent() {
        return targetPanel;
    }

    public final void setPanelComponent(JPanel panel) {
        targetPanel = panel;
    }

    public final Object getPanelDescriptorIdentifier() {
        return panelIdentifier;
    }

    public final void setPanelDescriptorIdentifier(Object id) {
        panelIdentifier = id;
    }
    protected void setBackButtonEnabled(boolean state){
        wizard.setBackButtonEnabled(state);
    }

    protected static void setNextButtonEnabled(boolean state){
        wizard.setNextButtonEnabled(state);
    }

    protected void changeNextButtonText(String text){
        wizard.changeNextButtonText(text);
    }
    
    /******************
     *ABSTRACT METHODS*
     ******************
     */

    abstract public Object getNextPanelDescriptor();

    abstract public Object getBackPanelDescriptor();

    abstract public void aboutToDisplayPanel();

    abstract public void displayingPanel();

    abstract public void aboutToHidePanel();

    //override on last panel
    protected void finishingMove(){
    }
	
}//end WizardPanelDescriptor
