package plugin;

/**
 * findByName and find2 found at:
 * http://www.exampledepot.com/egs/javax.swing.tree/FindNode.html
 * 
 */

import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class External {
    
    /**
     * Finds the path in tree as specified by the array of names. 
     * The names array is a sequence of names where names[0] is the root and 
     * names[i] is a child of names[i-1].
     * Comparison is done using String.equals(). Returns null if not found.
     * @param tree JTree
     * @param names Array of strings
     * @return TreePath
     */
    public static TreePath findByName(JTree tree, String[] names) {
            TreeNode root = (TreeNode)tree.getModel().getRoot();
            return find2(tree, new TreePath(root), names, 0, true);
    }
    
    /**
     * Helper function used by findByName()
     * @param tree
     * @param parent
     * @param nodes
     * @param depth
     * @param byName
     * @return 
     */
    private static TreePath find2(JTree tree, TreePath parent, Object[] nodes, 
            int depth, boolean byName) {
        
            TreeNode node = (TreeNode)parent.getLastPathComponent();
            Object o = node;
            
            // If by name, convert node to a string
            if (byName) {
                o = o.toString();
            }

            // If equal, go down the branch
            if (o.equals(nodes[depth])) {
                // If at end, return match
                if (depth == nodes.length-1) {
                        return parent;
                }
                
                // Traverse children
                if (node.getChildCount() >= 0) {
                    for (@SuppressWarnings("rawtypes")
                    Enumeration e = node.children(); e.hasMoreElements(); ) {
                        TreeNode n = (TreeNode)e.nextElement();
                        TreePath path = parent.pathByAddingChild(n);
                        TreePath result = find2(tree, path, nodes, depth+1, byName);
                        // Found a match
                        if (result != null) {
                                return result;
                        }
                    }
                }
            }
            // No match at this branch
            return null;
	}
}
