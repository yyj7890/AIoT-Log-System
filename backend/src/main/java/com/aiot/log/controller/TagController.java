package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.TagCreateRequest;
import com.aiot.log.service.TagService;
import com.aiot.log.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/tags")
@Tag(name = "标签管理", description = "日志标签的查询与维护")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "查询全部标签")
    public ApiResponse<List<TagVO>> listTags() {
        return ApiResponse.success(tagService.listTags());
    }

    @PostMapping
    @Operation(summary = "创建标签")
    public ApiResponse<TagVO> createTag(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.success(tagService.createTag(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除标签")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ApiResponse.success();
    }
}

