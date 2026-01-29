
package ubc.cosc322;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.ArrayList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;

/**
 * An example illustrating how to implement a GamePlayer
 * 
 * @author Yong Gao (yong.gao@ubc.ca)
 *         Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer {

    private GameClient gameClient = null;
    private BaseGameGUI gamegui = null;

    private String userName = null;
    private String passwd = null;

    /**
     * The main method
     * 
     * @param args for name and passwd (current, any string would work)
     */
    public static void main(String[] args) {
        COSC322Test player = new COSC322Test(args[0], args[1]);

        if (player.getGameGUI() == null) {
            player.Go();
        } else {
            BaseGameGUI.sys_setup();
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    player.Go();
                }
            });
        }
    }

    /**
     * Any name and passwd
     * 
     * @param userName
     * @param passwd
     */
    public COSC322Test(String userName, String passwd) {
        this.userName = userName;
        this.passwd = passwd;

        // To make a GUI-based player, create an instance of BaseGameGUI
        // and implement the method getGameGUI() accordingly
        this.gamegui = new BaseGameGUI(this);

        // Add mouse listener to handle board clicks
        this.gamegui.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int pixelX = e.getX();
                int pixelY = e.getY();

                // Game of the Amazons is 10x10 board
                int boardSize = 10;
                int minx = 60;
                int maxx = 560;
                int miny = 95;
                int maxy = 595;

                int boardX = maxx - minx;
                int boardY = maxy - miny;

                int row = (boardY - (pixelY - miny)) / (boardY / boardSize);
                int col = (pixelX - minx) / (boardX / boardSize);
                // Board coordinates are typically 1-indexed in the game protocol
                char[] horiz = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j' };

                int boardRow = row + 1;
                char boardCol = horiz[col];
                System.out.println("cords: x: " + pixelX + " y: " + pixelY);
                System.out.println("Board position: Row=" + boardRow + ", Col=" + boardCol);
                System.out.println("---");
            }
        });
    }

    @Override
    public void onLogin() {
        userName = gameClient.getUserName();
        if (gamegui != null) {
            gamegui.setRoomInformation(gameClient.getRoomList());

        }

    }

    @Override
    public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
        // This method will be called by the GameClient when it receives a game-related
        // message
        // from the server.

        // For a detailed description of the message types and format,
        // see the method GamePlayer.handleGameMessage() in the game-client-api
        // document.
        if (messageType.equals(GameMessage.GAME_STATE_BOARD)) {
            ArrayList<Integer> stateArr = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.GAME_STATE));
            this.getGameGUI().setGameState(stateArr);

        } else if (messageType.equals(GameMessage.GAME_ACTION_MOVE)) {
            ArrayList<Integer> queenPosCurr = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR));
            ArrayList<Integer> queenPosNext = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT));
            ArrayList<Integer> arrowPos = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.ARROW_POS));
            this.getGameGUI().updateGameState(queenPosCurr, queenPosNext, arrowPos);
        }

        return true;
    }

    @Override
    public String userName() {
        return userName;
    }

    @Override
    public GameClient getGameClient() {
        // TODO Auto-generated method stub
        return this.gameClient;
    }

    @Override
    public BaseGameGUI getGameGUI() {
        // TODO Auto-generated method stub
        return this.gamegui;
    }

    @Override
    public void connect() {
        // TODO Auto-generated method stub
        gameClient = new GameClient(userName, passwd, this);
    }

}// end of class
