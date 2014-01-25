//package ch.rakudave.suggest;
//
//import java.awt.BorderLayout;
//import java.awt.Dimension;
//import java.awt.GridLayout;
//import java.awt.Point;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
//import java.util.Vector;
//
//import javax.swing.JButton;
//import javax.swing.JFrame;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.JTextField;
//import javax.swing.border.EmptyBorder;
//
//
//public class SuggestDemo {
//	private static Vector<String> sample;
//	
//	public static void main(String[] args) {
//		sample = new Vector<String>();
//			for (int i = 0; i < 10; i++) {
//				sample.add("asdf"+i);
//				sample.add("hallo"+i);
//				sample.add("test"+i);
//				sample.add("abb"+i);
//				sample.add("suggest"+i);
//				sample.add("field"+i);
//			}
//		new SuggestDemo();
//	}
//	
//	public SuggestDemo() {
//
//		JFrame f = new JFrame("JSuggestField Test");
//			f.setPreferredSize(new Dimension(300, 130));
//			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//			f.setLocation(500, 500);
//		JPanel container = new JPanel(new GridLayout(4, 1, 5, 5));
//			container.setBorder(new EmptyBorder(5, 5, 5, 5));
//		container.add(new JLabel("JTextField:"));
//		JTextField t = new JTextField();
//		container.add(t);
//		container.add(new JLabel("JSuggestField:"));
//		JPanel p = new JPanel(new BorderLayout());
//		p.setMaximumSize(new Dimension(0, 0));
//                final JSuggestField s = new JSuggestField(f, sample);
//                final JButton drop = new JButton("v");
//                
//                
//                
//                drop.addActionListener(new ActionListener() {@Override public void actionPerformed(ActionEvent e) {
//                        if (!s.isSuggestVisible()) s.showSuggest();
//                        else s.hideSuggest();
//                }});
//                p.add(s, BorderLayout.CENTER);
//                p.add(drop, BorderLayout.EAST);
//		container.add(p);
//		f.add(container);
//		f.pack();
//		f.setVisible(true);
//	}
//	
//}
