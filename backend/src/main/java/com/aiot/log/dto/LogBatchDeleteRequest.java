package com.aiot.log.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class LogBatchDeleteRequest {

    @NotEmpty(message = "至少选择一条日志")
    private List<@NotNull(message = "日志编号不能为空") Long> ids;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
