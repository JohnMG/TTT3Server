


import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TictactoServer {
	private final static int MINPORT = 1023;
	private final static int MAXPORT = 65536;
	private final static int TTTSERVERARGS = 1;
	private final static int NUMCON = 2;
	private static TictactoThreadedServer[] players;
	private static boolean isClosed;
	
	public static void main(String args[]) throws Exception {
		int serverPort = 0;
		boolean passes = true;
		isClosed = false;
		int connects = 0;
		players = new TictactoThreadedServer[2];
		TTTModelBoard sharedBoard = new TTTModelBoard();
		ServerSocket welcome;
		
		if(args.length != TTTSERVERARGS) {
			//passes = false
			serverPort = 2064;
		} else {
			serverPort = Integer.parseInt(args[0]);
			if((serverPort < MINPORT) ||(serverPort > MAXPORT)) {
				passes = false; 
			}
		}
		
		if(passes) {
			try {
				welcome = new ServerSocket(serverPort);
				System.out.println("Opened a new port");
				
				BlockingQueue<Runnable> queuedThreads = new ArrayBlockingQueue<Runnable>(10);
				ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 4, 300, TimeUnit.MILLISECONDS, queuedThreads);
				
				while(!isClosed) {
					if(connects < 2) {
						System.out.println("Finding new player");
						Socket connect = welcome.accept();
						players[connects] = new TictactoThreadedServer(connect, sharedBoard, connects);
						pool.execute(players[connects]);
						System.out.println("Player: "+connects+" has connected part");
						connects++;
					} else {
						isClosed = testConnectsClosed();
					}
				}
				welcome.close();
				
			} catch(IOException e) {
				System.out.println("Could not establish connection on this port");
			}
		} else {
			System.out.println("You have made an error in starting the program");
			System.out.println("Ports need to be more than 1023 and less than 65536");
		}
	}
	
	public static boolean testConnectsClosed() {
		boolean result = false;
		for(int i=0; i<NUMCON; i++) {
			if(players[i].getSocket().isClosed()) {
				result = true;
				System.out.println("A client connection has closed");
			}
		}
		return result;
	}

}
