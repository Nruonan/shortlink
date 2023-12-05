package com.xzn.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dto.req.UserLoginReqDTO;
import com.xzn.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.xzn.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;

/**
 * @author Nruonan
 * 用户接口层
 */
public interface UserService extends IService<UserDo> {
    // 根据用户名返回用户信息
    UserRespDTO getUserByUsername(String username);
    // 查询用户名是否可用
    Boolean hasUsername(String username);
    // 注册用户
    void register(UserRegisterReqDTO requestParam);
    // 更改用户信息
    void update(UserUpdateReqDTO requestParam);
    // 登录用户
    UserLoginRespDTO login(UserLoginReqDTO requestParam);
    // 检测用户是否登录
    Boolean checkLogin(String token, String username);
    // 退出登录
    void logout(String token, String username);
}
