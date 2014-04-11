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

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * An implementation of {@link LocalClient} that is controlled by the keyboard
 * of the computer on which the game is being run.  
 * @author Geoffrey Washburn &lt;<a href="mailto:geoffw@cis.upenn.edu">geoffw@cis.upenn.edu</a>&gt;
 * @version $Id: GUIClient.java 343 2004-01-24 03:43:45Z geoffw $
 */

public class GUIClient extends LocalClient implements KeyListener {

        /**
         * Create a GUI controlled {@link LocalClient}.  
         */
        public GUIClient(String name, ObjectOutputStream out) {
                super(name, out);
        }
        
        /**
         * Handle a key press.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyPressed(KeyEvent e) {
        	
        	/*
        	 * 
        	 *                 // Down-arrow moves backward.
         } else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
                        backup();
                // Left-arrow turns left.
                } else if(e.getKeyCode() == KeyEvent.VK_LEFT) {
                        turnLeft();
                // Right-arrow turns right.
                } else if(e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        turnRight();
                // Spacebar fires.
                } else if(e.getKeyCode() == KeyEvent.VK_SPACE) {
                        fire();
                }
        	 */
        	
        	
                // If the user pressed Q, invoke the cleanup code and quit. 
                if((e.getKeyChar() == 'q') || (e.getKeyChar() == 'Q')) {
                	
                	try {
						notifyServer(this.getOutputStream(), this.getInputStream(), KeyEvent.VK_U);
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                    //Mazewar.quit();
                } 
                
                //notifies server of action client is taking
                else {
                	try {
						notifyServer(this.getOutputStream(), this.getInputStream(), e.getKeyCode());
					} catch (ClassNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
                }  
        }
        //KeyEvent.VK_U
        /**
         * Handle a key release. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyReleased(KeyEvent e) {
        }
        
        /**
         * Handle a key being typed. Not needed by {@link GUIClient}.
         * @param e The {@link KeyEvent} that occurred.
         */
        public void keyTyped(KeyEvent e) {
        }
        
        public void notifyServer(ObjectOutputStream out, ObjectInputStream in, int action) throws IOException, ClassNotFoundException{
        	
        	//notify server and wait for reply
			InstructionPacket notify = new InstructionPacket();
			notify.request_type = InstructionPacket.CLIENT_ACTION;
			notify.client_id = this.getName();
			notify.action = action;
			notify.pos_x = this.getPoint().getX();
			notify.pos_y = this.getPoint().getY();
			
			//dummy variables
			notify.location =  new ClientLocation("localhost", 1234);
			out.writeObject(notify);
        }
}


