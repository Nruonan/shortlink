package com.xzn.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xzn.shortlink.admin.dao.entity.UserDo;
import com.xzn.shortlink.admin.dto.resp.UserRegisterReqDTO;
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

    void register(UserRegisterReqDTO requestParam);
}
