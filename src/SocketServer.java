import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

public class SocketServer {
    private static final int PORT = 12345;
    private final Map<String, String> userStatuses = new ConcurrentHashMap<>(); // 사용자 상태 메시지
    private final Map<String, Boolean> activeUsers = new ConcurrentHashMap<>(); // 접속 중인 사용자 ID
    private final Map<ClientHandler, Boolean> clients = new ConcurrentHashMap<>(); // 접속 중인 클라이언트
    public static ConcurrentHashMap<String, Integer> roomPortMap = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> userClientMap = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        new SocketServer().startServer();
    }

    public void startServer() {
        loadUserStatusesFromFile(); // 서버 시작 시 상태 메시지 로드
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveUserStatusesToFile)); // 서버 종료 시 상태 메시지 저장

        out.println("서버 시작 중...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                synchronized (clients) {
                    clients.put(clientHandler, true); // 클라이언트 추가
                }
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 접속 상태와 상태 메시지 브로드캐스트
    public void broadcastStatus() {
        StringBuilder statusMessage = new StringBuilder("STATUS:");
        userStatuses.forEach((user, status) -> {
            if (activeUsers.containsKey(user)) { // 접속 중인 사용자만
                statusMessage.append(user).append(":").append(status).append(",");
            }
        });

        if (statusMessage.length() > 7) {
            statusMessage.setLength(statusMessage.length() - 1); // 마지막 쉼표 제거
        }

        clients.keySet().forEach(client -> client.sendMessage(statusMessage.toString()));
    }


    // 현재 접속자 목록 출력 (서버 콘솔용)
    public synchronized void displayActiveUsers() {
        out.println("=== 현재 접속자 목록 ===");
        if (activeUsers.isEmpty()) {
            out.println("현재 접속 중인 사용자가 없습니다.");
        } else {
            for (String user : activeUsers.keySet()) {
                out.println("- " + user);
            }

        }
        out.println("====================");
    }

    // CSV 파일에서 상태 메시지 로드
    private void loadUserStatusesFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("userStatusMessage.csv"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    userStatuses.put(parts[0], parts[1]);
                }
            }
            out.println("상태 메시지 데이터 로드 완료.");
        } catch (IOException e) {
            out.println("상태 메시지 로드 중 오류 발생: " + e.getMessage());
        }
    }

    // CSV 파일에 상태 메시지 저장
    private void saveUserStatusesToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("userStatusMessage.csv"))) {
            for (Map.Entry<String, String> entry : userStatuses.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue());
                writer.newLine();
            }
            out.println("상태 메시지 데이터 저장 완료.");
        } catch (IOException e) {
            out.println("상태 메시지 저장 중 오류 발생: " + e.getMessage());
        }
    }

    // 클라이언트 제거
    public synchronized void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
    }


    class ClientHandler extends Thread {
        private final Socket socket;
        private final SocketServer server;
        private PrintWriter out;
        private BufferedReader in;
        private String userID;

        public ClientHandler(Socket socket, SocketServer server) {
            this.socket = socket;
            this.server = server;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                System.out.println("클라이언트 연결 수립: " + socket.getInetAddress());
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("클라이언트 메시지: " + message);
                    if (message.startsWith("LOGIN:")) {
                        userID = message.substring(6).trim();
                        System.out.println("현재 activeUsers: " + activeUsers.keySet());
                        System.out.println("로그인 요청 userID: [" + userID + "]");

                        if (activeUsers.putIfAbsent(userID, true) != null) {
                            sendMessage("ERROR: 이미 로그인된 사용자입니다.");
                            server.displayActiveUsers();
                            server.broadcastStatus();
                            continue; // 이미 로그인된 사용자 처리
                        }

                        sendMessage("WELCOME");
                        userStatuses.putIfAbsent(userID, "상태 메시지 없음"); // 기본 상태 메시지 추가
                        System.out.println(userID + "님이 로그인했습니다.");
                        server.displayActiveUsers();
                        server.broadcastStatus();
                    }

                    else if (message.startsWith("UPDATE_STATUS:")) {
                        String newStatus = message.substring(14).trim();
                        synchronized (userStatuses) {
                            userStatuses.put(userID, newStatus); // 상태 메시지 업데이트
                        }
                        server.broadcastStatus(); // 변경 사항 브로드캐스트
                    }
                    else if (message.startsWith("ADD_FRIEND:")) {
                        System.out.println("ADD_FRIEND 요청 수신: " + message);

                        String targetID = message.substring(11).trim();
                        for (ClientHandler client : clients.keySet()) {
                            if (client.getUserID().equals(targetID)) {
                                client.sendMessage("FRIEND_REQUEST:" + userID); // 친구 요청 전송
                                break;
                            }
                        }
                    }
                    else if (message.startsWith("CHAT_INVITE:")) {
                        String[] parts = message.split(":", 3);
                        String friendsList = parts[1].trim();
                        String roomName = parts[2].trim();

                        String[] friends = friendsList.split(",");
                        for (String friendID : friends) {
                            for (ClientHandler client : clients.keySet()) {
                                if (client.getUserID().equals(friendID)) {
                                    client.sendMessage("CHAT_INVITE:" + friendID + ":" + roomName);
                                    break;
                                }
                            }
                        }
                    }

                    else if (message.startsWith("TITLE_CHANGE:")) {
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
                    }
                }
            } catch (IOException e) {
                System.out.println("클라이언트 연결 종료 또는 예외 발생: " + e.getMessage());
            } finally {
                logout(); // 연결 종료 시 로그아웃 처리
            }
        }

//        private void handleFriendRequest(String friendID) {
//            if (!clients.containsKey(friendID)) {
//                out.println("친구 요청 실패: " + friendID + "님은 현재 접속 중이 아닙니다.");
//                return;
//            }
//            if (friendRequests.containsKey(friendID)) {
//                out.println("친구 요청 실패: 이미 요청을 보냈습니다.");
//                return;
//            }
//
//            ClientHandler friendHandler = clients.get(friendID);
//            friendRequests.put(friendID, userID);
//            friendHandler.out.println("ADD_FRIEND:" + userID);
//            out.println(userID + "님이 " + friendID + "님에게 친구 요청을 보냈습니다.");
//        }

        private void handleChatInvite(String selectedFriendsList, String roomName) {
            int port;
            synchronized (roomPortMap) {
                port = roomPortMap.getOrDefault(roomName, -1);
            }

            if (port == -1) {
                out.println("오류: 채팅방 포트 번호를 찾을 수 없습니다.");
                return;
            }

            String[] friends = selectedFriendsList.split(",");
            for (String friendID : friends) {
                ClientHandler friendHandler = userClientMap.get(friendID);
                if (friendHandler != null) {
                    friendHandler.sendMessage("ALERT:" + userID + "님이 채팅방 '" + roomName + "'(포트: " + port + ")에 초대했습니다.");
                    out.println(userID + "님이 " + friendID + "님에게 초대 알림을 보냈습니다.");
                } else {
                    out.println("초대 실패: " + friendID + "님이 접속 중이 아닙니다.");
                }
            }
        }


        private void logout() {
            System.out.println(userID + "님이 로그아웃했습니다.");
            activeUsers.remove(userID); // 사용자 제거
            server.displayActiveUsers();
            clients.remove(this); // 클라이언트 목록에서 제거
            server.broadcastStatus();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        public String getUserID() {
            return userID;
        }
    }
}
