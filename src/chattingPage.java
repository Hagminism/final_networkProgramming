import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;

public class chattingPage extends JFrame {
    private FrameDragListener frameDragListener;
    private JTextPane t_display;
    private JTextField t_input;
    private JButton b_select, b_send, b_exitChat, b_emoji, b_canvas;
    private Socket socket;
    private canvasPage Canvas;
    private ObjectInputStream objIn;
    private ObjectOutputStream objOut;

    private String host;
    private int port;
    private String roomName;
    private String userID;

    public chattingPage(String host, int port, String roomName, String userID) {
        super("채팅방");

        this.host = host;
        this.port = port;
        this.roomName = roomName;
        this.userID = userID;

        setUndecorated(true);
        frameDragListener = new FrameDragListener(this);

        buildGUI();
        connectToServer();
        setVisible(true);
    }

    private void buildGUI() {
        setSize(400, 600);
        setLocation(1000, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(createMenuPanel(), BorderLayout.NORTH);
        add(createDisplayPanel(), BorderLayout.CENTER);
        add(createInputPanel(), BorderLayout.SOUTH);

        mouseMove();
    }

    private void mouseMove() {
        addMouseListener(frameDragListener);
        addMouseMotionListener(frameDragListener);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(new Color(255, 240, 180)); // 사용자 지정 배경색 유지

        // 뒤로가기 버튼
        b_exitChat = new JButton("  ⬅️");
        b_exitChat.setFont(new Font("SansSerif", Font.BOLD, 20));
        b_exitChat.setBackground(new Color(230, 230, 230));
        b_exitChat.setHorizontalAlignment(SwingConstants.CENTER);
        b_exitChat.setVerticalAlignment(SwingConstants.CENTER);
        b_exitChat.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        b_exitChat.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    chattingPage.this,
                    "채팅방을 종료하시겠습니까?",
                    "종료 확인",
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm == JOptionPane.YES_OPTION) {
                dispose(); // 채팅방 종료
            }
        });

        // 채팅방 이름 (중앙)
        JLabel chatTitle = new JLabel(roomName, JLabel.CENTER);
        chatTitle.setFont(new Font("나눔고딕", Font.BOLD, 18));
        chatTitle.setForeground(Color.DARK_GRAY);

        // 캔버스 버튼 (우측 상단)
        b_canvas = new JButton("  🖌️  ");
        b_canvas.setFont(new Font("SansSerif", Font.BOLD, 20));
        b_canvas.setBackground(new Color(255, 200, 200));
        b_canvas.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        b_canvas.addActionListener(e -> {
            int canvasPort = port + 100;
            if(!isPortInUse(canvasPort)){
                System.out.println("canvasPort = " + canvasPort);
                new Thread(() -> new canvasServer(canvasPort)).start(); // 캔버스 서버 실행
            }
            SwingUtilities.invokeLater(() -> Canvas = new canvasPage(canvasPort)); // 캔버스 페이지 실행
        });

        // 컴포넌트 배치
        panel.add(b_exitChat, BorderLayout.WEST); // 뒤로가기 버튼
        panel.add(chatTitle, BorderLayout.CENTER); // 중앙 타이틀
        panel.add(b_canvas, BorderLayout.EAST); // 우측 상단 캔버스 버튼

        return panel;
    }

    private boolean isPortInUse(int port){
        try (ServerSocket socket = new ServerSocket(port)) {
            // 포트를 열었으므로 사용 중이 아님
            return false;
        } catch (IOException e) {
            // 포트를 열 수 없으므로 이미 사용 중
            return true;
        }
    }

    private JPanel createDisplayPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(245, 245, 245)); // 사용자 지정 배경색 유지

        t_display = new JTextPane();
        t_display.setEditable(false);
        t_display.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t_display.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        t_display.setBackground(Color.WHITE);

        p.add(new JScrollPane(t_display), BorderLayout.CENTER);
        return p;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 245, 245)); // 사용자 지정 배경색 유지

        // 텍스트 입력란
        t_input = new JTextField(25);
        t_input.setBackground(new Color(255, 240, 180));
        t_input.setFont(new Font("SansSerif", Font.PLAIN, 14));
        t_input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        t_input.addActionListener(e -> sendMessage(t_input.getText()));

        // 파일 선택 버튼
        b_select = new JButton("📂");
        b_select.setFont(new Font("SansSerif", Font.PLAIN, 20));
        b_select.setBackground(new Color(200, 255, 200));
        b_select.addActionListener(e -> selectFile());

        // 이모티콘 버튼 (파일 선택 버튼과 보내기 버튼 사이)
        b_emoji = new JButton("🙂");
        b_emoji.setFont(new Font("SansSerif", Font.PLAIN, 20));
        b_emoji.setBackground(new Color(255, 255, 200));
        b_emoji.addActionListener(e -> showEmojiPicker());

        // 전송 버튼
        b_send = new JButton("📨");
        b_send.setFont(new Font("SansSerif", Font.PLAIN, 20));
        b_send.setBackground(new Color(200, 230, 255));
        b_send.addActionListener(e -> sendMessage(t_input.getText()));

        // 버튼 패널
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.add(b_select); // 파일 선택 버튼
        buttonPanel.add(b_emoji); // 이모티콘 버튼
        buttonPanel.add(b_send);  // 전송 버튼

        panel.add(t_input, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }


    private void connectToServer() {
        try {
            socket = new Socket(host, port);
            objOut = new ObjectOutputStream(socket.getOutputStream());
            objIn = new ObjectInputStream(socket.getInputStream());

            // 접속 후 바로 로그인 메시지 전송
            FileChatMsg loginMessage = new FileChatMsg(userID, FileChatMsg.MODE_LOGIN, userID);
            objOut.writeObject(loginMessage);
            objOut.flush();

            Thread receiveThread = new Thread(this::receiveMessages);
            receiveThread.start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage());
        }
    }

    private void sendMessage(String message) {
        if (message.trim().isEmpty()) return;
        try {
            // 메시지 본문만 서버로 전송
            FileChatMsg msg = new FileChatMsg(userID, FileChatMsg.MODE_TX_STRING, message);
            objOut.writeObject(msg);
            objOut.flush();

            // 전송한 메시지를 UI에 표시 (헤더는 appendMessage에서 처리)
            appendMessage(message, msg.getUserID());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "메시지 전송 실패: " + e.getMessage());
        }
        t_input.setText("");
    }


    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        int ret = fileChooser.showOpenDialog(this);

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] fileData = Files.readAllBytes(file.toPath());
                String fileName = file.getName();
                boolean isImage = isImageFile(fileName);

                ImageIcon imageIcon = null;
                if (isImage) {
                    // 이미지 데이터로 ImageIcon 생성
                    imageIcon = new ImageIcon(fileData);
                }

                // FileChatMsg 생성 및 전송
                FileChatMsg msg = new FileChatMsg(
                        userID,
                        isImage ? FileChatMsg.MODE_TX_IMAGE : FileChatMsg.MODE_TX_FILE,
                        fileName,
                        imageIcon,
                        fileData
                );
                objOut.writeObject(msg);
                objOut.flush();

                // UI 업데이트
                if (isImage) {
                    appendImage(imageIcon, msg.getUserID());
                } else {
                    appendFileLink(fileName, fileData, msg.getUserID());
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "파일 전송 실패: " + e.getMessage());
            }
        }
    }


    private void appendFileLink(String fileName, byte[] fileData, String sender) {
        StyledDocument doc = t_display.getStyledDocument();

        try {
            // 헤더 스타일 설정
            SimpleAttributeSet headerAttr = new SimpleAttributeSet();
            StyleConstants.setAlignment(headerAttr, userID.equals(sender) ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setForeground(headerAttr, userID.equals(sender) ? Color.BLUE : Color.RED);

            // 헤더 추가
            String header = userID.equals(sender) ? "나: " : sender + ": ";
            doc.insertString(doc.getLength(), header + "\n", headerAttr);
            doc.setParagraphAttributes(doc.getLength() - header.length(), header.length(), headerAttr, false);

            // 파일 링크 텍스트 추가
            int startLink = doc.getLength();
            doc.insertString(startLink, "[파일 다운로드] " + fileName + "\n", headerAttr);
            doc.setParagraphAttributes(startLink, doc.getLength() - startLink, headerAttr, false);

            // 링크 클릭 이벤트 처리
            t_display.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    try {
                        int pos = t_display.viewToModel2D(e.getPoint());
                        if (pos >= startLink && pos < startLink + ("[파일 다운로드] " + fileName).length()) {
                            saveFile(fileName, fileData);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void saveFile(String fileName, byte[] fileData) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));

        int ret = fileChooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(fileData);
                JOptionPane.showMessageDialog(this, "파일이 성공적으로 저장되었습니다: " + saveFile.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "파일 저장 실패: " + e.getMessage());
            }
        }
    }

    private boolean isImageFile(String fileName) {
        String[] validExtensions = {"jpg", "jpeg", "png", "gif"};
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return Arrays.asList(validExtensions).contains(extension);
    }

    private void receiveMessages() {
        try {
            while (true) {
                Object received = objIn.readObject();
                if (received instanceof FileChatMsg) {
                    FileChatMsg msg = (FileChatMsg) received;
                    handleReceivedMessage(msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("메시지 수신 실패: " + e.getMessage());
        }
    }

    private void handleReceivedMessage(FileChatMsg msg) {
        switch (msg.getMode()) {
            case FileChatMsg.MODE_TX_STRING:
                appendMessage(msg.getMessage(), msg.getUserID());
                break;
            case FileChatMsg.MODE_TX_IMAGE:
                appendImage(new ImageIcon(msg.getFileData()), msg.getUserID());
                break;
            case FileChatMsg.MODE_TX_FILE:
                appendFileLink(msg.getMessage(), msg.getFileData(), msg.getUserID());
                break;
        }
    }

    private void appendMessage(String message, String sender) {
        StyledDocument doc = t_display.getStyledDocument();

        try {
            // 새로운 스타일 객체 생성
            SimpleAttributeSet attr = new SimpleAttributeSet();
            StyleConstants.setAlignment(attr, userID.equals(sender) ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);

            // 메시지 색상 설정
            if(sender.equals("System")){
                StyleConstants.setForeground(attr, Color.GRAY);
            } else if (userID.equals(sender)) {
                StyleConstants.setForeground(attr, Color.BLUE); // 내가 보낸 메시지는 파란색
            } else {
                StyleConstants.setForeground(attr, Color.RED); // 다른 사용자의 메시지는 빨간색
            }

            // 메시지 추가
            String header = (userID.equals(sender) ? "나: " : sender + ": ");
            doc.insertString(doc.getLength(), header + message + "\n", attr);
            doc.setParagraphAttributes(doc.getLength() - message.length() - header.length(), message.length() + header.length(), attr, false);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }


    private void appendImage(ImageIcon image, String sender) {
        StyledDocument doc = t_display.getStyledDocument();

        try {
            // 헤더 스타일 설정
            SimpleAttributeSet headerAttr = new SimpleAttributeSet();
            StyleConstants.setAlignment(headerAttr, userID.equals(sender) ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            StyleConstants.setForeground(headerAttr, userID.equals(sender) ? Color.BLUE : Color.RED);

            // 헤더 추가
            String header = userID.equals(sender) ? "나: " : sender + ": ";
            int startHeader = doc.getLength();
            doc.insertString(startHeader, header + "\n", headerAttr);
            doc.setParagraphAttributes(startHeader, header.length(), headerAttr, false);

            // 이미지 크기 조정
            if (image.getIconWidth() > 400) {
                Image scaledImg = image.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
                image = new ImageIcon(scaledImg);
            }

            // 이미지 삽입
            int startImage = doc.getLength();
            t_display.setCaretPosition(startImage);
            t_display.insertIcon(image);

            // 이미지 정렬 적용
            SimpleAttributeSet imageAttr = new SimpleAttributeSet();
            StyleConstants.setAlignment(imageAttr, userID.equals(sender) ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            doc.setParagraphAttributes(startImage, 1, imageAttr, false);

            // 줄바꿈 추가
            doc.insertString(doc.getLength(), "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void showEmojiPicker() {
        JFrame emojiFrame = new JFrame("이모티콘 선택");
        emojiFrame.setSize(300, 200);
        emojiFrame.setLocationRelativeTo(this);
        emojiFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel emojiPanel = new JPanel();
        emojiPanel.setLayout(new GridLayout(1, 5)); // 이모티콘 5개 가로 배열

        String[] emojiFiles = {"dog1.png", "dog2.png", "dog3.png", "dog4.png", "dog5.png"};
        for (String emoji : emojiFiles) {
            // 이모티콘 버튼 생성
            ImageIcon icon = new ImageIcon("image/" + emoji);
            JButton emojiButton = new JButton(new ImageIcon(icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
            emojiButton.setBorder(BorderFactory.createEmptyBorder());
            emojiButton.setContentAreaFilled(false);

            // 이모티콘 버튼 클릭 이벤트
            emojiButton.addActionListener(e -> {
                sendEmoji(emoji);
                emojiFrame.dispose(); // 창 닫기
            });

            emojiPanel.add(emojiButton);
        }

        emojiFrame.add(emojiPanel);
        emojiFrame.setVisible(true);
    }

    private void sendEmoji(String emojiFileName) {
        try {
            // 이모티콘 파일 읽기
            File emojiFile = new File("image/" + emojiFileName);
            byte[] emojiData = Files.readAllBytes(emojiFile.toPath());

            // FileChatMsg 생성 및 전송
            FileChatMsg msg = new FileChatMsg(userID, FileChatMsg.MODE_TX_IMAGE, emojiFileName, emojiData);
            objOut.writeObject(msg);
            objOut.flush();

            // 채팅창에 표시
            appendImage(new ImageIcon(emojiData), msg.getUserID());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "이모티콘 전송 실패: " + e.getMessage());
        }
    }
}
