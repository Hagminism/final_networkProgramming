import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    private static final int PORT = 12345;
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> friendRequests = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("서버가 포트 " + PORT + "에서 시작되었습니다.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userID;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 로그인 처리
                out.println("서버에 연결되었습니다. 로그인 정보를 보내주세요.");
                String loginMessage = in.readLine();
                if (loginMessage.startsWith("LOGIN:")) {
                    userID = loginMessage.substring(6);
                    clients.put(userID, this);
                    broadcastStatus();
                    System.out.println(userID + "님이 접속했습니다.");
                }

                // 클라이언트 메시지 처리
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("ADD_FRIEND:")) {
                        handleFriendRequest(message.substring(11));
                    } else if (message.equals("LOGOUT")) {
                        handleLogout();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleLogout();
            }
        }

        private void handleFriendRequest(String friendID) {
            if (clients.containsKey(friendID)) {
                ClientHandler friendHandler = clients.get(friendID);
                friendRequests.put(friendID, userID);
                friendHandler.out.println("ADD_FRIEND:" + userID);
                System.out.println(userID + "님이 " + friendID + "님에게 친구 요청을 보냈습니다.");
            } else {
                out.println("친구 요청 실패: " + friendID + "님이 접속 중이 아닙니다.");
            }
        }

        private void handleLogout() {
            if (userID != null) {
                clients.remove(userID);
                broadcastStatus();
                System.out.println(userID + "님이 로그아웃했습니다.");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void broadcastStatus() {
            StringBuilder statusMessage = new StringBuilder();
            clients.forEach((id, handler) -> statusMessage.append(id).append(":").append(true).append(","));
            if (statusMessage.length() > 0) {
                statusMessage.setLength(statusMessage.length() - 1); // 마지막 쉼표 제거
            }

            String status = "STATUS:" + statusMessage.toString();
            clients.values().forEach(client -> client.out.println(status));
        }
    }
}
