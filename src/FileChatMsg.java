import java.io.Serializable;

import javax.swing.ImageIcon;

public class FileChatMsg implements Serializable {
    public final static int MODE_LOGIN     =  0x1;  // 로그인 모드
    public final static int MODE_LOGOUT    =  0x2;  // 로그아웃 모드
    public final static int MODE_TX_STRING = 0x10;  // 텍스트 메시지 전송
    public final static int MODE_TX_FILE   = 0x20;  // 파일 전송
    public final static int MODE_TX_IMAGE  = 0x40;  // 이미지 전송

    private String userID;     // 송신자 ID
    private int mode;          // 메시지 모드
    private String message;    // 텍스트 메시지
    private ImageIcon image;   // 이미지 객체
    private long size;         // 파일 크기
    private byte[] fileData;   // 파일 데이터 (추가됨)

    // 다양한 생성자 제공
    public FileChatMsg(String userID, int code, String message, ImageIcon image, long size, byte[] fileData) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
        this.fileData = fileData;
    }

    public FileChatMsg(String userID, int code, String message, ImageIcon image) {
        this(userID, code, message, image, 0, null);
    }

    public FileChatMsg(String userID, int code) {
        this(userID, code, null, null, 0, null);
    }

    public FileChatMsg(String userID, int code, String message) {
        this(userID, code, message, null, 0, null);
    }

    public FileChatMsg(String userID, int code, ImageIcon image) {
        this(userID, code, null, image, 0, null);
    }

    public FileChatMsg(String userID, int code, String filename, long size, byte[] fileData) {
        this(userID, code, filename, null, size, fileData);
    }

    public FileChatMsg(String userID, int code, String message, byte[] fileData) {
        this.userID = userID;
        this.mode = code;
        this.message = message;
        this.image = null;  // 이미지가 아님
        this.size = fileData.length;  // 파일 크기 저장
        this.fileData = fileData;  // 파일 데이터 저장
    }


    // Getter/Setter 추가
    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ImageIcon getImage() {
        return image;
    }

    public void setImage(ImageIcon image) {
        this.image = image;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
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
