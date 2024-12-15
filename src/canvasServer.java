import java.io.*;
import java.net.*;
import java.util.*;

public class canvasServer {
    private static Vector<PrintWriter> clients = new Vector<PrintWriter>();
    private static Vector<String> drawingData = new Vector<>(); // 그림 데이터를 저장할 Vector
    private static int port;

    public canvasServer(int port) {
        this.port = port;
        startServer();
    }

    private static void startServer(){
        System.out.println("서버가 시작되었습니다.");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("클라이언트 연결: " + socket);

            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                    BufferedReader in = new BufferedReader(inputStreamReader)
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clients) {
                    clients.add(out);
                }

                // 새 클라이언트에게 기존 그림 데이터를 전송
                synchronized (drawingData) {
                    for (String data : drawingData) {
                        out.println(data);
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("수신된 메시지: " + message);

                    // CLEAR 메시지 처리
                    if (message.equals("CLEAR")) {
                        synchronized (drawingData) {
                            drawingData.clear(); // 모든 그림 데이터 삭제
                        }
                    } else {
                        // 새로운 그림 데이터를 저장
                        synchronized (drawingData) {
                            drawingData.add(message);
                        }
                    }

                    broadcast(message);
                }
            } catch (IOException e) {
                System.out.println("연결 끊김");
            } finally {
                if (out != null) {
                    synchronized (clients) {
                        clients.remove(out);
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("클라이언트 연결 종료: " + socket);
            }
        }

        private void broadcast(String message) {
            synchronized (clients) {
                if (message.equals("CLEAR")) {
                    // CLEAR 명령을 모든 클라이언트로 브로드캐스트
                    for (PrintWriter writer : clients) {
                        writer.println("CLEAR");
                    }
                    return;
                }

                // 일반 메시지 처리
                for (PrintWriter writer : clients) {
                    writer.println(message);
                }
            }
        }
    }
}
