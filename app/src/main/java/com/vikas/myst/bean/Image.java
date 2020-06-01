package com.vikas.myst.bean;

import java.sql.Blob;

public class Image {
    private int id;
    private Blob imageDate;
    private String name;
    private String status;

    public Image(int id, Blob imageDate, String name, String status) {
        this.id = id;
        this.imageDate = imageDate;
        this.name = name;
        this.status = status;
    }

    public Image() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Blob getImageDate() {
        return imageDate;
    }

    public void setImageDate(Blob imageDate) {
        this.imageDate = imageDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Image{" +
                "id=" + id +
                ", imageDate=" + imageDate +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
