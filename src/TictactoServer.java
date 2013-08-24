


import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.*;

public class TictactoServer {
	private final static int MINPORT = 1023;
	private final static int MAXPORT = 65536;
	private final static int TTTSERVERARGS = 1;
	
	private static LinkedList<TictactoThreadedServer> players;
	private static LinkedList<TictactoThreadedServer> rejects;
	private static boolean isClosed;
	private static int playerDiscon;
	
	public static void main(String args[]) throws Exception {
		int serverPort = 0;
		boolean passes = true;
		isClosed = false;
		int connects = 0;
		int rejections = 0;
		players = new LinkedList<TictactoThreadedServer>();
		rejects = new LinkedList<TictactoThreadedServer>();
		TTTModelBoard sharedBoard = new TTTModelBoard();
		TTTModelBoard rejectBoard = new TTTModelBoard();
		ServerSocket welcome;
		playerDiscon = 0;
		
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
				ThreadPoolExecutor pool = new ThreadPoolExecutor(6, 12, 300, TimeUnit.MILLISECONDS, queuedThreads);
				
				while(!isClosed) {
					if(connects < 2) {
						System.out.println("Finding new player");
						Socket connect = welcome.accept();
						players.add(new TictactoThreadedServer(connect, sharedBoard, connects));
						pool.execute(players.get(connects));
						System.out.println("Player: "+connects+" has connected part");
						connects++;
					} else {
						Socket connect = welcome.accept();
						rejects.add(new TictactoThreadedServer(connect, rejectBoard, connects));
						pool.execute(rejects.get(rejections));
						System.out.println("Rejecting extra thread");
						rejections = rejects.size();
					}
					isClosed = testPlayersClosed();
					testRejectionClosed();
				}
				System.out.println("Shutting down now");
				welcome.close();
				pool.shutdown();
				
			} catch(IOException e) {
				System.out.println("Could not establish connection on this port");
			}
		} else {
			System.out.println("You have made an error in starting the program");
			System.out.println("Ports need to be more than 1023 and less than 65536");
		}
	}
	
	public static boolean testPlayersClosed() {
		boolean result = false;
		for(int i=0; i<players.size(); i++) {
			if(players.get(i).getSocket().isClosed()) {
				System.out.println("A client connection has closed");
				playerDiscon++;
				players.remove(i);
			}
		}
		if(playerDiscon == 2) {
			result = true;
		}
		return result;
	}
	
	public static void testRejectionClosed() {
		for(int i=0; i<rejects.size(); i++) {
			if(rejects.get(i).getSocket().isClosed()) {
				rejects.remove(i);
			}
		}
	}

}
