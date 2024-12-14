import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    private static final int PORT = 12345;
    private static ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, String> friendRequests = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Integer> roomPortMap = new ConcurrentHashMap<>();

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
                    if (message.startsWith("CHAT_INVITE:")) {
                        try {
                            String[] parts = message.substring(12).split(":", 3);
                            String friendsList = parts[0].trim();
                            String roomName = parts[1].trim();
                            int port = Integer.parseInt(parts[2].trim());

                            synchronized (roomPortMap) {
                                roomPortMap.put(roomName, port);
                            }
                            handleChatInvite(friendsList, roomName);
                        } catch (Exception e) {
                            System.err.println("CHAT_INVITE 메시지 처리 오류: " + message);
                            e.printStackTrace();
                        }
                    } else if (message.startsWith("TITLE_CHANGE:")) {
                        String[] parts = message.split(":");
                        String oldTitle = parts[1];
                        String newTitle = parts[2];
                        int port = Integer.parseInt(parts[3]);

                        synchronized (roomPortMap) {
                            if (roomPortMap.containsKey(oldTitle)) {
                                roomPortMap.remove(oldTitle);
                                roomPortMap.put(newTitle, port);
                                System.out.println("방 제목 변경 처리됨: " + oldTitle + " -> " + newTitle);
                            } else {
                                System.out.println("오류: 변경하려는 방 제목이 서버에 존재하지 않습니다.");
                            }
                        }
                    } else if (message.startsWith("ADD_FRIEND:")) {
                        handleFriendRequest(message.substring(11));
                    } else if (message.equals("LOGOUT")) {
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
            if (!clients.containsKey(friendID)) {
                out.println("친구 요청 실패: " + friendID + "님은 현재 접속 중이 아닙니다.");
                return;
            }
            if (friendRequests.containsKey(friendID)) {
                out.println("친구 요청 실패: 이미 요청을 보냈습니다.");
                return;
            }

            ClientHandler friendHandler = clients.get(friendID);
            friendRequests.put(friendID, userID);
            friendHandler.out.println("ADD_FRIEND:" + userID);
            System.out.println(userID + "님이 " + friendID + "님에게 친구 요청을 보냈습니다.");
        }

        private void handleChatInvite(String selectedFriendsList, String roomName) {
            int port;
            synchronized (roomPortMap) {
                port = roomPortMap.getOrDefault(roomName, -1);
            }

            if (port == -1) {
                System.out.println("오류: 채팅방 포트 번호를 찾을 수 없습니다.");
                return;
            }

            String[] friends = selectedFriendsList.split(",");
            for (String friendID : friends) {
                if (clients.containsKey(friendID)) {
                    ClientHandler friendHandler = clients.get(friendID);
                    friendHandler.out.println("ALERT:" + userID + "님이 채팅방 '" + roomName + "'(포트: " + port + ")에 초대했습니다.");
                    System.out.println(userID + "님이 " + friendID + "님에게 초대 알림을 보냈습니다.");
                } else {
                    System.out.println("초대 실패: " + friendID + "님이 접속 중이 아닙니다.");
                }
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
                statusMessage.setLength(statusMessage.length() - 1);
            }

            String status = "STATUS:" + statusMessage.toString();
            clients.values().forEach(client -> client.out.println(status));
        }
    }
}
