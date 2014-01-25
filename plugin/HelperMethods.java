package plugin;

import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class HelperMethods {
    
    public static File showExportDialog(String title){
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle(title);
        jfc.setMultiSelectionEnabled(false);
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        int input = jfc.showSaveDialog(null);
        if(input == JFileChooser.CANCEL_OPTION) return null;
        
        return jfc.getSelectedFile();
    }
    
    public static void showSaveSuccess(List<String> filePaths){
        int numFiles = filePaths.size();
        if(numFiles < 1) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<span style=\"font-family:Helvetica;font-size:13px;font-weight:bold;color:009900;\">Successfully wrote "+numFiles+" files as:</span><br>");
        
        for(String s: filePaths){
            sb.append("<span style=\"font-family:Helvetica;font-size:11px;font-weight:bold;\">"+s+"</span><br>");
        }
        sb.append("</html>");
        
        JOptionPane.showMessageDialog(null, sb.toString(), "File(s) saved", JOptionPane.INFORMATION_MESSAGE);
    }
    
     /**
     * Write contents of a list to an output file
     * @param list LIST to write
     * @param outputFile output FILE
     * @return 1 if successful, -1 if unsuccessful
     */
    public static int writeListToFile(List<String> list, String outputFile){
        try {
            File f = new File(outputFile);
            
            //Ignore for now
            /*
            if(f.exists()){
                int ret = JOptionPane.showConfirmDialog(null , f.getName()+
                        " already exists in the chosen directory. Overwrite?",
                        "Error", JOptionPane.YES_NO_OPTION);
                if(ret == JOptionPane.NO_OPTION) return -1;
                //else go on and save
            }
             */
            BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
            for(String s: list)
                out.write(s + "\n");
            out.close();
            return 1;
        } catch (Exception e) {
            System.err.println("EXCEPTION: "+ e.getMessage());
            JOptionPane.showMessageDialog(null, "Error saving file: "+e.getMessage(), 
                    "Error,", JOptionPane.ERROR_MESSAGE);
            return -1;
        }

    }
    
    public static Set<String> fileToSet(File file, boolean toLowerCase, boolean trim){
        try {
            Set<String> set = new HashSet<String>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while((line = br.readLine()) != null){
                if(toLowerCase) set.add(line.toLowerCase());
                else if(trim) set.add(line.trim());
                else set.add(line);
            }
            if(set.isEmpty())
                return null;
            return set;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static List<String> fileToList(File file, boolean toLowerCase, boolean trim){
        try {
            List<String> list = new ArrayList<String>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while((line = br.readLine()) != null){
                if(toLowerCase) list.add(line.toLowerCase());
                else if(trim) list.add(line.trim());
                else list.add(line);
            }
            return list;
        } catch (Exception e) {
            return null;
        }
    }
    
    public static int getScreenWidth(){
        Toolkit tk = Toolkit.getDefaultToolkit();
        return ((int) tk.getScreenSize().getWidth());
    }
    
    public static int getScreenHeight(){
        Toolkit tk = Toolkit.getDefaultToolkit();  
        return ((int) tk.getScreenSize().getHeight());
    }
    
    /**
     * Gets OS name
     * @return "windows" , "mac" or "linux"
     */
    public static String getOsName() {
        String os = "";
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                os = "windows";
            } else if (System.getProperty("os.name").toLowerCase().contains("linux")) {
                os = "linux";
            } else if (System.getProperty("os.name").toLowerCase().contains("mac")) {
                os = "mac";
        }

        return os;
    }
    /**
     * Sorts a list of strings that represent integers (based on their integer 
     * order, as opposed to the default "alphabetical" order
     * @param list List of strings that represent integers to sort
     */
    public static void NumericalSort(List<String> list){
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                try{
                    return Integer.parseInt(s1) - Integer.parseInt(s2);
                }
                catch(NumberFormatException e){
                    return s1.compareTo(s2);
                }
            }
        });
    }
	
    public static void InteractionSort(List<String> list){
        Collections.sort(list, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                int s1_1 = Integer.parseInt(s1.split("//")[0]);
                int s1_2 = Integer.parseInt(s1.split("//")[1]);

                int s2_1 = Integer.parseInt(s2.split("//")[0]);
                int s2_2 = Integer.parseInt(s2.split("//")[1]);
                try{
                    if (s1_1 != s2_1){
                        return s1_1 - s2_1;
                    }
                    return s1_2 - s2_2;
                }
                catch(NumberFormatException e){
                    return s1.compareTo(s2);
                }
            }
        });
    }
	
    /**
     * Sorts a list of genes alphabetically based on their names
     * @param list List of genes to sort alphabetically
     */
    public static void GeneSort(List<Gene> list){		
        Collections.sort(list, new Comparator<Gene>() {
            @Override
            public int compare(Gene g1, Gene g2) {
                String s1 = g1.getGeneName();
                String s2 = g2.getGeneName();
                return s1.compareTo(s2);
            }
        });
    }
	
    /**
     * Sorts a list of complexes numerically based on their names
     * @param list of complexes
     */
    public static void ComplexSort(List<Complex> list){		
        Collections.sort(list, new Comparator<Complex>() {
            public int compare(Complex c1, Complex c2) {
                String thisS = c1.getName();
                String compareS = c2.getName();
                try{
                    return Integer.parseInt(thisS) - Integer.parseInt(compareS);
                } 
                catch(NumberFormatException e){
                    return thisS.compareTo(compareS);
                }
            }
        });
    }
	
    /**
     * Returns the string, describing the significance, associated with each significance letter
     * @param significance String code of significance 
     * @return "POSITIVE" if significantly positive, "NEGATIVE" if significantly 
     * negative, "BOTH" if positively and negatively significant or "NONE" if 
     * not significant
     */
    public static String getSignificanceString(String significance){
        if (significance.equals(Config.nodePosSignificanceKey) ||
                        significance.equals(Config.edgePosSignificanceKey)){
            return "POSITIVE";
        }
        else if (significance.equals(Config.nodeNegSignificanceKey) ||
                        significance.equals(Config.edgeNegSignificanceKey)){
            return "NEGATIVE";
        }
        else if (significance.equals(Config.nodeBothSignificanceKey) ||
                        significance.equals(Config.edgeBothSignificanceKey)){
            return "BOTH";
        }
        return "NONE";
    }

}
