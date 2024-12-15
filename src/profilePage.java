import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import javax.swing.*;

public class profilePage extends JFrame {
    private DefaultListModel<Friend> friendListModel;
    private JList<Friend> friendList;
    private JPanel mainPanel, friendPanel, optionPanel;
    private CardLayout cardLayout;
    private JButton btn_Friend, btn_Option, btn_AddFriend, btn_Exit;
    private JLabel myProfilePicLabel, myNameLabel, myStatusMessageLabel;
    private ProfileSocketClient socketClient;
    private Socket socket; // 클라이언트 소켓
    private String userID;
    private FrameDragListener frameDragListener;
    private int roomPort = 5000;

    private JPanel chatRoomListPanel;
    private JButton btn_Chatroom, btn_AddChatRoom;
    private JScrollPane scrollPane;
    private int chatRoomCounter = 0;

    private JPanel notificationPanel;

    private static final Color backgroundColor = new Color(255, 236, 143);
    private static final Color buttonColor = new Color(82, 55, 56);
    private static final String STATUS_FILE_PATH = "userStatusMessage.csv"; // 상태 메시지 CSV 파일 경로

    public profilePage(String userID, Socket socket) {
        super("프로필 및 친구창");
        this.userID = userID;
        this.socketClient = new ProfileSocketClient(socket, userID); // 전달된 소켓 활용

        // 드래그 기능 활성화
        frameDragListener = new FrameDragListener(this);
        addMouseListener(frameDragListener);
        addMouseMotionListener(frameDragListener);

        // UI 설정
        setUndecorated(true);
        setSize(400, 600);
        setLocation(1000, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildGUI();
        loadUserStatusMessage(); // 상태 메시지 로드

        setVisible(true);
    }



    public profilePage(String userID) {
        super("프로필 및 친구창");
        this.userID = userID;

        // 소켓 연결
        socketClient = new ProfileSocketClient(socket, userID);

        // 드래그 기능 활성화
        frameDragListener = new FrameDragListener(this);
        addMouseListener(frameDragListener);
        addMouseMotionListener(frameDragListener);

        // UI 설정
        setUndecorated(true);
        setSize(400, 600);
        setLocation(1000, 100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildGUI();
        loadUserStatusMessage(); // 상태 메시지 로드

        setVisible(true);
    }

    private void buildGUI() {
        // CardLayout 설정
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 친구 패널
        friendPanel = new JPanel(new BorderLayout());
        friendPanel.add(createMyProfilePanel(), BorderLayout.NORTH);
        friendPanel.add(createFriendList(), BorderLayout.CENTER);

        // 옵션 패널 (빈 상태로 유지)
        optionPanel = new JPanel();
        optionPanel.setBackground(new Color(255, 240, 180));
        optionPanel.add(NotificationPanel());

        // 하단 네비게이션 바
        JPanel bottomPanel = createBottomPanel();

        // 메인 패널에 추가
        mainPanel.add(friendPanel, "FRIEND");
        mainPanel.add(createChatRoomPanel(), "CHATROOM"); // 채팅방 화면
        mainPanel.add(optionPanel, "OPTION");

        add(mainPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadUserStatusMessage() {
        String statusMessage = readStatusMessageFromCSV(userID);
        if (statusMessage != null) {
            myStatusMessageLabel.setText(statusMessage);
        } else {
            myStatusMessageLabel.setText("상태 메시지가 없습니다.");
        }
    }

    private String readStatusMessageFromCSV(String userID) {
        try (BufferedReader br = new BufferedReader(new FileReader(STATUS_FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("Read line: " + line); // 디버깅 출력
                String[] data = line.split(",");
                if (data.length == 2 && data[0].trim().equals(userID)) {
                    return data[1].trim(); // 상태 메시지 반환
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // 상태 메시지를 찾지 못한 경우
    }

    private JPanel createMyProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(255, 240, 180));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 30));

        // 닫기 버튼
        createExitButton(panel);

        // 프로필 사진
        myProfilePicLabel = new JLabel(new ImageIcon("images/default_profile.png"));
        myProfilePicLabel.setPreferredSize(new Dimension(80, 80));
        myProfilePicLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        // 프로필 사진 클릭 이벤트 추가
        myProfilePicLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) { // 왼쪽 클릭 확인
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("프로필 사진 선택");
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                            "이미지 파일", "jpg", "jpeg", "png", "gif"));

                    int returnValue = fileChooser.showOpenDialog(null);
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = fileChooser.getSelectedFile();
                        String filePath = selectedFile.getAbsolutePath();

                        // 선택한 이미지를 JLabel에 표시
                        myProfilePicLabel.setIcon(new ImageIcon(new ImageIcon(filePath).getImage()
                                .getScaledInstance(80, 80, Image.SCALE_DEFAULT)));

                        // 파일 경로를 저장하거나 서버에 업로드하는 로직 추가 가능
                        saveProfilePicturePath(filePath);
                    }
                }
            }
        });

        // 이름 라벨
        myNameLabel = new JLabel(userID);
        myNameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        // 이름 클릭 이벤트 추가
        myNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    String newName = JOptionPane.showInputDialog("새 이름을 입력하세요:", myNameLabel.getText());
                    if (newName != null && !newName.trim().isEmpty()) {
                        myNameLabel.setText(newName);
                        socketClient.sendCommand("UPDATE_NAME:" + newName);
                    }
                }
            }
        });

        myStatusMessageLabel = new JLabel("상태 메시지를 불러오는 중...");
        myStatusMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        myStatusMessageLabel.setForeground(Color.GRAY);

        // 상태 메시지 클릭 이벤트 추가
        myStatusMessageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    String newStatus = JOptionPane.showInputDialog("새 상태 메시지를 입력하세요:", myStatusMessageLabel.getText());
                    if (newStatus != null && !newStatus.trim().isEmpty()) {
                        myStatusMessageLabel.setText(newStatus);
                        socketClient.sendCommand("UPDATE_STATUS:" + newStatus);
                    }
                }
            }
        });

        // 텍스트 패널
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(myNameLabel);
        textPanel.add(Box.createVerticalStrut(15));
        textPanel.add(myStatusMessageLabel);

        btn_AddFriend = new JButton("추가");
        btn_AddFriend.setBackground(new Color(200, 255, 200));
        panel.add(myProfilePicLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(btn_AddFriend, BorderLayout.EAST);

        btn_AddFriend.addActionListener(e -> {
            String friendID = JOptionPane.showInputDialog(this, "추가할 친구의 ID를 입력하세요:");
            if (isFriendAlready(friendID)){
                JOptionPane.showMessageDialog(this, "이미 친구입니다.");
            }

            else if (friendID != null && !friendID.trim().isEmpty()) {
                socketClient.sendCommand("ADD_FRIEND:" + friendID.trim()); // 서버로 친구 요청 전송
                JOptionPane.showMessageDialog(this, friendID + "님에게 친구 요청을 보냈습니다.");
                //updateUserList("", userID);
            }
        });

        return panel;
    }

    // 프로필 사진 경로 저장 메서드
    private void saveProfilePicturePath(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("profilePicturePath.txt"))) {
            writer.write(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JScrollPane createFriendList() {
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setCellRenderer(new FriendListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(friendList);
        scrollPane.setBackground(new Color(255, 240, 180));
        return scrollPane;
    }


    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3));
        bottomPanel.setPreferredSize(new Dimension(380, 60));
        bottomPanel.setBackground(buttonColor);

        // 친구 버튼
        btn_Friend = createBottomButton("image/Friend.png");
        btn_Friend.addActionListener(e -> cardLayout.show(mainPanel, "FRIEND"));

        // 채팅방 버튼
        btn_Chatroom = createBottomButton("image/ChatRoom.png");
        btn_Chatroom.addActionListener(e -> cardLayout.show(mainPanel, "CHATROOM"));

        // 옵션 버튼
        btn_Option = createBottomButton("image/Option.png");
        btn_Option.addActionListener(e -> cardLayout.show(mainPanel, "OPTION"));

        // 버튼 추가
        bottomPanel.add(btn_Friend);
        bottomPanel.add(btn_Chatroom);
        bottomPanel.add(btn_Option);

        return bottomPanel;
    }

    private JButton createBottomButton(String imageSrc) {
        JButton button = new JButton(new ImageIcon(imageSrc));
        button.setBackground(new Color(82, 55, 56));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        return button;
    }

    private void createExitButton(JPanel panel) {
        Image img = Toolkit.getDefaultToolkit().getImage("image/Exit_btn.png");
        img = img.getScaledInstance(25, 25, Image.SCALE_DEFAULT);
        Icon icon = new ImageIcon(img);
        btn_Exit = new JButton(icon);
        btn_Exit.setBounds(375, 0, 25, 25);
        btn_Exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        panel.add(btn_Exit);
    }

    private void addFriend() {
        String friendID = JOptionPane.showInputDialog(this, "추가할 친구의 ID를 입력하세요:", "친구 추가", JOptionPane.PLAIN_MESSAGE);
        if (friendID != null && !friendID.trim().isEmpty()) {
            socketClient.sendCommand("ADD_FRIEND:" + friendID);
        }
    }

    private JPanel createChatRoomPanel() {
        JPanel chatRoomPanel = new JPanel(null); // null 레이아웃 유지
        chatRoomPanel.setBackground(backgroundColor);
        createExitButton(chatRoomPanel); // 패널에 종료 버튼 추가

        // 채팅방 추가 버튼
        Image img = Toolkit.getDefaultToolkit().getImage("image/addChatRoom.png");
        img = img.getScaledInstance(30, 30, Image.SCALE_DEFAULT);
        Icon icon = new ImageIcon(img);
        btn_AddChatRoom = new JButton(icon);
        btn_AddChatRoom.setBounds(320, 40, 30, 30); // 위치와 크기 설정
        btn_AddChatRoom.addActionListener(e -> addChatRoom());
        chatRoomPanel.add(btn_AddChatRoom);

        // 채팅방 목록 패널 생성
        chatRoomListPanel = new JPanel();
        chatRoomListPanel.setLayout(new BoxLayout(chatRoomListPanel, BoxLayout.Y_AXIS)); // 세로 정렬
        chatRoomListPanel.setBackground(backgroundColor);

        // 채팅방 목록 스크롤
        scrollPane = new JScrollPane(chatRoomListPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBounds(17, 80, 345, 500); // 스크롤 패널 위치 및 크기 설정
        scrollPane.setBorder(null); // 테두리 제거
        chatRoomPanel.add(scrollPane);

        return chatRoomPanel;
    }

    private int getAvailablePort() {
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(roomPort)) {
                return roomPort; // 포트가 사용되지 않으면 리턴
            } catch (IOException e) {
                roomPort++; // 포트가 사용 중이면 다음 포트로
            }
        }
    }

    private void addChatRoom() {
        // 새로운 채팅방 생성
        JPanel chatRoom = new JPanel();
        chatRoom.setLayout(new BorderLayout());
        chatRoom.setPreferredSize(new Dimension(360, 60));
        chatRoom.setMaximumSize(new Dimension(360, 60)); // 고정 크기
        chatRoom.setBackground(buttonColor);

        // 채팅방 이름
        String roomName = "Chat Room " + (++chatRoomCounter);
        JLabel chatRoomLabel = new JLabel(roomName);
        chatRoomLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        chatRoomLabel.setForeground(Color.WHITE);
        chatRoomLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatRoom.add(chatRoomLabel, BorderLayout.WEST);

        // 채팅방 이름을 chatRoom에 저장 (chatRoom 자체에 roomname 추가)
        chatRoom.putClientProperty("roomname", roomName);
        int port = getAvailablePort(); // 고유한 포트 번호 할당
        SocketServer.roomPortMap.put(roomName, port);
        System.out.println("저장된 값 : " + SocketServer.roomPortMap.get(roomName));

        // 서버 실행 (새로운 채팅방을 위해 고유한 포트 사용)
        new Thread(() -> {
            // ChatServer는 해당 포트로 서버 시작
            new ChatServer(port); // 서버 시작
        }).start();

        // 팝업 메뉴 생성
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editTitleMenuItem = new JMenuItem("방 제목 수정");
        popupMenu.add(editTitleMenuItem);
        JMenuItem addChatMember = new JMenuItem("초대");
        popupMenu.add(addChatMember);
        JMenuItem deleteChatRoomMenuItem = new JMenuItem("삭제"); // 삭제 메뉴 추가
        popupMenu.add(deleteChatRoomMenuItem);

        chatRoom.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(chatRoom, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(chatRoom, e.getX(), e.getY());
                }
            }

            // 더블클릭 이벤트
            @Override
            public void mouseClicked(MouseEvent e) {
                // 더블클릭 감지
                if (e.getClickCount() == 2) {
                    // chatRoom에서 roomname을 가져옴
                    String roomName = (String) chatRoom.getClientProperty("roomname");
                    int port = SocketServer.roomPortMap.get(roomName);
                    System.out.println(roomName + " / " + port);
                    // 서버가 시작될 때까지 기다린 후 연결 시도
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // 서버 시작 대기 (예: 1초)
                            new chattingPage("localhost", port, roomName); // 서버와 연결
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
            }
        });

        editTitleMenuItem.addActionListener(e -> {
            String oldTitle = (String) chatRoom.getClientProperty("roomname"); // 기존 방 제목
            String newTitle = JOptionPane.showInputDialog(chatRoom, "새로운 방 제목:", chatRoomLabel.getText());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                // UI 업데이트
                chatRoomLabel.setText(newTitle);
                chatRoom.putClientProperty("roomname", newTitle); // chatRoom에 새로운 방 제목 저장

                // SocketServer.roomPortMap 업데이트
                int getPort = SocketServer.roomPortMap.get(oldTitle); // 기존 포트 번호 가져오기
                SocketServer.roomPortMap.remove(oldTitle); // 기존 항목 삭제
                SocketServer.roomPortMap.put(newTitle, getPort); // 새 제목과 포트 매핑
                System.out.println("방 제목 변경: " + oldTitle + " -> " + newTitle);

                socketClient.sendCommand("TITLE_CHANGE:" + oldTitle + ":" + newTitle + ":" + getPort);
            }
        });

        // 삭제 기능
        deleteChatRoomMenuItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(chatRoom, "이 채팅방을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // 채팅방 이름 가져오기
                String DeleteroomName = (String) chatRoom.getClientProperty("roomname");

                // 채팅방 패널 제거
                chatRoomListPanel.remove(chatRoom);

                // SocketServer.roomPortMap에서 데이터 제거
                SocketServer.roomPortMap.remove(DeleteroomName);

                // 필요하면 서버 소켓 종료 로직 추가
                // 예: ChatServer.stopServer(port);

                // UI 업데이트
                chatRoomListPanel.revalidate();
                chatRoomListPanel.repaint();

                System.out.println("채팅방 삭제됨: " + DeleteroomName);
            }
        });


        // 초대 기능
        addChatMember.addActionListener(e -> {
            JFrame selectFriendsFrame = new JFrame("친구 초대");
            selectFriendsFrame.setSize(300, 400);
            selectFriendsFrame.setLocationRelativeTo(null);

            JPanel friendSelectionPanel = new JPanel();
            friendSelectionPanel.setLayout(new BoxLayout(friendSelectionPanel, BoxLayout.Y_AXIS));

            // 친구 목록 출력
            JScrollPane scrollPane = new JScrollPane(friendSelectionPanel);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            JCheckBox[] friendCheckBoxes = new JCheckBox[friendListModel.size()];
            for (int i = 0; i < friendListModel.size(); i++) {
                Friend friend = friendListModel.get(i);
                JCheckBox checkBox = new JCheckBox(friend.name);
                friendCheckBoxes[i] = checkBox;
                friendSelectionPanel.add(checkBox);
            }

            // 확인 버튼 추가
            JButton confirmButton = new JButton("확인");
            confirmButton.addActionListener(confirmEvent -> {
                StringBuilder selectedFriends = new StringBuilder();
                for (JCheckBox checkBox : friendCheckBoxes) {
                    if (checkBox.isSelected()) {
                        if (selectedFriends.length() > 0) {
                            selectedFriends.append(", ");
                        }
                        selectedFriends.append(checkBox.getText());
                    }
                }

                // 최신 방 이름 가져오기
                String currentRoomName = (String) chatRoom.getClientProperty("roomname");

                if (selectedFriends.length() > 0 && currentRoomName != null && !currentRoomName.isEmpty()) {
                    JOptionPane.showMessageDialog(selectFriendsFrame,
                            "선택된 친구: " + selectedFriends.toString() +
                                    "\n채팅방 이름: " + currentRoomName +
                                    "\n포트: " + SocketServer.roomPortMap.get(currentRoomName));

                    // 서버로 초대 요청 보내기
                    String selectedFriendsList = selectedFriends.toString();
                    String[] friends = selectedFriendsList.split(", ");
                    for(String friend : friends){
                        socketClient.sendCommand("CHAT_INVITE:" + friend + ":" + currentRoomName + ":" + SocketServer.roomPortMap.get(currentRoomName)); // 친구 목록과 채팅방 이름, 포트번호를 함께 보내기
                    }
                } else {
                    JOptionPane.showMessageDialog(selectFriendsFrame, "선택된 친구 또는 채팅방 이름이 없습니다.");
                }

                selectFriendsFrame.dispose(); // 창 닫기
            });

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(confirmButton);

            selectFriendsFrame.add(scrollPane, BorderLayout.CENTER);
            selectFriendsFrame.add(buttonPanel, BorderLayout.SOUTH);

            selectFriendsFrame.setVisible(true);
        });



        // 채팅방 패널에 추가
        chatRoomListPanel.add(chatRoom);
        chatRoomListPanel.revalidate();
        chatRoomListPanel.repaint();
    }


    private JPanel NotificationPanel() {
        notificationPanel = new JPanel();
        notificationPanel.setBackground(new Color(255, 240, 180));
        notificationPanel.setLayout(new BoxLayout(notificationPanel, BoxLayout.Y_AXIS));

        JLabel notificationLabel = new JLabel("알림 없음");
        notificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        notificationPanel.add(notificationLabel);
        notificationPanel.setVisible(true); // 기본적으로 숨김

        return notificationPanel;
    }

    // 알림 표시 메소드 추가
    private void showFriendNotification(String alertMessage, String requesterID) {
        JPanel notificationItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notificationItem.setBackground(new Color(255, 240, 180));

        JLabel notificationLabel = new JLabel(alertMessage);
        notificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton acceptButton = new JButton("수락");
        acceptButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFriendshipToCSV(userID, requesterID);

                notificationPanel.remove(notificationItem);
                notificationPanel.revalidate(); // 레이아웃 갱신
                notificationPanel.repaint();    // 화면 갱신
            }
        });

        JButton declineButton = new JButton("거절");
        declineButton.addActionListener(e -> declineInvitation(alertMessage, notificationItem));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(255, 240, 180));
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        notificationItem.add(notificationLabel);
        notificationItem.add(buttonPanel);

        notificationPanel.add(notificationItem);
        notificationPanel.revalidate();
        notificationPanel.repaint();
    }

    // 알림 표시 메소드 추가
    private void showNotification(String alertMessage) {
        JPanel notificationItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notificationItem.setBackground(new Color(255, 240, 180));

        String[] parts = alertMessage.split(":");
        JLabel notificationLabel = new JLabel(parts[1] + "(포트 : " + parts[2] + ")에 초대되었습니다.");
        notificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton acceptButton = new JButton("수락");
        acceptButton.addActionListener(e -> acceptInvitation(alertMessage, notificationItem)
        );

        JButton declineButton = new JButton("거절");
        declineButton.addActionListener(e -> declineInvitation(alertMessage, notificationItem));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(255, 240, 180));
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        notificationItem.add(notificationLabel);
        notificationItem.add(buttonPanel);

        notificationPanel.add(notificationItem);
        notificationPanel.revalidate();
        notificationPanel.repaint();
    }

    private boolean isFriendAlready(String friendID) {
        String filePath = "friendships.csv";
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if ((parts[0].equals(userID) && parts[1].equals(friendID)) ||
                        (parts[0].equals(friendID) && parts[1].equals(userID))) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void saveFriendshipToCSV(String user1, String user2) {
        String filePath = "friendships.csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(user1 + "," + user2);
            writer.newLine();
            writer.write(user2 + "," + user1);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private void acceptInvitation(String alertMessage, JPanel notificationItem) {
        try {
            // 메시지 형식: "user2:Chat Room 1:5000"
            String[] parts = alertMessage.split(":");
            if (parts.length != 3) {
                System.err.println("초대 메시지 형식이 잘못되었습니다: " + alertMessage);
                return;
            }

            // 메시지에서 필요한 정보 추출
            String roomName = parts[1].trim(); // 채팅방 이름
            int port = Integer.parseInt(parts[2].trim()); // 포트 번호

            // 채팅방 추가 로직 실행
            addChatRoomByInvite(roomName, port);

            // 알림 제거
            notificationPanel.remove(notificationItem);
            notificationPanel.revalidate();
            notificationPanel.repaint();
        } catch (Exception e) {
            System.err.println("초대 메시지 처리 중 오류 발생: " + e.getMessage());
        }
    }



    private void declineInvitation(String alertMessage, JPanel notificationItem) {
        // 알림을 거절했을 때 처리 로직
        // 예: 아무 작업도 하지 않음
        System.out.println("초대를 거절했습니다: " + alertMessage);
        // 알림 제거
        notificationPanel.remove(notificationItem);
        notificationPanel.revalidate();
        notificationPanel.repaint();
    }

    private void addChatRoomByInvite(String roomName, int port) {
        JPanel chatRoom = new JPanel();
        chatRoom.setLayout(new BorderLayout());
        chatRoom.setPreferredSize(new Dimension(360, 60));
        chatRoom.setMaximumSize(new Dimension(360, 60)); // 고정 크기
        chatRoom.setBackground(buttonColor);

        // 채팅방 이름
        JLabel chatRoomLabel = new JLabel(roomName);
        chatRoomLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        chatRoomLabel.setForeground(Color.WHITE);
        chatRoomLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatRoom.add(chatRoomLabel, BorderLayout.WEST);

        // **추가 부분: roomName을 chatRoom에 저장**
        chatRoom.putClientProperty("roomname", roomName);

        // 초대받은 채팅방에 대해 port 정보도 저장
        SocketServer.roomPortMap.put(roomName, port);

        // 팝업 메뉴 생성
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem editTitleMenuItem = new JMenuItem("방 제목 수정");
        popupMenu.add(editTitleMenuItem);
        JMenuItem deleteChatRoomMenuItem = new JMenuItem("삭제"); // 삭제 메뉴 추가
        popupMenu.add(deleteChatRoomMenuItem);

        chatRoom.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(chatRoom, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(chatRoom, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // 더블클릭 감지
                if (e.getClickCount() == 2) {
                    String roomName = (String) chatRoom.getClientProperty("roomname");
                    System.out.println("전달받은 roomName 값은 " + roomName);
                    if (roomName == null) {
                        JOptionPane.showMessageDialog(chatRoom, "채팅방 정보가 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    Integer port = SocketServer.roomPortMap.get(roomName);
                    if (port == null) {
                        JOptionPane.showMessageDialog(chatRoom, "채팅방 포트 정보가 없습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    System.out.println(roomName + " / " + port);
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // 서버 시작 대기 (예: 1초)
                            new chattingPage("localhost", port, roomName); // 서버와 연결
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
            }

        });

        // 방 이름 수정
        editTitleMenuItem.addActionListener(e -> {
            String oldTitle = (String) chatRoom.getClientProperty("roomname"); // 기존 방 제목
            String newTitle = JOptionPane.showInputDialog(chatRoom, "새로운 방 제목:", chatRoomLabel.getText());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                // UI 업데이트
                chatRoomLabel.setText(newTitle);
                chatRoom.putClientProperty("roomname", newTitle); // chatRoom에 새로운 방 제목 저장

                // SocketServer.roomPortMap 업데이트
                int getPort = SocketServer.roomPortMap.get(oldTitle); // 기존 포트 번호 가져오기
                SocketServer.roomPortMap.remove(oldTitle); // 기존 항목 삭제
                SocketServer.roomPortMap.put(newTitle, getPort); // 새 제목과 포트 매핑
                System.out.println("방 제목 변경: " + oldTitle + " -> " + newTitle);

                socketClient.sendCommand("TITLE_CHANGE:" + oldTitle + ":" + newTitle + ":" + getPort);
            }
        });

        // 삭제 기능
        deleteChatRoomMenuItem.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(chatRoom, "이 채팅방을 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // 채팅방 이름 가져오기
                String DeleteroomName = (String) chatRoom.getClientProperty("roomname");

                // 채팅방 패널 제거
                chatRoomListPanel.remove(chatRoom);

                // SocketServer.roomPortMap에서 데이터 제거
                SocketServer.roomPortMap.remove(DeleteroomName);

                // 필요하면 서버 소켓 종료 로직 추가
                // 예: ChatServer.stopServer(port);

                // UI 업데이트
                chatRoomListPanel.revalidate();
                chatRoomListPanel.repaint();

                System.out.println("채팅방 삭제됨: " + DeleteroomName);
            }
        });

        // 채팅방 패널에 추가
        chatRoomListPanel.add(chatRoom);
        chatRoomListPanel.revalidate();
        chatRoomListPanel.repaint();
    }

    private class Friend {
        String name;
        String statusMessage;
        boolean isOnline;

        public Friend(String name, String statusMessage, boolean isOnline) {
            this.name = name;
            this.statusMessage = statusMessage;
            this.isOnline = isOnline;
        }

        @Override
        public String toString() {
            return name; // JList에는 이름만 표시
        }

        public String getFullInfo() {
            return name + ": " + statusMessage; // 전체 정보는 필요시 호출
        }
    }


    private static class FriendListCellRenderer extends JPanel implements ListCellRenderer<Friend> {
        private JLabel profilePicLabel;
        private JLabel nameLabel;
        private JLabel statusMessageLabel;
        private JLabel onlineMarker;

        public FriendListCellRenderer() {
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            // 프로필 사진
            profilePicLabel = new JLabel(new ImageIcon("image/dog1.png"));
            profilePicLabel.setPreferredSize(new Dimension(50, 50));

            // 이름과 상태 메시지
            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            nameLabel = new JLabel();
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            statusMessageLabel = new JLabel();
            statusMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            statusMessageLabel.setForeground(Color.GRAY);
            textPanel.add(nameLabel);
            textPanel.add(statusMessageLabel);

            // 접속 상태 표시
            onlineMarker = new JLabel();
            onlineMarker.setOpaque(true);
            onlineMarker.setPreferredSize(new Dimension(10, 10));
            onlineMarker.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

            add(profilePicLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(onlineMarker, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Friend> list, Friend friend, int index, boolean isSelected, boolean cellHasFocus) {
            nameLabel.setText(friend.name); // 이름만 표시
            statusMessageLabel.setText(friend.statusMessage); // 상태 메시지는 아래 표시

            // 온라인 상태에 따라 색상 설정
            if (friend.isOnline) {
                onlineMarker.setBackground(Color.GREEN); // 온라인은 녹색
            } else {
                onlineMarker.setBackground(Color.RED); // 오프라인은 회색
            }

            // 선택 여부에 따라 색상 설정
            if (isSelected) {
                setBackground(new Color(220, 220, 220));
            } else {
                setBackground(Color.WHITE);
            }
            return this;
        }
    }

    private class ProfileSocketClient {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String currentUserID; // 현재 사용자 ID

        ProfileSocketClient(Socket socket, String userID) {
            try {
                this.socket = socket;
                this.currentUserID = userID;
                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 서버로 로그인 알림 전송
                out.println("LOGIN:" + userID);

                // 기존에는 STATUS용, FRIEND_REQUEST용으로 스레드가 2개였으나 이를 1개로 통합
                new Thread(() -> {
                    try {
                        String message;
                        while ((message = in.readLine()) != null) {
                            System.out.println("서버 측에서 온 메시지 : " + message);
                            if (message.startsWith("STATUS:")) {
                                // 상태 업데이트 메시지 처리
                                String userList = message.substring(7);
                                SwingUtilities.invokeLater(() -> updateUserList(userList));
                            } else if (message.startsWith("FRIEND_REQUEST:")) {
                                // 친구 요청 메시지 처리
                                String requesterID = message.substring(15);
                                SwingUtilities.invokeLater(() -> {
                                    showFriendNotification(requesterID + "님이 친구 요청을 보냈습니다.", requesterID);
                                });
                            } else if (message.startsWith("CHAT_INVITE:")) {
                                // 채팅방 초대 메시지 처리
                                String chatroom = message.substring(12);
                                String[] parts = chatroom.split(":");
                                SwingUtilities.invokeLater(() -> {
                                    showNotification(chatroom);
                                });
                            }
                            else {
                                // 그 외 메시지 처리 필요시 여기서 처리
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void sendCommand(String command) {
            if (out != null) {
                out.println(command);
            }
        }
    }

    private void updateUserList(String statusMessage) {
        System.out.println(statusMessage); // 디버깅용 출력
        String[] entries = statusMessage.split(","); // 여러 친구 정보 ','로 분리

        for (String entry : entries) {
            String[] parts = entry.split(":", 2); // "아이디:상태메시지" 분리
            if (parts.length == 2) { // 데이터가 올바르게 형식화된 경우만 처리
                String friendID = parts[0].trim();   // 친구의 아이디
                String statusMsg = parts[1].trim(); // 친구의 상태 메시지

                // 본인 아이디는 제외
                if (friendID.equals(userID)) {
                    continue;
                }

                boolean found = false;
                // 이미 존재하는 친구인지 확인
                for (int i = 0; i < friendListModel.size(); i++) {
                    Friend friend = friendListModel.get(i);
                    if (friend.name.equals(friendID)) {
                        // 이미 존재하면 상태 메시지만 업데이트
                        friend.statusMessage = statusMsg;
                        found = true;
                        break;
                    }
                }

                // 친구가 목록에 없으면 새로 추가
                if (!found) {
                    friendListModel.addElement(new Friend(friendID, statusMsg, true));
                }
            }
        }
        friendList.repaint(); // UI 갱신
    }
}
