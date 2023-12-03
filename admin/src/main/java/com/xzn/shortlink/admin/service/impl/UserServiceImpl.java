package com.xzn.shortlink.admin.service.impl;

import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dao.mapper.UserMapper;
import com.xzn.shortlink.admin.dto.resp.UserRegisterReqDTO;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author Nruonan
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService {

    private final RBloomFilter<String> rBloomFilter;

    private final RedissonClient redissonClient;

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

    @Override
    public Boolean hasUsername(String username) {
//        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
//            .eq(UserDo::getUsername, username);
//        UserDo userDo = baseMapper.selectOne(queryWrapper);
//        return userDo != null;
        return !rBloomFilter.contains(username);
    }

    @Override
    public void register(UserRegisterReqDTO requestParam) {
        if(!hasUsername(requestParam.getUsername())){
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        try{
            if(lock.tryLock()){
                int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDo.class));
                if(insert < 1){
                    throw new ClientException(USER_SAVE_ERROR);
                }
                rBloomFilter.add(requestParam.getUsername());
                return;
            }else{
                throw new ClientException(USER_NAME_EXIST);
            }
        }finally {
            lock.unlock();
        }


    }
}
