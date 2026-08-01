package com.aiot.log.service;
import com.aiot.log.dto.EnvironmentSpaceRequest; import com.aiot.log.vo.EnvironmentSpaceVO; import java.util.List;
public interface EnvironmentSpaceService { List<EnvironmentSpaceVO> list(); EnvironmentSpaceVO create(EnvironmentSpaceRequest request); EnvironmentSpaceVO update(Long id, EnvironmentSpaceRequest request); void delete(Long id); }
