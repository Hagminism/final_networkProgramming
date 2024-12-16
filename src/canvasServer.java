import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import java.awt.*;

public class canvasServer extends JFrame{
    private JTextPane t_display;
    private DefaultStyledDocument document;

    private Vector<PrintWriter> clients = new Vector<PrintWriter>();
    private Vector<String> drawingData = new Vector<>(); // 그림 데이터를 저장할 Vector
    private int port; // 인스턴스마다 고유한 포트

    public canvasServer(int port) {
        this.port = port; // 인스턴스 변수로 포트 저장
        System.out.println("port = " + port);
        buildGUI();
        startServer();
    }

    private void buildGUI(){
        setTitle("canvasServer");
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
        printDisplay("서버가 포트 " + port + "에서 시작되었습니다.");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                new ClientHandler(serverSocket.accept(), this).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter for drawingData
    public synchronized Vector<String> getDrawingData() {
        return drawingData;
    }

    // Getter for clients
    public synchronized Vector<PrintWriter> getClients() {
        return clients;
    }

    private class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private canvasServer server; // 서버 인스턴스 참조

        public ClientHandler(Socket socket, canvasServer server) {
            this.socket = socket;
            this.server = server; // 현재 서버 객체와 연결
        }

        @Override
        public void run() {
            printDisplay("클라이언트 연결: " + socket);

            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                    BufferedReader in = new BufferedReader(inputStreamReader)
            ) {
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (server.getClients()) {
                    server.getClients().add(out); // 현재 클라이언트 추가
                }

                // 새 클라이언트에게 기존 그림 데이터를 전송
                synchronized (server.getDrawingData()) {
                    for (String data : server.getDrawingData()) {
                        out.println(data);
                    }
                }

                String message;
                while ((message = in.readLine()) != null) {
                    printDisplay("수신된 메시지: " + message);

                    if (message.equals("CLEAR")) {
                        synchronized (server.getDrawingData()) {
                            server.getDrawingData().clear(); // 모든 그림 데이터 삭제
                        }
                    } else {
                        synchronized (server.getDrawingData()) {
                            server.getDrawingData().add(message); // 새 그림 데이터 추가
                        }
                    }

                    broadcast(message);
                }
            } catch (IOException e) {
                printDisplay("연결 끊김");
            } finally {
                if (out != null) {
                    synchronized (server.getClients()) {
                        server.getClients().remove(out); // 클라이언트 제거
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                printDisplay("클라이언트 연결 종료: " + socket);
            }
        }

        private void broadcast(String message) {
            synchronized (server.getClients()) {
                if (message.equals("CLEAR")) {
                    for (PrintWriter writer : server.getClients()) {
                        writer.println("CLEAR");
                    }
                    return;
                }

                for (PrintWriter writer : server.getClients()) {
                    writer.println(message);
                }
            }
        }
    }
}

