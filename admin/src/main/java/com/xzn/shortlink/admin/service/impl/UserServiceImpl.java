package com.xzn.shortlink.admin.service.impl;

import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_EXIST;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dao.mapper.UserMapper;
import com.xzn.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.xzn.shortlink.admin.dto.req.UserLoginReqDTO;
import com.xzn.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.xzn.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import com.xzn.shortlink.admin.service.UserService;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

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
                try{
                    int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDo.class));
                    if(insert < 1){
                        throw new ClientException(USER_SAVE_ERROR);
                    }
                }catch (DuplicateKeyException ex){
                    throw new ClientException(USER_EXIST);
                }
                rBloomFilter.add(requestParam.getUsername());
                groupService.saveGroup(new ShortLinkGroupSaveReqDTO("默认分组"));
                return;
            }

            throw new ClientException(USER_NAME_EXIST);

        }finally {
            lock.unlock();
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // TODO 检测当前用户是否修改用户
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
            .eq(UserDo::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam,UserDo.class),queryWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
            .eq(UserDo::getUsername, requestParam.getUsername())
            .eq(UserDo::getPassword, requestParam.getPassword())
            .eq(UserDo::getDelFlag, 0);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        if(userDo == null){
            throw new ClientException("用户不存在！");
        }
        Boolean hasLogin = stringRedisTemplate.hasKey("login_" + requestParam.getUsername());
        if(hasLogin != null && hasLogin){
            throw  new ClientException("用户已登录");
        }
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_"+requestParam.getUsername(),uuid, JSON.toJSONString(requestParam));
        stringRedisTemplate.expire("login_"+requestParam.getUsername(), 30, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String token,String username) {
        return stringRedisTemplate.opsForHash().get("login_"+username,token) != null;
    }

    @Override
    public void logout(String token, String username) {
        if(checkLogin(token,username)){
            stringRedisTemplate.delete("login_"+username);
            return;
        }
        throw new ClientException("用户尚未登录或者用户token不存在");

    }
}
