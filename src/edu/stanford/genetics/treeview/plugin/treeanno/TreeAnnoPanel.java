/*
 * Created on Mar 6, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.plugin.treeanno;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.stanford.genetics.treeview.ConfigNode;
import edu.stanford.genetics.treeview.DataModel;
import edu.stanford.genetics.treeview.DummyConfigNode;
import edu.stanford.genetics.treeview.ExportException;
import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.LogBuffer;
import edu.stanford.genetics.treeview.MainPanel;
import edu.stanford.genetics.treeview.MainProgramArgs;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.TreeviewMenuBarI;
import edu.stanford.genetics.treeview.ViewFrame;

/**
 * 
 * This is the main panel for the tree annotation editor
 * This same class will be used for editing the gene and array trees.
 *
 */
public class TreeAnnoPanel extends JPanel implements MainPanel {

	private ViewFrame viewFrame;
	private DataModel dataModel;
	private NamedNodeView namedNodeView;
	private SingleNodeView singleView;
	private TreeSelectionI selection;
	private HeaderInfo nodeInfo;
	private TableNodeView tableNodeView;

	/**
	 *  Constructor for the TreeAnno object
	 *  Note this will reuse any existing TreeAnno nodes in the documentconfig
	 *
	 * @param  tVModel   model this DendroView is to represent
	 * @param  vFrame  parent ViewFrame of DendroView
	 * @param  type   type of tree annotation to edit, either GENE_TREE or ARRAY_TREE
	 */
	public TreeAnnoPanel(DataModel tVModel, ViewFrame vFrame, int type) {
		super();
		viewFrame = vFrame;
		dataModel = tVModel;
		if (dataModel.getDocumentConfig() != null ) {
		  bindConfig(dataModel.getDocumentConfig().fetchOrCreate("TreeAnno"));
		} else {
		  bindConfig(new DummyConfigNode("TreeAnno"));
		}

		// node info must be set before we set up views
		if (type == GENE_TREE) {
			nodeInfo = dataModel.getGtrHeaderInfo();
		} else {
			nodeInfo = dataModel.getAtrHeaderInfo();
		}
		setupViews();
		// selection must be set after we set up views
		if (type == GENE_TREE) {
			setSelection(viewFrame.getGeneSelection());
		} else {
			setSelection(viewFrame.getArraySelection());
		}
	}

	public TreeAnnoPanel(ViewFrame vFrame, ConfigNode root) {
		super();
		viewFrame = vFrame;
		dataModel = vFrame.getDataModel();
		bindConfig(root);
		
		// node info must be set before we set up views
		if (type == GENE_TREE) {
			nodeInfo = dataModel.getGtrHeaderInfo();
		} else {
			nodeInfo = dataModel.getAtrHeaderInfo();
		}
		setupViews();
		// selection must be set after we set up views
		if (type == GENE_TREE) {
			setSelection(viewFrame.getGeneSelection());
		} else {
			setSelection(viewFrame.getArraySelection());
		}
	}
	
	/**
	 * 
	 */
	private void setupViews() {
		namedNodeView = new NamedNodeView(nodeInfo);
		namedNodeView.setViewFrame(viewFrame);
		
		singleView = new SingleNodeView(nodeInfo);
		singleView.setViewFrame(viewFrame);
		
		
		tableNodeView = new TableNodeView(nodeInfo);
		tableNodeView.setViewFrame(viewFrame);
		
		doSingleLayout();
	}
	
	private void doSingleLayout() {
		JPanel left = new JPanel();
		left.setLayout(new BorderLayout());
		left.add(namedNodeView);
		JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				singleView,
				tableNodeView);
		right.setResizeWeight(0.5);
		right.setOneTouchExpandable(true);
		JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				left,
				right);
		main.setResizeWeight(0.5);
		main.setOneTouchExpandable(true);
		setLayout(new BorderLayout());
		add(main, BorderLayout.CENTER);
	}
	private int type;
	public int getType() {
		return type;
	}
	/** 
	 * this shouldn't be changed once object is constructed.
	 * 
	 * @param type
	 */
	private void setType(int type) {
		this.type = type;
		if (root != null) root.setAttribute("tree_type", type, DEFAULT_TYPE);
	}
	public TreeSelectionI getSelection() {
		return selection;
	}
	public void setSelection(TreeSelectionI sel) {
		if (selection != null) {
//			selection.deleteObserver(this);	
		}
		selection = sel;
//		selection.addObserver(this);
		namedNodeView.setSelection(sel);
		singleView.setSelection(sel);
		tableNodeView.setSelection(sel);
	}

	public static final int GENE_TREE = 0;
	public static final int ARRAY_TREE = 1;
	public static final int DEFAULT_TYPE = 0;
	
	private ConfigNode root;
	
	public void syncConfig() {
		// nothing to do, since type is static.
	}

	public ConfigNode getConfigNode() {
		return root;
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.MainPanel#populateSettingsMenu(java.awt.Menu)
	 */
	public void populateSettingsMenu(TreeviewMenuBarI menu) {
   // no settings
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.MainPanel#populateAnalysisMenu(java.awt.Menu)
	 */
	public void populateAnalysisMenu(TreeviewMenuBarI menu) {
		// no analysis
	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.MainPanel#populateExportMenu(java.awt.Menu)
	 */
	public void populateExportMenu(TreeviewMenuBarI menu) {
		// no export

	}

	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.MainPanel#scrollToIndex(int)
	 */
	public void scrollToGene(int i) {
		LogBuffer.println("TreeAnnoPanel.scrollToGene not implemented");
	}
	public void scrollToArray(int i) {
		LogBuffer.println("TreeAnnoPanel.scrollToArray not implemented");
	}
	public void bindConfig(ConfigNode configNode) {
		root = configNode;
		setType(root.getAttribute("tree_type", DEFAULT_TYPE));
	}

	private static ImageIcon treeviewIcon = null;
	/**
	 * icon for display in tabbed panel
	 */
	public ImageIcon getIcon() {
		if (treeviewIcon == null)
			treeviewIcon = new ImageIcon("images/treeview.gif", "TreeView Icon");
		return treeviewIcon;
	}

	public void export(MainProgramArgs args) throws ExportException {
		throw new ExportException("Export not implemented for plugin " + getName());
	}

}
