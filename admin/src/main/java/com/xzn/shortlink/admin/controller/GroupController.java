package com.xzn.shortlink.admin.controller;

import com.xzn.shortlink.admin.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Nruonan
 * @description
 */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
}
