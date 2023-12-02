package com.xzn.shortlink.admin.controller;


import com.xzn.shortlink.admin.common.convention.exception.ClientException;
import com.xzn.shortlink.admin.common.convention.result.Result;
import com.xzn.shortlink.admin.common.convention.result.Results;
import com.xzn.shortlink.admin.common.enums.UserErrorCodeEnum;
import com.xzn.shortlink.admin.dto.resp.UserRespDTO;
import com.xzn.shortlink.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * 用户管理控制层
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/short-link/admin/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username){
        UserRespDTO result = userService.getUserByUsername(username);
        if(result == null){
            throw new ClientException(UserErrorCodeEnum.USER_NULL);
        }else {
            return Results.success(result);
        }
    }
}
