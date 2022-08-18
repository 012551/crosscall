package com.adups.fota.bean;

import java.util.List;

/**
 * Created by Administrator on 2016/3/17.
 */
public class RootErrBean {

    private List<String> add, modify, delete;

    public List<String> getAdd() {
        return add;
    }

    public void setAdd(List<String> add) {
        this.add = add;
    }

    public List<String> getModify() {
        return modify;
    }

    public void setModify(List<String> modify) {
        this.modify = modify;
    }

    public List<String> getDelete() {
        return delete;
    }

    public void setDelete(List<String> delete) {
        this.delete = delete;
    }
}
