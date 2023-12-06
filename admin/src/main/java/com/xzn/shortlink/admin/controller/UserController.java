package com.xzn.shortlink.admin.controller;


import cn.hutool.core.bean.BeanUtil;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.common.convention.result.Results;
import com.xzn.shortlink.admin.dto.req.UserLoginReqDTO;
import com.xzn.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.xzn.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.xzn.shortlink.admin.dto.resp.UserActualRespDTO;
import com.xzn.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor
public class UserController  {

    private final UserService userService;


    /**
     * 根据用户名查询用户脱敏信息
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username){
        return Results.success(userService.getUserByUsername(username));
    }
    /**
     * 根据用户名查询用户无脱敏信息
     */
    @GetMapping("/api/short-link/admin/v1/actual/user/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable("username") String username){
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserActualRespDTO.class));
    }

    /**
     * 查询用户名是否可用
     */
    @GetMapping("/api/short-link/admin/v1/user/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username){
        return Results.success(userService.hasUsername(username));
    }

    /**
     *  注册账号
     */
    @PostMapping("/api/short-link/admin/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam){
        userService.register(requestParam);
        return Results.success();
    }

    /**
     *  修改用户信息
     */
    @PutMapping("/api/short-link/admin/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO requestParam){
        userService.update(requestParam);
        return Results.success();
    }

    /**
     *  登录用户
     */
    @PostMapping("/api/short-link/admin/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam){
        return  Results.success(userService.login(requestParam));
    }

    /*
    * 检测登录用户
    */
    @GetMapping("/api/short-link/admin/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("token") String token, @RequestParam("username") String username){
        return Results.success(userService.checkLogin(token, username));
    }

    /**
     *  退出登录
     */
    @DeleteMapping("/api/short-link/admin/v1/user/logout")
    public Result<Void> logout(@RequestParam("token") String token, @RequestParam("username") String username){
        userService.logout(token,username);
        return Results.success();
    }
}
