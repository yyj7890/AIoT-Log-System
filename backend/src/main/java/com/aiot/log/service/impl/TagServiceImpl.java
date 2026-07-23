package com.aiot.log.service.impl;

import com.aiot.log.dto.TagCreateRequest;
import com.aiot.log.entity.LogTag;
import com.aiot.log.entity.Tag;
import com.aiot.log.exception.BusinessException;
import com.aiot.log.exception.ErrorCode;
import com.aiot.log.mapper.LogTagMapper;
import com.aiot.log.mapper.TagMapper;
import com.aiot.log.service.TagService;
import com.aiot.log.vo.TagVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final LogTagMapper logTagMapper;

    public TagServiceImpl(TagMapper tagMapper, LogTagMapper logTagMapper) {
        this.tagMapper = tagMapper;
        this.logTagMapper = logTagMapper;
    }

    @Override
    public List<TagVO> listTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                .orderByAsc(Tag::getName));
        List<TagVO> result = new ArrayList<TagVO>();
        for (Tag tag : tags) {
            result.add(toVO(tag));
        }
        return result;
    }

    @Override
    public TagVO createTag(TagCreateRequest request) {
        ensureTagNameUnique(request.getName());

        Tag tag = new Tag();
        tag.setName(request.getName());
        tagMapper.insert(tag);

        return toVO(tagMapper.selectById(tag.getId()));
    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
        }
        logTagMapper.delete(new LambdaQueryWrapper<LogTag>().eq(LogTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    private void ensureTagNameUnique(String name) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (count > 0) {
            throw new BusinessException(ErrorCode.TAG_NAME_DUPLICATED);
        }
    }

    private TagVO toVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setCreatedAt(tag.getCreatedAt());
        vo.setUpdatedAt(tag.getUpdatedAt());
        return vo;
    }
}

