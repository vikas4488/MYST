package com.vikas.myst.bean;

import java.util.Objects;

public class Chat {

    private long id;
    private String date;
    private String from;
    private String msg;
    private String status;
    private String time;
    private String friend;
    private String msgType;


    public Chat() {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chat)) return false;
        Chat chat = (Chat) o;
        return getDate().equals(chat.getDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDate());
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getFriend() {
        return friend;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    @Override
    public String toString() {
        return "Chat{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", from='" + from + '\'' +
                ", msg='" + msg + '\'' +
                ", status='" + status + '\'' +
                ", time='" + time + '\'' +
                ", friend='" + friend + '\'' +
                ", msgType='" + msgType + '\'' +
                '}';
    }

    public Chat(long id, String date, String from, String msg, String status, String time, String friend, String msgType) {
        this.id = id;
        this.date = date;
        this.from = from;
        this.msg = msg;
        this.status = status;
        this.time = time;
        this.friend = friend;
        this.msgType = msgType;
    }
}
