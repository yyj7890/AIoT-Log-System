package com.aiot.log.service;

import com.aiot.log.dto.TagCreateRequest;
import com.aiot.log.vo.TagVO;

import java.util.List;

public interface TagService {

    List<TagVO> listTags();

    TagVO createTag(TagCreateRequest request);

    void deleteTag(Long id);
}

