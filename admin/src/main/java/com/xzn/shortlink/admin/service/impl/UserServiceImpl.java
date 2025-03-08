package com.xzn.shortlink.admin.service.impl;

import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISTER_KEY;
import static com.xzn.shortlink.admin.common.constant.RedisCacheConstant.USER_LOGIN_KEY;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_EXIST;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_NAME_EXIST;
import static com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum.USER_SAVE_ERROR;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.protobuf.ServiceException;
import com.xzn.shortlink.admin.common.biz.user.UserContext;
import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dao.mapper.UserMapper;
import com.xzn.shortlink.admin.dto.req.UserLoginReqDTO;
import com.xzn.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.xzn.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.GroupService;
import com.xzn.shortlink.admin.service.UserService;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Nruonan
 * 用户接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDo> implements UserService {

    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final GroupService groupService;

    @Override
    public UserRespDTO getUserByUsername(String username) {
        // 根据名字查询用户
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
        return !userRegisterCachePenetrationBloomFilter.contains(username);
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(UserRegisterReqDTO requestParam) {
        // 1 判断布隆过滤器是否存在这名字哦
        if(!hasUsername(requestParam.getUsername())){
            throw new ClientException(USER_NAME_EXIST);
        }
        // 上锁
        RLock lock = redissonClient.getLock(LOCK_USER_REGISTER_KEY + requestParam.getUsername());
        if(!lock.tryLock()) {
            throw new ClientException(USER_NAME_EXIST);
        }
        try{
            // 添加进数据库
            int insert = baseMapper.insert(BeanUtil.toBean(requestParam, UserDo.class));
            if(insert < 1){
                throw new ClientException(USER_SAVE_ERROR);
            }
            // 创建分组
            groupService.saveGroup(requestParam.getUsername(),"默认分组");
            // 布隆过滤器添加姓名
            userRegisterCachePenetrationBloomFilter.add(requestParam.getUsername());
        }catch (DuplicateKeyException ex){
            throw new ClientException(USER_EXIST);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 判断登录用户是否和当前用户一样
        if (!Objects.equals(requestParam.getUsername(), UserContext.getUsername())) {
            throw new ClientException("当前登录用户修改请求异常");
        }
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
            .eq(UserDo::getUsername, requestParam.getUsername());
        baseMapper.update(BeanUtil.toBean(requestParam,UserDo.class),queryWrapper);
    }

    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) throws ServiceException {
        // 查询 判断del_flag是否删除
        LambdaQueryWrapper<UserDo> queryWrapper = Wrappers.lambdaQuery(UserDo.class)
            .eq(UserDo::getUsername, requestParam.getUsername())
            .eq(UserDo::getPassword, requestParam.getPassword())
            .eq(UserDo::getDelFlag, 0);
        UserDo userDo = baseMapper.selectOne(queryWrapper);
        if(userDo == null){
            throw new ServiceException("用户不存在！");
        }
        // 用户登录后再其他地方在登录
        Map<Object,Object> hasLoginMap = stringRedisTemplate.opsForHash().entries("login_" + requestParam.getUsername());
        if(CollUtil.isNotEmpty(hasLoginMap)){
            stringRedisTemplate.expire(USER_LOGIN_KEY + requestParam.getUsername(), 30L, TimeUnit.MINUTES);
            String token = hasLoginMap.keySet().stream()
                .findFirst()
                .map(Object::toString)
                .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        // 创建uuid存入redis
        String uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_"+requestParam.getUsername(),uuid, JSON.toJSONString(requestParam));
        stringRedisTemplate.expire("login_"+requestParam.getUsername(), 30, TimeUnit.MINUTES);
        return new UserLoginRespDTO(uuid);
    }

    @Override
    public Boolean checkLogin(String token,String username) {
        return stringRedisTemplate.opsForHash().get(USER_LOGIN_KEY +username,token) != null;
    }

    @Override
    public void logout(String token, String username) {
        // 先判断是否登录上
        if(checkLogin(token,username)){
            // 删除redis
            stringRedisTemplate.delete("login_"+username);
            return;
        }
        throw new ClientException("用户尚未登录或者用户token不存在");

    }
}
