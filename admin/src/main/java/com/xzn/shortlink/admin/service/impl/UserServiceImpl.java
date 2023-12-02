package com.xzn.shortlink.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dao.mapper.UserMapper;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * 用户接口实现层
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService {

    @Override
    public UserRespDTO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
            .eq(UserDo::getUsername, username);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        UserRespDTO result = new UserRespDTO();
        if(userDo == null) {
           throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }
        else BeanUtils.copyProperties(userDo,result);
        return result;
    }
}
