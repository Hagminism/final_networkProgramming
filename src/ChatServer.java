import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Vector;

public class ChatServer extends JFrame {
    private JTextPane t_display;
    private DefaultStyledDocument document;

    private ServerSocket serverSocket = null;
    private Vector<ClientHandler> clients = new Vector<>();
    private Vector<FileChatMsg> chattingData = new Vector<>();

    private int port;
    private String userID;

    public ChatServer(int port, String userID) {
        this.port = port;
        this.userID = userID;
        buildGUI();
        startServer();
    }

    private void buildGUI(){
        setTitle("ChatServer");
        setSize(400,600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        add(createDisplayPanel(), BorderLayout.CENTER);
        setVisible(true);
    }

    private JPanel createDisplayPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        document = new DefaultStyledDocument();
        t_display = new JTextPane(document);

        t_display.setEditable(false);

        panel.add(new JScrollPane(t_display), BorderLayout.CENTER);

        return panel;
    }

    private void printDisplay(String msg) {
        int len = t_display.getDocument().getLength();

        try {
            document.insertString(len, msg + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        t_display.setCaretPosition(len);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            printDisplay("서버가 시작되었습니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                printDisplay("클라이언트가 연결되었습니다: " + clientAddress);

                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            printDisplay("서버 오류: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            printDisplay("서버 소켓 종료 오류: " + e.getMessage());
        }
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private ObjectInputStream objIn;
        private ObjectOutputStream objOut;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                // 클라이언트에 대한 독립적인 스트림 생성
                objOut = new ObjectOutputStream(socket.getOutputStream());
                objIn = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                printDisplay("스트림 생성 오류: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                // 클라이언트의 첫 번째 메시지를 받아 userID 설정
                Object firstMessage = objIn.readObject();
                if (firstMessage instanceof FileChatMsg) {
                    FileChatMsg msg = (FileChatMsg) firstMessage;
                    if (msg.getMode() == FileChatMsg.MODE_LOGIN) {
                        userID = msg.getMessage(); // 클라이언트 ID 설정
                        printDisplay("클라이언트 연결됨: " + userID);

                        // 입장 메시지 브로드캐스트
                        FileChatMsg joinMsg = new FileChatMsg("System", FileChatMsg.MODE_TX_STRING, userID + " 님이 입장하셨습니다.");
                        broadcast(joinMsg, this);

                        // 기존 채팅 데이터 전송
                        synchronized (chattingData) {
                            for (FileChatMsg dataMsg : chattingData) {
                                rewrite(dataMsg, this);
                            }
                        }
                    } else {
                        printDisplay("유효하지 않은 첫 번째 메시지입니다.");
                        disconnect();
                        return;
                    }
                }

                while (true) {
                    Object received = objIn.readObject();
                    if (received instanceof FileChatMsg) {
                        FileChatMsg msg = (FileChatMsg) received;
                        printDisplay(userID + ":" + msg);
                        handleMessage(msg);
                        chattingData.add(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                printDisplay("클라이언트 처리 오류: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void disconnect() {
            try {
                printDisplay("클라이언트 연결 종료: " + socket.getInetAddress());
                clients.remove(this);

                // 퇴장 메시지 브로드캐스트
                FileChatMsg leaveMsg = new FileChatMsg("System", FileChatMsg.MODE_TX_STRING, userID + " 님이 퇴장하셨습니다.");
                broadcast(leaveMsg, this);

                socket.close();
            } catch (IOException e) {
                printDisplay("클라이언트 소켓 종료 오류: " + e.getMessage());
            }
        }

        private void handleMessage(FileChatMsg msg) {
            broadcast(msg, this);
        }
    }

    private void broadcast(FileChatMsg msg, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) { // 송신자를 제외한 클라이언트에게만 메시지 전송
                try {
                    client.objOut.writeObject(msg);
                    client.objOut.flush();
                } catch (IOException e) {
                    printDisplay("브로드캐스트 오류: " + e.getMessage());
                }
            }
        }
    }

    private void rewrite(FileChatMsg msg, ClientHandler sender) {
        try {
            sender.objOut.writeObject(msg);
            sender.objOut.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
