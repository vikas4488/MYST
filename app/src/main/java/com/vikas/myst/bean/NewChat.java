package com.vikas.myst.bean;

import java.util.Objects;

public class NewChat {
        private String date;
        private String msgCount;
        private String msg;
        private String status;
        private String time;
        private String friend;


        public NewChat() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof com.vikas.myst.bean.Chat)) return false;
            com.vikas.myst.bean.Chat chat = (com.vikas.myst.bean.Chat) o;
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

    public String getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(String msgCount) {
        this.msgCount = msgCount;
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

    public NewChat(String date, String msgCount, String msg, String status, String time, String friend) {
        this.date = date;
        this.msgCount = msgCount;
        this.msg = msg;
        this.status = status;
        this.time = time;
        this.friend = friend;
    }

    @Override
    public String toString() {
        return "NewChat{" +
                "date='" + date + '\'' +
                ", msgCount='" + msgCount + '\'' +
                ", msg='" + msg + '\'' +
                ", status='" + status + '\'' +
                ", time='" + time + '\'' +
                ", friend='" + friend + '\'' +
                '}';
    }
}
