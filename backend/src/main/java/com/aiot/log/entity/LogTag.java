package com.aiot.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("log_tags")
public class LogTag {

    private Long logId;
    private Long tagId;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getTagId() {
        return tagId;
    }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }
}

