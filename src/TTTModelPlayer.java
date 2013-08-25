/*
 * Author: John Massy-Greene
 * Program: TicTacTo3.0 - Internet Multiplayer
 * Date: 25/8/13
 * Comment: This is the class that represents and holds information about the players
 * 			Their names, piece and victories
 */

public class TTTModelPlayer {
	public final char NOUGHT = 'O';
	public final char CROSS = 'X';
	public final int NONUM = 0;
	
	private String name;
	private char piece;
	private int victories;
	
	public TTTModelPlayer(){
		this.victories = 0;
	}
//various getters and setters
	public char getPiece(){
		return this.piece;
	}
	public void setPiece(int i) {
		if(i == NONUM) {
			this.piece = NOUGHT;
		} else {
			this.piece = CROSS;
		}
	}
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getVictories(){
		return this.victories;
	}
	public void addVictory(){
		this.victories++;
	}
	public void resetVictory() {
		this.victories = 0;
	}
//helper function that is used to get the piece and victories
//used by the server to help the client update its view
	public String pieceAndVictory(){
		String result = "Piece: "+this.piece+" Won: "+this.victories+" times";
		return result;
	}
}
