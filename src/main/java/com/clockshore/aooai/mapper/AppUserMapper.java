package com.clockshore.aooai.mapper;

import com.clockshore.aooai.entity.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppUserMapper {

    int insert(AppUser user);

    int update(AppUser user);

    int deleteById(@Param("id") Long id);

    AppUser selectById(@Param("id") Long id);

    List<AppUser> selectAll();

    AppUser selectByUsername(@Param("username") String username);

    String selectPrimaryRoleCodeByUserId(@Param("userId") Long userId);

}
