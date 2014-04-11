/*
Copyright (C) 2004 Geoffrey Alan Washburn
   
This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.
   
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
   
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
USA.
*/
  
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JOptionPane;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.BorderFactory;

import java.io.*;
import java.net.*;
//import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;


/**
 * The entry point and glue code for the game.  It also contains some helpful
 * global utility methods.
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: Mazewar.java 371 2004-02-10 21:55:32Z geoffw $
 */

public class Mazewar extends JFrame {

        /**
         * The default width of the {@link Maze}.
         */
        private final int mazeWidth = 20;

        /**
         * The default height of the {@link Maze}.
         */
        private final int mazeHeight = 10;

        /**
         * The default random seed for the {@link Maze}.
         * All implementations of the same protocol must use 
         * the same seed value, or your mazes will be different.
         */
        private final int mazeSeed = 42;

        /**
         * The {@link Maze} that the game uses.
         */
        private Maze maze = null;

        /**
         * The {@link GUIClient} for the game.
         */
        private GUIClient guiClient = null;

        /**
         * The panel that displays the {@link Maze}.
         */
        private OverheadMazePanel overheadPanel = null;

        /**
         * The table the displays the scores.
         */
        private JTable scoreTable = null;
        
        /** 
         * Create the textpane statically so that we can 
         * write to it globally using
         * the static consolePrint methods  
         */
        private static final JTextPane console = new JTextPane();
      
        /** 
         * Write a message to the console followed by a newline.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrintLn(String msg) {
                console.setText(console.getText()+msg+"\n");
        }
        
        /** 
         * Write a message to the console.
         * @param msg The {@link String} to print.
         */ 
        public static synchronized void consolePrint(String msg) {
                console.setText(console.getText()+msg);
        }
        
        /** 
         * Clear the console. 
         */
        public static synchronized void clearConsole() {
           console.setText("");
        }
        
        /**
         * Static method for performing cleanup before exiting the game.
         */
        public static void quit() {
                // Put any network clean-up code you might have here.
                // (inform other implementations on the network that you have 
                //  left, etc.)
                

                System.exit(0);
        }
       
