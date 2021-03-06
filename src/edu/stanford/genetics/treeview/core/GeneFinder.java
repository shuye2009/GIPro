/*
 * Created on Aug 1, 2005
 *
 * Copyright Alok Saldnaha, all rights reserved.
 */
package edu.stanford.genetics.treeview.core;

import edu.stanford.genetics.treeview.HeaderInfo;
import edu.stanford.genetics.treeview.TreeSelectionI;
import edu.stanford.genetics.treeview.ViewFrame;


/**
 * @author aloksaldanha
 *
 */
public class GeneFinder extends HeaderFinder {

	/**
	 * @param f
	 * @param hI
	 * @param geneSelection
	 */
	public GeneFinder(ViewFrame f, HeaderInfo hI, TreeSelectionI geneSelection) {
		super(f, hI, geneSelection, "Search Gene Text for Substring");
	}
	/* (non-Javadoc)
	 * @see edu.stanford.genetics.treeview.HeaderFinder#scrollToIndex(int)
	 */
	public void scrollToIndex(int i) {
		viewFrame.scrollToGene(i);
	}

	protected void showSubDataModel() {
		seekAll();
		viewFrame.showSubDataModel(geneSelection.getSelectedIndexes(),
				search_text.getText() +" matches in " + viewFrame.getDataModel().getSource(),
				search_text.getText() +" matches in " + viewFrame.getDataModel().getName()
				);
	}

}
