import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** Custom Drawing Code Template */
// Graphics application extends JFrame
public class Draw2D extends JFrame {
	// Constants
	public static final int CANVAS_WIDTH = 800;
	public static final int CANVAS_HEIGHT = 600;

	// Declare an instance the drawing canvas (extends JPanel)
	private DrawCanvas canvas;

	/** Constructor to set up the GUI components */
	public Draw2D() {
		canvas = new DrawCanvas(); // Construct the drawing canvas
		canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

		// Set the Drawing JPanel as the JFrame's content-pane
		Container cp = getContentPane();
		cp.add(canvas);
		// or
		// setContentPane(canvas);

		this.setDefaultCloseOperation(EXIT_ON_CLOSE); // Handle the CLOSE button
		this.pack(); // Either pack() the components; or setSize()
		this.setTitle("......"); // this JFrame sets the title
		this.setVisible(true); // this JFrame show
	}

	/**
	 * Define a inner class called DrawCanvas which is a JPanel used for custom
	 * drawing
	 */
	private class DrawCanvas extends JPanel {
		// Override paintComponent to perform your own painting
		@Override
		public void paintComponent(Graphics g) {
			BufferedImage bImg = new BufferedImage(CANVAS_WIDTH, CANVAS_HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = (Graphics2D)g;
//		    g2 = bImg.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			super.paintComponent(g2); // paint parent's background
			
			setBackground(Color.WHITE); // set background color for this JPanel
						
			g2.setColor(Color.GRAY);
			g2.drawLine(70, 50 - 20, 70, 500 + 20);
			g2.drawLine(70 - 20, 500 + 5, 750 + 25, 500 + 5);
			
			double genCut = 0.04999;
			
			try {
				Scanner scanner = new Scanner(new File("Results//com_java_draw.txt"));
				
				while (scanner.hasNext()) {
					int xMid = (int)scanner.nextDouble();
					int radius = (int)scanner.nextDouble();
					
					int yMin = (int)scanner.nextDouble();
					int yMid = (int)scanner.nextDouble();
					int yMax = (int)scanner.nextDouble();					
					
					double avgGen = scanner.nextDouble();

					g2.setColor(Color.GRAY);
					if (avgGen > genCut) {
						g2.setColor(Color.RED);
					}
					
					g2.drawLine(xMid, yMin, xMid, yMax);					
					g2.fillOval(xMid - radius, yMid - radius, 2 * radius, 2 * radius);					
				}
				
				scanner.close();
				
//				Printing texts
				g2.setColor(Color.BLACK);
				g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
				g2.drawString("Communities", 370, 520);
				g2.drawString("Communitie are sorted by descending size from left to right (also represented by circle size)", 90, 540);
				g2.drawString("Red marked communities have large Mean Generality", 240, 560);
				g2.drawString("1.0", 70 - 25, 55);
				g2.drawString("0.0", 70 - 25, 500);
				
//				ROTATED TEXT
				g2.rotate(-Math.PI/2);
				g2.drawString("Location", -325, 62);
				g2.drawString("25-75-50 percentile of Locations is lower, upper end of the line and circle", -550, 22);
				
				ImageIO.write(bImg, "png", new File("output_image.png"));
			} 
			catch(Exception e) {e.printStackTrace();}
		}
	}

	/** Entry main method */
	public static void main(String[] args) {
		// Run the GUI codes on the Event-Dispatching thread for thread safety
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new Draw2D(); // Let the constructor do the job
			}
		});
	}
}
