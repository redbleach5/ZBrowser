package com.zbrowser;

import android.os.Parcel;
import android.os.Parcelable;

public class TabInfo implements Parcelable {

    private int id;
    private String title;
    private String url;
    private boolean isIncognito;

    public TabInfo(int id, String title, String url, boolean isIncognito) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.isIncognito = isIncognito;
    }

    protected TabInfo(Parcel in) {
        id = in.readInt();
        title = in.readString();
        url = in.readString();
        isIncognito = in.readByte() != 0;
    }

    public static final Creator<TabInfo> CREATOR = new Creator<TabInfo>() {
        @Override
        public TabInfo createFromParcel(Parcel in) {
            return new TabInfo(in);
        }

        @Override
        public TabInfo[] newArray(int size) {
            return new TabInfo[size];
        }
    };

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title != null ? title : ""; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url != null ? url : ""; }
    public void setUrl(String url) { this.url = url; }

    public boolean isIncognito() { return isIncognito; }
    public void setIncognito(boolean incognito) { isIncognito = incognito; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeByte((byte) (isIncognito ? 1 : 0));
    }
}
