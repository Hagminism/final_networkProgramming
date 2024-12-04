import java.io.*;
import java.net.*;
import java.util.Vector;

public class ChatServer {
    private static final int PORT_NUM = 54321; // 고정된 포트 번호
    private ServerSocket serverSocket = null;
    private Vector<ClientHandler> clients = new Vector<>();

    public ChatServer() {
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT_NUM);
            System.out.println("서버가 시작되었습니다.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientAddress = clientSocket.getInetAddress().getHostAddress();
                System.out.println("클라이언트가 연결되었습니다: " + clientAddress);

                ClientHandler handler = new ClientHandler(clientSocket);
                clients.add(handler);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    private void stopServer() {
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("서버 소켓 종료 오류: " + e.getMessage());
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
                System.err.println("스트림 생성 오류: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                System.out.println("클라이언트 연결됨: " + socket.getInetAddress());

                // 입장 메시지 브로드캐스트
                FileChatMsg joinMsg = new FileChatMsg("System", FileChatMsg.MODE_TX_STRING, socket.getInetAddress() + " 님이 입장하셨습니다.");
                broadcast(joinMsg, this);

                while (true) {
                    Object received = objIn.readObject();
                    if (received instanceof FileChatMsg) {
                        FileChatMsg msg = (FileChatMsg) received;
                        handleMessage(msg);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("클라이언트 처리 오류: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void disconnect() {
            try {
                System.out.println("클라이언트 연결 종료: " + socket.getInetAddress());
                clients.remove(this);

                // 퇴장 메시지 브로드캐스트
                FileChatMsg leaveMsg = new FileChatMsg("System", FileChatMsg.MODE_TX_STRING, socket.getInetAddress() + " 님이 퇴장하셨습니다.");
                broadcast(leaveMsg, this);

                socket.close();
            } catch (IOException e) {
                System.err.println("클라이언트 소켓 종료 오류: " + e.getMessage());
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
                    System.err.println("브로드캐스트 오류: " + e.getMessage());
                }
            }
        }
    }


    public static void main(String[] args) {
        new ChatServer();
    }
}
