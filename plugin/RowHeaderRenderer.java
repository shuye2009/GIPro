package plugin;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

/*
 *	Use a JTable as a renderer for row numbers of a given main table.
 *  This table must be added to the row header of the scrollpane that
 *  contains the main table.
 */
public class RowHeaderRenderer extends JTable
	implements ChangeListener, PropertyChangeListener
{
	private JTable main;
        static Vector<String> vector;
        static Font headerFont;
        static int width;

	public RowHeaderRenderer(JTable table, Vector<String> vector, Font headerFont, int width)
	{
		main = table;
		main.addPropertyChangeListener( this );
                this.vector = vector;
                this.width = width;
                this.headerFont = headerFont;

		setFocusable( false );
		setAutoCreateColumnsFromModel( false );
		setModel( main.getModel() );
		setSelectionModel( main.getSelectionModel() );

		TableColumn column = new TableColumn();
		column.setHeaderValue(" ");
		addColumn( column );
		column.setCellRenderer(new RowNumberRenderer());

		getColumnModel().getColumn(0).setPreferredWidth(80);
		setPreferredScrollableViewportSize(getPreferredSize());
	}

	@Override
	public void addNotify()
	{
		super.addNotify();

		Component c = getParent();

		//  Keep scrolling of the row table in sync with the main table.

		if (c instanceof JViewport)
		{
			JViewport viewport = (JViewport)c;
			viewport.addChangeListener( this );
		}
	}

	/*
	 *  Delegate method to main table
	 */
	@Override
	public int getRowCount()
	{
		return main.getRowCount();
	}

	@Override
	public int getRowHeight(int row)
	{
		return main.getRowHeight(row);
	}

	/*
	 *  This table does not use any data from the main TableModel,
	 *  so just return a value based on the row parameter.
	 */
	@Override
	public Object getValueAt(int row, int column)
	{
		return Integer.toString(row+1);
	}

	/*
	 *  Don't edit data in the main TableModel by mistake
	 */
	@Override
	public boolean isCellEditable(int row, int column)
	{
		return false;
	}
//
//  Implement the ChangeListener
//
	public void stateChanged(ChangeEvent e)
	{
		//  Keep the scrolling of the row table in sync with main table

		JViewport viewport = (JViewport) e.getSource();
		JScrollPane scrollPane = (JScrollPane)viewport.getParent();
		scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
	}
//
//  Implement the PropertyChangeListener
//
	public void propertyChange(PropertyChangeEvent e)
	{
		//  Keep the row table in sync with the main table

		if ("selectionModel".equals(e.getPropertyName()))
		{
			setSelectionModel( main.getSelectionModel() );
		}

		if ("model".equals(e.getPropertyName()))
		{
			setModel( main.getModel() );
		}
	}

	/*
	 *  Borrow the renderer from JDK1.4.2 table header
	 */
	private static class RowNumberRenderer extends DefaultTableCellRenderer
	{
		public RowNumberRenderer()
		{
			setHorizontalAlignment(JLabel.CENTER);
		}

		public Component getTableCellRendererComponent(
			JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			if (table != null)
			{
				JTableHeader header = table.getTableHeader();

				if (header != null)
				{
					setForeground(header.getForeground());
                                        setBackground(header.getBackground());
					setFont(headerFont);
				}
			}

			if (isSelected)
			{
				setFont( headerFont );
			}

			setValue((value == null) ? "" : value.toString());
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));

			return this;
		}
                
                @Override
                public void setValue(Object o){
                    Integer i = Integer.parseInt(o.toString());
                    setText(vector.get(i-1).toString());
                }
                
                
                
                
	}
}
