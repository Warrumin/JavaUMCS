import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class KolkoKrzyzykGUI {

    private JFrame frame = new JFrame("Tic Tac Toe");
    private JLabel messageLabel = new JLabel("");
    private String icon;
    private String opponentIcon;
    private String status;
    private Square[] board = new Square[9];
    private Square currentSquare;

    private static int PORT = 8901;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructs the client by connecting to a server, laying out the GUI and registering GUI listeners.
  
    public KolkoKrzyzykGUI(String serverAddress) throws Exception {

        // Setup networking
        socket = new Socket(serverAddress, PORT);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Layout GUI
        messageLabel.setBackground(Color.DARK_GRAY);
        frame.getContentPane().add(messageLabel, "South");

        JPanel boardPanel = new JPanel();
        boardPanel.setBackground(Color.black);
        boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
        for (int i = 0; i < board.length; i++) {
            final int j = i;
            board[i] = new Square();
            board[i].addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    currentSquare = board[j];
                    out.println("MOVE " + j);}});
            boardPanel.add(board[i]);
        }
        frame.getContentPane().add(boardPanel, "Center");
    }

   //* The main thread of the client will listen for messages from the server.  
   //The first message will be a "WELCOME" message in which we receive our mark.  
   //Then we go into a loop listening for: 
   //--> "VALID_MOVE", --> "OPPONENT_MOVED", --> "VICTORY", --> "DEFEAT", --> "TIE", --> "OPPONENT_QUIT, --> "MESSAGE" messages, and handling each message appropriately.
   //The "VICTORY","DEFEAT" and "TIE" ask the user whether or not to play another game. 
   //If the answer is no, the loop is exited and the server is sent a "QUIT" message.  If an OPPONENT_QUIT message is recevied then the loop will exit and the server will be sent a "QUIT" message also.
    public void play() throws Exception {
        String response;
        try {
            response = in.readLine();
            if (response.startsWith("WELCOME")) {
                char mark = response.charAt(8);
                if(mark == 'X') {
                    icon = "X";
                    opponentIcon = "O";
                } else {
                    icon = "O";
                    opponentIcon = "X";
                }
                frame.setTitle("Tic Tac Toe - Gracz " + mark);
            }
            while (true) {
                response = in.readLine();
                if (response.startsWith("VALID_MOVE")) {
                    messageLabel.setText("Wykonałeś ruch - kolej przeciwnika");
                    currentSquare.setIcon(icon);
                    currentSquare.repaint();
                } else if (response.startsWith("OPPONENT_MOVED")) {
                    int loc = Integer.parseInt(response.substring(15));
                    board[loc].setIcon(opponentIcon);
                    board[loc].repaint();
                    messageLabel.setText("Twój ruch");
                } else if (response.startsWith("VICTORY")) {
                    status = "Wygrałeś";
                    messageLabel.setText("Wygrałeś!");
                    break;
                } else if (response.startsWith("DEFEAT")) {
                    status = "Przegrałeś";
                    messageLabel.setText("Przegrałeś!");
                    break;
                } else if (response.startsWith("TIE")) {
                    messageLabel.setText("Remis");
                    break;
                } else if (response.startsWith("MESSAGE")) {
                    messageLabel.setText(response.substring(8));
                }
            }
            out.println("QUIT");
        }
        finally {
            socket.close();
        }
    }

    private boolean wantsToPlayAgain() {
        int response = JOptionPane.showConfirmDialog(frame,
            status + ". Chcesz zagrać jeszcze raz?",
            "Tic Tac Toe",
            JOptionPane.YES_NO_OPTION);
        frame.dispose();
        return response == JOptionPane.YES_OPTION;
    }

    //Graphical square in the client window.  
    static class Square extends JPanel {
        JLabel label = new JLabel((Icon)null);

        public Square() {
            setBackground(Color.white);
            add(label);
        }

        public void setIcon(String icon) {
            label.setText(icon);
            label.setFont(new Font("Serif", Font.PLAIN, 160));
        }
    }
    
    //main
    public static void main(String[] args) throws Exception {
        while (true) {
            String serverAddress = (args.length == 0) ? "localhost" : args[1];
            KolkoKrzyzykGUI client = new KolkoKrzyzykGUI(serverAddress);
            client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            client.frame.setSize(720, 480);
            client.frame.setVisible(true);
            client.frame.setResizable(false);
            client.play();
            if (!client.wantsToPlayAgain()) {
                break;
            }
        }
    }
}