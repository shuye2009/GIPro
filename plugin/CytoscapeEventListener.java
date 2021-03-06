/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CytoscapeDesktop;
import cytoscape.view.cytopanels.CytoPanelImp;
import giny.view.GraphViewChangeEvent;
import giny.view.GraphViewChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Used for disabling buttons for functions not supposed to be used in subnetworks
 * / Reenabling 
 * @author omarwagih
 */
public class CytoscapeEventListener implements PropertyChangeListener{
        RootNetwork rn;
        
        public CytoscapeEventListener(RootNetwork rn){
            this.rn = rn;
            Cytoscape.getDesktop().getSwingPropertyChangeSupport().
                    addPropertyChangeListener(this);
        }

    @Override
    public void propertyChange(PropertyChangeEvent pce) {
        /*Network focus change: disable/enable buttons depending on if network is subnetwork or no*/
        if(pce.getPropertyName().equals(CytoscapeDesktop.NETWORK_VIEW_FOCUSED)){
            try{
                CyAttributes networkAtr = Cytoscape.getNetworkAttributes();
                String str = networkAtr.getAttribute(Cytoscape.getCurrentNetwork().getIdentifier(), "ICTools_NetworkType").toString();
                if(str.equalsIgnoreCase("Complex")){
                    rn.enablePanelButtons();
                }
                else{
                    rn.disablePanelButtons();
                }
            /*When subnetwork is first created, before its network attribute is set, a null pointer exception will be thrown 
                 * so we catch it knowing its the subnetwork*/
            }catch(NullPointerException npe){
                rn.disablePanelButtons();
            }
        }
        
        //On new session or close 
        if(pce.getPropertyName().equals(CytoscapeDesktop.NETWORK_VIEW_DESTROYED)){
            //Only if all networks are gone i.e. new session is clicked
            if(! Cytoscape.getNetworkSet().isEmpty()) return;
            
            CytoPanelImp panel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
            if(panel.indexOfComponent("ICTools") != -1){
                panel.remove(panel.indexOfComponent("ICTools"));
            }
            panel.setSelectedIndex(0);
            
            rn.clearAll();
            Initialization.setRootNetwork(null);
        }
    }
}