        /** 
         * The place where all the pieces are put together. 
         */
        public Mazewar(String host, int port, int clientPort) throws IOException, ClassNotFoundException {
                super("ECE419 Mazewar");
                consolePrintLn("ECE419 Mazewar started!");
                
                int index=0;
                
                // Create the maze
                maze = new MazeImpl(new Point(mazeWidth, mazeHeight), mazeSeed);
                assert(maze != null);
                
                // Have the ScoreTableModel listen to the maze to find
                // out how to adjust scores.
                ScoreTableModel scoreModel = new ScoreTableModel();
                assert(scoreModel != null);
                maze.addMazeListener(scoreModel);
                
                // Throw up a dialog to get the GUIClient name.
                String name = JOptionPane.showInputDialog("Enter your name");
                if((name == null) || (name.length() == 0)) {
                  Mazewar.quit();
                }
                
                // You may want to put your network initialization code somewhere in
                // here.
				//connecting to server
				Socket serverSocket = null;
				ObjectOutputStream out = null;
				ObjectInputStream in = null;
				ServerSocket clientSocket = null;
				try{
					System.out.println("connecting to server: "+host+" "+port);
					serverSocket = new Socket(host, port);
			
					out = new ObjectOutputStream(serverSocket.getOutputStream());
					in = new ObjectInputStream(serverSocket.getInputStream());
				}
				catch(UnknownHostException e){
					System.err.println("ERROR: Don't know where to connect!!");
					Mazewar.quit();
				}
				catch(IOException e) {
					System.err.println("ERROR: Couldn't get I/O for the connection.");
					Mazewar.quit();
				}
				
				try{
					clientSocket= new ServerSocket(clientPort); 
				}
				catch (IOException e){
		            System.err.println("ERROR: Could not listen on port!");
		            System.exit(-1);					
				}
				//register the client with the server
				System.out.println("registering the client with server");
				InstructionPacket register = new InstructionPacket();
				register.request_type = InstructionPacket.CLIENT_REGISTER;
				register.location = new ClientLocation(host, port);
				register.client_id = new String(name);

				out.writeObject(register);

				InstructionPacket confirm = (InstructionPacket)in.readObject();
				
                
                // Create the GUIClient and connect it to the KeyListener queue
				Queue<InstructionPacket> actions = new LinkedList<InstructionPacket>();
				HashMap<String, Client> clients = new HashMap<String, Client>();
				Vector<Client> players = new Vector<Client>();
				

				//Vector<String, Client> players = new Vector<String, Client>();	

				//id already registered
				if(confirm.error_code == InstructionPacket.ERROR_INVALID_ID){
					System.out.println("wrong id lol");
					in.close();
					out.close();
					serverSocket.close();
					Mazewar.quit();
				}
				//list of clients
				else if(confirm.request_type == InstructionPacket.SERVER_ACCEPT){
					while((confirm = (InstructionPacket)in.readObject()).request_type != InstructionPacket.SERVER_DONE){
						//add client to list of clients, then spawn a remoteClient object for them
						RemoteClient new_client = new RemoteClient(confirm.client_id, confirm.pos_x, confirm.pos_y, confirm.orientation, confirm.location.client_host, confirm.location.client_port);
						clients.put(confirm.client_id, new_client);
						players.addElement(new_client);
						maze.addClient(new_client);
						index++;
					}
				}
				
				//enqueueing thread
				new ServerListeningThread(in, actions, name, maze,clients,players).start();
				
                guiClient = new GUIClient(name, out, actions, InetAddress.getLocalHost().getHostName(), clientPort);
                consolePrintLn("putting "+name+" into the hash");
                

                
                maze.addClient(guiClient);
                
                clients.put(name, guiClient);
                players.addElement(guiClient);
                
                confirm = new InstructionPacket();
				confirm.request_type = InstructionPacket.CLIENT_ACTION;
                confirm.client_id=guiClient.getName();
                confirm.pos_x= guiClient.getPoint().getX();
                confirm.pos_y= guiClient.getPoint().getY();
                confirm.orientation = guiClient.getDirection();
                confirm.location = new ClientLocation(InetAddress.getLocalHost().getHostName(), clientPort);
                
                //sending server our coordinates
                out.writeObject(confirm);
                
                
                this.addKeyListener(guiClient);
                System.out.println("added keylistener");
                
                new CommunicationThread(actions,clients,players,maze, clientSocket, index, name).start();
                
                // Use braces to force constructors not to be called at the beginning of the
                // constructor.
                /*{
                        maze.addClient(new RobotClient("Norby"));
                        maze.addClient(new RobotClient("Robbie"));
                        maze.addClient(new RobotClient("Clango"));
                        maze.addClient(new RobotClient("Marvin"));
                }*/

                
                // Create the panel that will display the maze.
                overheadPanel = new OverheadMazePanel(maze, guiClient);
                assert(overheadPanel != null);
                maze.addMazeListener(overheadPanel);
                
                // Don't allow editing the console from the GUI
                console.setEditable(false);
                console.setFocusable(false);
                console.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder()));
               
                // Allow the console to scroll by putting it in a scrollpane
                JScrollPane consoleScrollPane = new JScrollPane(console);
                assert(consoleScrollPane != null);
                consoleScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Console"));
                
                // Create the score table
                scoreTable = new JTable(scoreModel);
                assert(scoreTable != null);
                scoreTable.setFocusable(false);
                scoreTable.setRowSelectionAllowed(false);

                // Allow the score table to scroll too.
                JScrollPane scoreScrollPane = new JScrollPane(scoreTable);
                assert(scoreScrollPane != null);
                scoreScrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Scores"));
                
                // Create the layout manager
                GridBagLayout layout = new GridBagLayout();
                GridBagConstraints c = new GridBagConstraints();
                getContentPane().setLayout(layout);
                
                // Define the constraints on the components.
                c.fill = GridBagConstraints.BOTH;
                c.weightx = 1.0;
                c.weighty = 3.0;
                c.gridwidth = GridBagConstraints.REMAINDER;
                layout.setConstraints(overheadPanel, c);
                c.gridwidth = GridBagConstraints.RELATIVE;
                c.weightx = 2.0;
                c.weighty = 1.0;
                layout.setConstraints(consoleScrollPane, c);
                c.gridwidth = GridBagConstraints.REMAINDER;
                c.weightx = 1.0;
                layout.setConstraints(scoreScrollPane, c);
                                
                // Add the components
                getContentPane().add(overheadPanel);
                getContentPane().add(consoleScrollPane);
                getContentPane().add(scoreScrollPane);
                
                // Pack everything neatly.
                pack();

                // Let the magic begin.
                setVisible(true);
                overheadPanel.repaint();
                this.requestFocusInWindow();
        }

		/**
         * Entry point for the game.  
         * @param args Command-line arguments.
         */
        public static void main(String args[]) throws IOException, ClassNotFoundException {
				//check the args
				int port = -1, clientPort =-1;
				String host;
				if(args.length != 3){
					//System.out.println("wrong number of arguments");
					System.out.println("java Mazewar [ns_name] [ns_port] [client_port]");
					System.exit(0);
				}
				try{
					port = Integer.parseInt(args[1]);
					clientPort = Integer.parseInt(args[2]);
				}
				catch (NumberFormatException e) {
    				System.out.println("port number not defined!");
					System.exit(0);
  				}
				host = args[0];
                /* Create the GUI */
                new Mazewar(host, port, clientPort);
        }
}
