package com.aiot.log.controller;

import com.aiot.log.common.ApiResponse;
import com.aiot.log.dto.TagCreateRequest;
import com.aiot.log.service.TagService;
import com.aiot.log.vo.TagVO;
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
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagVO>> listTags() {
        return ApiResponse.success(tagService.listTags());
    }

    @PostMapping
    public ApiResponse<TagVO> createTag(@Valid @RequestBody TagCreateRequest request) {
        return ApiResponse.success(tagService.createTag(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ApiResponse.success();
    }
}

