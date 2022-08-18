package com.adups.fota.bean;

public class EventMessage {

    private int what;
    private int arg1;
    private long arg2;
    private long arg3;

    private Object mObject;

    public EventMessage(int what, int arg1, long arg2, long arg3, Object object) {
        this.what = what;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        mObject = object;
    }

    public int getWhat() {
        return what;
    }

    public int getArg1() {
        return arg1;
    }

    public long getArg2() {
        return arg2;
    }

    public long getArg3() {
        return arg3;
    }

    public Object getObject() {
        return mObject;
    }

}
