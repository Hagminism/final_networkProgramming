import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

public class profilePage extends JFrame {
    private DefaultListModel<Friend> friendListModel;
    private JList<Friend> friendList;
    private JPanel mainPanel, friendPanel, optionPanel;
    private CardLayout cardLayout;
    private JButton btn_Friend, btn_Option, btn_AddFriend, btn_Exit;
    private JLabel myProfilePicLabel, myNameLabel, myStatusMessageLabel;
    private ProfileSocketClient socketClient;
    private String userID;
    private FrameDragListener frameDragListener;

    private JPanel chatRoomListPanel;
    private JButton btn_Chatroom, btn_AddChatRoom;
    private JScrollPane scrollPane;
    private int chatRoomCounter = 0;

    private JPanel notificationPanel;

    private static final Color backgroundColor = new Color(255, 236, 143);
    private static final Color buttonColor = new Color(82, 55, 56);
    private static final String STATUS_FILE_PATH = "status.csv"; // 상태 메시지 CSV 파일 경로

    private String roomname;
    private int roomPort = 5000;

    public profilePage(String userID) {
        super("프로필 및 친구창");
        this.userID = userID;

        // 소켓 연결
        socketClient = new ProfileSocketClient("localhost", 12345, userID);

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
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 닫기 버튼
        createExitButton(panel);

        // 프로필 사진
        myProfilePicLabel = new JLabel(new ImageIcon("images/default_profile.png"));
        myProfilePicLabel.setPreferredSize(new Dimension(80, 80));
        myProfilePicLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));

        // 이름 라벨
        myNameLabel = new JLabel(userID);
        myNameLabel.setFont(new Font("SansSerif", Font.BOLD, 18));

        myStatusMessageLabel = new JLabel("상태 메시지를 불러오는 중...");
        myStatusMessageLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        myStatusMessageLabel.setForeground(Color.GRAY);

        // 텍스트 패널
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(myNameLabel);
        textPanel.add(Box.createVerticalStrut(15));
        textPanel.add(myStatusMessageLabel);

        panel.add(myProfilePicLabel, BorderLayout.WEST);
        panel.add(textPanel, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createFriendList() {
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setCellRenderer(new FriendListCellRenderer());

//        // 빈 리스트 메시지 설정
//        if (friendListModel.isEmpty()) {
//            friendListModel.addElement(new Friend("친구 없음", false));
//        }

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
        img = img.getScaledInstance(25,25,Image.SCALE_DEFAULT);
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

    private void updateFriendList(String statusMessage) {
        String[] entries = statusMessage.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            String friendID = parts[0].trim();
            boolean isOnline = Boolean.parseBoolean(parts[1].trim());

            boolean found = false;
            for (int i = 0; i < friendListModel.size(); i++) {
                Friend friend = friendListModel.get(i);
                if (friend.name.equals(friendID)) {
                    friend.isOnline = isOnline;
                    found = true;
                    break;
                }
            }
            if (!found) {
                friendListModel.addElement(new Friend(friendID, statusMessage, isOnline));
            }
        }
        friendList.repaint();
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
                            new chattingPage("localhost", port); // 서버와 연결
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
                    socketClient.sendCommand("CHAT_INVITE:" + selectedFriendsList + ":" + currentRoomName + ":" +
                            SocketServer.roomPortMap.get(currentRoomName)); // 친구 목록과 채팅방 이름, 포트번호를 함께 보내기

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
    private void showNotification(String alertMessage) {
        // 새로운 알림을 패널로 만들어 추가
        JPanel notificationItem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        notificationItem.setBackground(new Color(255, 240, 180));

        JLabel notificationLabel = new JLabel(alertMessage);
        notificationLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton acceptButton = new JButton("수락");
        acceptButton.addActionListener(e -> acceptInvitation(alertMessage, notificationItem));

        JButton declineButton = new JButton("거절");
        declineButton.addActionListener(e -> declineInvitation(alertMessage));

        // 버튼들을 한 패널에 추가
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(255, 240, 180));
        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        // 알림 내용과 버튼 패널을 notificationItem에 추가
        notificationItem.add(notificationLabel);
        notificationItem.add(buttonPanel);

        // notificationPanel에 추가 (알림들을 계속해서 추가할 수 있도록)
        notificationPanel.add(notificationItem);
        notificationPanel.revalidate(); // 레이아웃 갱신
        notificationPanel.repaint();   // 화면 갱신
    }

    private void acceptInvitation(String alertMessage, JPanel notificationItem) {
        // 초대 메시지에서 채팅방 이름과 포트 추출
        String roomName = alertMessage.replaceAll(".*채팅방 '([^']*)'\\(포트: \\d+\\)에 초대.*", "$1");
        int port = Integer.parseInt(alertMessage.replaceAll(".*포트: (\\d+)\\).*", "$1"));

        // 채팅방 추가 처리
        addChatRoomByInvite(roomName, port); // 포트를 함께 전달하도록 수정

        // 수락한 알림은 삭제
        notificationPanel.remove(notificationItem);
        notificationPanel.revalidate(); // 레이아웃 갱신
        notificationPanel.repaint();    // 화면 갱신
    }




    private void declineInvitation(String alertMessage) {
        // 알림을 거절했을 때 처리 로직
        // 예: 아무 작업도 하지 않음
        System.out.println("초대를 거절했습니다: " + alertMessage);
    }

    private void addChatRoomByInvite(String roomName, int port) {
        System.out.println(roomName + " 포트 : " + port);

        // 새로운 채팅방 생성
        JPanel chatRoom = new JPanel();
        chatRoom.setLayout(new BorderLayout());
        chatRoom.setPreferredSize(new Dimension(360, 60));
        chatRoom.setMaximumSize(new Dimension(360, 60)); // 고정 크기
        chatRoom.setBackground(buttonColor);

        // 채팅방 이름
        JLabel chatRoomLabel = new JLabel(roomName); // 채팅방 이름은 초대받은 이름 그대로
        chatRoomLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        chatRoomLabel.setForeground(Color.WHITE);
        chatRoomLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        chatRoom.add(chatRoomLabel, BorderLayout.WEST);

        // 채팅방 이름을 chatRoom에 저장 (chatRoom 자체에 roomname 추가)
        chatRoom.putClientProperty("roomname", roomName);

        System.out.println(roomName + " / " + port);

        // 팝업 메뉴 생성
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem deleteChatRoomMenuItem = new JMenuItem("삭제"); // 삭제 메뉴 추가
        popupMenu.add(deleteChatRoomMenuItem);

        // 우클릭 이벤트
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
                    System.out.println(roomName + " / " + port);
                    // 서버가 시작될 때까지 기다린 후 연결 시도
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // 서버 시작 대기 (예: 1초)
                            new chattingPage("localhost", port); // 서버와 연결
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                }
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
        boolean isOnline;
        String statusMessage;

        public Friend(String name, String statusMessage, boolean isOnline) {
            this.name = name;
            this.statusMessage = statusMessage;
            this.isOnline = isOnline;
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
            profilePicLabel = new JLabel(new ImageIcon("images/default_profile.png"));
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
            nameLabel.setText(friend.name);
            statusMessageLabel.setText(friend.statusMessage);

            if (friend.isOnline) {
                onlineMarker.setBackground(Color.GREEN);
            } else {
                onlineMarker.setBackground(Color.RED);
            }

            setBackground(isSelected ? new Color(220, 220, 220) : Color.WHITE);

            return this;
        }
    }

    private class ProfileSocketClient {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        ProfileSocketClient(String serverAddress, int serverPort, String userID) {
            try {
                socket = new Socket(serverAddress, serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println("LOGIN:" + userID);
                // ProfileSocketClient 클래스에서 메시지 수신 부분 수정
                new Thread(() -> {
                    try {
                        String message;
                        while ((message = in.readLine()) != null) {
                            if (message.startsWith("STATUS:")) {
                                String status = message.substring(7);
                                SwingUtilities.invokeLater(() -> updateFriendList(status));
                            } else if (message.startsWith("ALERT:")) { // 알림 메시지 처리
                                String alertMessage = message.substring(6); // "ALERT:" 이후의 메시지
                                SwingUtilities.invokeLater(() -> showNotification(alertMessage)); // 알림 표시
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
}
