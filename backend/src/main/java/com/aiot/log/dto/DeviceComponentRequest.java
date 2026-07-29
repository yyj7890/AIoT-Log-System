package com.aiot.log.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeviceComponentRequest {
    @NotBlank(message = "零件名称不能为空")
    @Size(max = 100)
    private String name;
    @Size(max = 50) private String category;
    @Size(max = 100) private String model;
    @Min(value = 1, message = "数量至少为 1") private Integer quantity;
    @Size(max = 500) private String notes;
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
