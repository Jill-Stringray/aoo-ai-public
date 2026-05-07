package com.clockshore.aooai.mapper;

import com.clockshore.aooai.dto.UserDto;
import com.clockshore.aooai.entity.AppUser;
import com.clockshore.aooai.vo.UserVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AppUserMapper {

    @Insert("""
            SELECT id,user_name,balance
            FROM app_user
            WHERE user_name = #{userName} AND password = #{password}
            LIMIT 1
            """)
    UserVO login(UserDto u);

    @Insert("""
            INSERT INTO app_user (user_name, balance, password)
            VALUES (#{userName}, #{balance}, #{password})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(AppUser appUser);

    @Select("""
            SELECT id, user_name AS userName, created_at AS createdAt, updated_at AS updatedAt,
                   balance, password
            FROM app_user
            WHERE id = #{id}
            """)
    AppUser selectById(@Param("id") Long id);

    @Select("""
            SELECT id, user_name AS userName, created_at AS createdAt, updated_at AS updatedAt,
                   balance, password
            FROM app_user
            ORDER BY updated_at DESC
            """)
    List<AppUser> selectAll();

    @Update("""
            UPDATE app_user
            SET user_name = #{userName},
                balance = #{balance},
                password = #{password}
            WHERE id = #{id}
            """)
    int updateById(AppUser appUser);

    @Delete("DELETE FROM app_user WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}