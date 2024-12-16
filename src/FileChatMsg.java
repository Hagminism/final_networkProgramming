import java.io.Serializable;

import javax.swing.ImageIcon;

public class FileChatMsg implements Serializable {
    public final static int MODE_LOGIN     =  0x1;  // 로그인 모드
//    public final static int MODE_LOGOUT    =  0x2;
    public final static int MODE_TX_STRING = 0x10;  // 텍스트 메시지 전송
    public final static int MODE_TX_FILE   = 0x20;  // 파일 전송
    public final static int MODE_TX_IMAGE  = 0x40;  // 이미지 전송

    private String userID;     // 송신자 ID
    private int mode;          // 메시지 모드
    private String message;    // 텍스트 메시지
    private ImageIcon image;   // 이미지 객체
    private long size;         // 파일 크기
    private byte[] fileData;   // 파일 데이터

    // 포괄 생성자
    public FileChatMsg(String userID, int code, String message, ImageIcon image, long size, byte[] fileData) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
        this.fileData = fileData;
    }

    // 메세지 전송용 생성자
    public FileChatMsg(String userID, int code, String message) {
        this(userID, code, message, null, 0, null);
    }

    // 이미지 전송용 생성자
    public FileChatMsg(String userID, int code, String message, ImageIcon image, byte[] fileData) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;  // 이미지 객체 저장
        this.size = fileData != null ? fileData.length : 0;  // 파일 크기 저장
        this.fileData = fileData;  // 파일 데이터 저장
    }

    // 파일 전송용 생성자
    public FileChatMsg(String userID, int code, String message, byte[] fileData) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = null;  // 이미지가 아님
        this.size = fileData.length;  // 파일 크기 저장
        this.fileData = fileData;  // 파일 데이터 저장
    }


    // Getter/Setter
    public String getUserID() {
        return userID;
    }

    public int getMode() {
        return mode;
    }

    public String getMessage() {
        return message;
    }

    public byte[] getFileData() {
        return fileData;
    }

    @Override
    public String toString() {
        return "FileChatMsg{" +
                "userID='" + userID + '\'' +
                ", mode=" + mode +
                ", message='" + message + '\'' +
                ", image=" + (image != null ? "Yes" : "No") +
                ", size=" + size +
                ", fileData=" + (fileData != null ? "Yes" : "No") +
                '}';
    }
}
