import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.*;

public class canvasPage extends JFrame {

    private JPanel canvasPanel; // 그림을 그릴 캔버스 패널
    private JButton b_pen, b_eraser, b_black, b_red, b_green, b_blue, b_clear;
    private Color currentColor = Color.BLACK; // 기본 색상: 검정
    private String currentTool = "PEN"; // 기본 도구: 펜
    private int brushSize = 5; // 기본 선 굵기
    private BufferedImage canvasImage; // 캔버스 이미지를 저장

    private Socket socket;
    private PrintWriter out;

    private static int port;

    public canvasPage(int port) {
        super("캔버스");
        this.port = port;
        buildGUI();
        connectToServer();
        setVisible(true);
    }

    private void buildGUI() {
        setSize(600, 600);
        setLocation(1000, 100);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 캔버스 이미지를 초기화
        canvasImage = new BufferedImage(600, 600, BufferedImage.TYPE_INT_ARGB);

        // 캔버스 패널 생성
        canvasPanel = new DrawingPanel();
        canvasPanel.setBackground(Color.WHITE); // 캔버스 배경색 설정
        add(canvasPanel, BorderLayout.CENTER);

        // 버튼 패널 추가
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // 굵기 선택 슬라이더 추가
        JPanel sliderPanel = createSliderPanel();
        add(sliderPanel, BorderLayout.NORTH);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(255, 240, 180));

        // 펜 도구 버튼
        b_pen = new JButton("펜");
        b_pen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = "PEN";
                System.out.println("펜 도구가 선택되었습니다.");
            }
        });

        // 지우개 도구 버튼
        b_eraser = new JButton("지우개");
        b_eraser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentTool = "ERASER";
                System.out.println("지우개 도구가 선택되었습니다.");
            }
        });

        // 색상 변경 버튼 (RGB)
        b_black = new JButton("검정");
        b_black.setBackground(Color.BLACK);
        b_black.setOpaque(true);
        b_black.setBorderPainted(false);
        b_black.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentColor = Color.BLACK;
                System.out.println("선 색상이 검정으로 변경되었습니다.");
            }
        });

        // 색상 변경 버튼 (RGB)
        b_red = new JButton("빨강");
        b_red.setBackground(Color.RED);
        b_red.setOpaque(true);
        b_red.setBorderPainted(false);
        b_red.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentColor = Color.RED;
                System.out.println("선 색상이 빨강으로 변경되었습니다.");
            }
        });

        b_green = new JButton("초록");
        b_green.setBackground(Color.GREEN);
        b_green.setOpaque(true);
        b_green.setBorderPainted(false);
        b_green.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentColor = Color.GREEN;
                System.out.println("선 색상이 초록으로 변경되었습니다.");
            }
        });

        b_blue = new JButton("파랑");
        b_blue.setBackground(Color.BLUE);
        b_blue.setOpaque(true);
        b_blue.setBorderPainted(false);
        b_blue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentColor = Color.BLUE;
                System.out.println("선 색상이 파랑으로 변경되었습니다.");
            }
        });

        // 초기화 버튼
        b_clear = new JButton("초기화");
        b_clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 캔버스 초기화
                clearCanvas();
                // 서버에 초기화 명령 전송
                out.println("CLEAR");
            }
        });

        // 버튼 추가
        panel.add(b_pen);
        panel.add(b_eraser);
        panel.add(b_black);
        panel.add(b_red);
        panel.add(b_green);
        panel.add(b_blue);
        panel.add(b_clear);

        return panel;
    }

    private JPanel createSliderPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(255, 240, 180));

        JLabel sliderLabel = new JLabel("선 굵기: ");
        JSlider brushSizeSlider = new JSlider(5, 20, brushSize); // 최소값 5, 최대값 20, 기본값 현재 굵기
        brushSizeSlider.setMajorTickSpacing(5); // 5 단위로 숫자 표시
        brushSizeSlider.setMinorTickSpacing(1); // 작은 눈금을 1 단위로 표시
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setPaintLabels(true);
        brushSizeSlider.setBackground(new Color(255, 255, 230));

        // 슬라이더 값 변경 이벤트 리스너
        brushSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                brushSize = brushSizeSlider.getValue();
                System.out.println("선 굵기가 변경되었습니다: " + brushSize);
            }
        });

        panel.add(sliderLabel);
        panel.add(brushSizeSlider);

        return panel;
    }

    private void clearCanvas() {
        Graphics2D g2 = canvasImage.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
        g2.dispose();
        canvasPanel.repaint();
    }

    private class DrawingPanel extends JPanel {
        public DrawingPanel() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    drawLine(e.getX(), e.getY(), e.getX(), e.getY(), currentColor, brushSize, currentTool);
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    drawLine(e.getX(), e.getY(), e.getX(), e.getY(), currentColor, brushSize, currentTool);
                    out.println(e.getX() + "," + e.getY() + "," + e.getX() + "," + e.getY() + "," + currentColor.getRGB() + "," + brushSize + "," + currentTool);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(canvasImage, 0, 0, null); // 버퍼에 저장된 이미지 그리기
        }
    }

    private void drawLine(int x1, int y1, int x2, int y2, Color color, int size, String tool) {
        Graphics2D g2 = canvasImage.createGraphics();
        g2.setStroke(new BasicStroke(size));
        g2.setColor(tool.equals("ERASER") ? canvasPanel.getBackground() : color);
        g2.drawLine(x1, y1, x2, y2);
        g2.dispose();
        canvasPanel.repaint();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", port);
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        receiveServerMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패");
            System.exit(0);
        }
    }

    private void receiveServerMessage(String message) {
        if (message.equals("CLEAR")) {
            clearCanvas();
            return;
        }

        String[] parts = message.split(",");
        int x1 = Integer.parseInt(parts[0]);
        int y1 = Integer.parseInt(parts[1]);
        int x2 = Integer.parseInt(parts[2]);
        int y2 = Integer.parseInt(parts[3]);
        Color color = new Color(Integer.parseInt(parts[4]));
        int size = Integer.parseInt(parts[5]);
        String tool = parts[6];

        drawLine(x1, y1, x2, y2, color, size, tool);
    }
}
