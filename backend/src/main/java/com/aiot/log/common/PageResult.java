package com.aiot.log.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页查询结果")
public class PageResult<T> {

    @Schema(description = "当前页记录")
    private List<T> records;

    @Schema(description = "符合条件的记录总数", example = "46")
    private Long total;

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private Long page;

    @Schema(description = "每页记录数", example = "10")
    private Long pageSize;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long page, Long pageSize) {
        this.records = records;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}

